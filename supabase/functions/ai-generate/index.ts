import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { adminClient, authenticatedUser, requiredEnv } from "../_shared/auth.ts";
import { jsonResponse, preflight } from "../_shared/cors.ts";

type GeminiPart = {
  text?: string;
  inline_data?: {
    mime_type?: string;
    data?: string;
  };
};

type GeminiContent = {
  role?: string;
  parts?: GeminiPart[];
};

type GeminiRequest = {
  system_instruction?: {
    parts?: GeminiPart[];
  };
  contents?: GeminiContent[];
  generationConfig?: {
    temperature?: number;
    maxOutputTokens?: number;
  };
};

type AiGenerateBody = {
  tool?: string;
  idempotencyKey?: string;
  request?: GeminiRequest;
};

const GEMINI_ENDPOINT =
  "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

const MAX_REQUEST_BYTES = Number(Deno.env.get("AI_MAX_REQUEST_BYTES") ?? "1500000");
const MAX_TEXT_CHARS = Number(Deno.env.get("AI_MAX_TEXT_CHARS") ?? "30000");
const MAX_INLINE_BYTES = Number(Deno.env.get("AI_MAX_INLINE_BYTES") ?? "1200000");

const ALLOWED_INLINE_MIME = new Set([
  "image/jpeg",
  "image/png",
  "image/webp",
  "application/pdf",
]);

function estimateBase64Bytes(base64: string): number {
  const padding = base64.endsWith("==") ? 2 : base64.endsWith("=") ? 1 : 0;
  return Math.floor((base64.length * 3) / 4) - padding;
}

function cleanText(value: unknown, maxChars: number): string {
  if (typeof value !== "string") throw new Error("invalid_text");
  const text = value.trim();
  if (!text) throw new Error("empty_text");
  if (text.length > maxChars) throw new Error("text_too_large");
  return text;
}

function sanitizePart(part: GeminiPart, textBudget: { remaining: number }): GeminiPart {
  if (part.text != null) {
    const text = cleanText(part.text, Math.min(12000, textBudget.remaining));
    textBudget.remaining -= text.length;
    return { text };
  }

  const inline = part.inline_data;
  if (inline?.mime_type && inline?.data) {
    const mime = inline.mime_type.toLowerCase();
    if (!ALLOWED_INLINE_MIME.has(mime)) throw new Error("unsupported_media_type");
    if (estimateBase64Bytes(inline.data) > MAX_INLINE_BYTES) throw new Error("media_too_large");
    return { inline_data: { mime_type: mime, data: inline.data } };
  }

  throw new Error("invalid_part");
}

function sanitizeContents(contents: GeminiContent[], textBudget: { remaining: number }): GeminiContent[] {
  if (!Array.isArray(contents) || contents.length === 0 || contents.length > 30) {
    throw new Error("invalid_contents");
  }

  return contents.map((content) => {
    const role = content.role === "model" ? "model" : "user";
    if (!Array.isArray(content.parts) || content.parts.length === 0 || content.parts.length > 8) {
      throw new Error("invalid_parts");
    }
    return {
      role,
      parts: content.parts.map((part) => sanitizePart(part, textBudget)),
    };
  });
}

function sanitizeRequest(body: GeminiRequest): GeminiRequest {
  const textBudget = { remaining: MAX_TEXT_CHARS };
  const contents = sanitizeContents(body.contents ?? [], textBudget);
  const systemParts = body.system_instruction?.parts?.map((part) => {
    if (part.text == null) throw new Error("invalid_system_instruction");
    return { text: cleanText(part.text, Math.min(12000, textBudget.remaining)) };
  });

  const temperature = Number(body.generationConfig?.temperature ?? 0.7);
  const maxOutputTokens = Number(body.generationConfig?.maxOutputTokens ?? 600);

  return {
    system_instruction: systemParts?.length ? { parts: systemParts } : undefined,
    contents,
    generationConfig: {
      temperature: Number.isFinite(temperature) ? Math.min(Math.max(temperature, 0), 1) : 0.7,
      maxOutputTokens: Number.isFinite(maxOutputTokens)
        ? Math.min(Math.max(Math.floor(maxOutputTokens), 128), 4096)
        : 600,
    },
  };
}

Deno.serve(async (req: Request) => {
  const preflightResponse = preflight(req);
  if (preflightResponse) return preflightResponse;

  if (req.method !== "POST") {
    return jsonResponse(405, { code: "method_not_allowed", message: "Method not allowed." });
  }

  const contentLength = Number(req.headers.get("content-length") ?? "0");
  if (Number.isFinite(contentLength) && contentLength > MAX_REQUEST_BYTES) {
    return jsonResponse(413, { code: "request_too_large", message: "AI isteği çok büyük." });
  }

  let usageEventId: string | null = null;
  const supabase = adminClient();

  try {
    const { user } = await authenticatedUser(req);
    const body = await req.json().catch(() => ({})) as AiGenerateBody;
    const tool = typeof body.tool === "string" ? body.tool.trim() : "";
    const idempotencyKey = typeof body.idempotencyKey === "string" ? body.idempotencyKey.trim() : crypto.randomUUID();
    if (!tool || !body.request) {
      return jsonResponse(400, { code: "invalid_request", message: "AI isteği geçersiz." });
    }

    const sanitized = sanitizeRequest(body.request);
    const { data: reservation, error: reserveError } = await supabase.rpc("reserve_ai_usage", {
      p_user_id: user.id,
      p_tool: tool,
      p_idempotency_key: idempotencyKey,
    });

    if (reserveError) {
      return jsonResponse(503, { code: "entitlement_unavailable", message: "AI kullanım hakkı doğrulanamadı." });
    }
    if (reservation?.allowed !== true) {
      return jsonResponse(402, {
        code: reservation?.reason ?? "insufficient_entitlement",
        message: reservation?.message ?? "Bu işlem için kredi veya abonelik gerekiyor.",
        requiredCredits: reservation?.required_credits ?? null,
        remainingCredits: reservation?.remaining_credits ?? null,
      });
    }
    usageEventId = reservation.usage_event_id;

    const upstream = await fetch(`${GEMINI_ENDPOINT}?key=${encodeURIComponent(requiredEnv("GEMINI_API_KEY"))}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(sanitized),
    });

    const upstreamJson = await upstream.json().catch(() => ({}));
    if (!upstream.ok || upstreamJson?.error) {
      throw new Error("ai_provider_error");
    }

    const text = upstreamJson?.candidates?.[0]?.content?.parts?.[0]?.text;
    if (typeof text !== "string" || !text.trim()) {
      throw new Error("empty_ai_response");
    }

    await supabase.rpc("complete_ai_usage", { p_usage_event_id: usageEventId });
    return jsonResponse(200, { text: text.trim(), billing: reservation });
  } catch (error) {
    const code = error instanceof Error ? error.message : "unknown_error";
    if (usageEventId) {
      await supabase.rpc("refund_ai_usage", {
        p_usage_event_id: usageEventId,
        p_error_code: code,
      });
    }
    if (code === "unauthorized") {
      return jsonResponse(401, { code: "unauthorized", message: "Oturum gerekli." });
    }
    if (code.startsWith("invalid_") || code.endsWith("_too_large") || code === "unsupported_media_type") {
      return jsonResponse(400, { code, message: "AI isteği geçersiz." });
    }
    return jsonResponse(502, { code: "ai_provider_error", message: "AI servisi şu anda yanıt veremiyor." });
  }
});
