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

const GEMINI_API_BASE = "https://generativelanguage.googleapis.com/v1beta/models";
const DEFAULT_GEMINI_MODELS = [
  "gemini-2.5-flash-lite",
  "gemini-flash-lite-latest",
  "gemini-2.5-flash",
];

const MAX_REQUEST_BYTES = Number(Deno.env.get("AI_MAX_REQUEST_BYTES") ?? "1500000");
const MAX_TEXT_CHARS = Number(Deno.env.get("AI_MAX_TEXT_CHARS") ?? "30000");
const MAX_INLINE_BYTES = Number(Deno.env.get("AI_MAX_INLINE_BYTES") ?? "1200000");
const MAX_IDEMPOTENCY_KEY_CHARS = 120;
const DEFAULT_ENERGY_ENTITLEMENT_MESSAGE = "Bu işlem için Enerji veya abonelik gerekiyor.";

const ALLOWED_INLINE_MIME = new Set([
  "image/jpeg",
  "image/png",
  "image/webp",
  "application/pdf",
]);

const ALLOWED_TOOLS = new Set([
  "ORACLE_CHAT",
  "PROGRAM_GENERATE_TEXT",
  "PROGRAM_GENERATE_MEDIA",
  "PROGRAM_EDIT",
  "WEIGHT_TREND_ANALYSIS",
  "EXERCISE_PROGRESS_ANALYSIS",
  "WORKOUT_PROGRESS_ANALYSIS",
  "ORACLE_TO_PROGRAM",
]);

function configuredModels(): string[] {
  const configured = Deno.env.get("GEMINI_MODEL")
    ?.split(",")
    .map((model) => model.trim())
    .filter(Boolean);

  const models = configured?.length ? configured : DEFAULT_GEMINI_MODELS;
  return [...new Set(models)];
}

function energyEntitlementMessage(message?: string | null): string {
  if (!message) return DEFAULT_ENERGY_ENTITLEMENT_MESSAGE;
  return message
    .replaceAll("kredi veya abonelik hakkı", "Enerji veya abonelik")
    .replaceAll("kredi veya abonelik", "Enerji veya abonelik")
    .replaceAll("kredi", "Enerji")
    .replaceAll("Kredi", "Enerji");
}

function cleanTool(value: unknown): string {
  if (typeof value !== "string") throw new Error("invalid_tool");
  const tool = value.trim();
  if (!ALLOWED_TOOLS.has(tool)) throw new Error("invalid_tool");
  return tool;
}

function cleanIdempotencyKey(value: unknown): string {
  if (typeof value !== "string") return crypto.randomUUID();
  const key = value.trim();
  if (!key) return crypto.randomUUID();
  if (key.length > MAX_IDEMPOTENCY_KEY_CHARS) throw new Error("invalid_idempotency_key");
  if (!/^[A-Za-z0-9._:-]+$/.test(key)) throw new Error("invalid_idempotency_key");
  return key;
}

function estimateBase64Bytes(base64: string): number {
  const padding = base64.endsWith("==") ? 2 : base64.endsWith("=") ? 1 : 0;
  return Math.floor((base64.length * 3) / 4) - padding;
}

function requestHasInlineData(request: GeminiRequest): boolean {
  return (request.contents ?? []).some((content) =>
    (content.parts ?? []).some((part) => part.inline_data != null)
  );
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
    const tool = cleanTool(body.tool);
    const idempotencyKey = cleanIdempotencyKey(body.idempotencyKey);
    if (!body.request) {
      return jsonResponse(400, { code: "invalid_request", message: "AI isteği geçersiz." });
    }

    const sanitized = sanitizeRequest(body.request);
    if (requestHasInlineData(sanitized) && tool !== "PROGRAM_GENERATE_MEDIA") {
      return jsonResponse(400, { code: "invalid_request", message: "AI isteği geçersiz." });
    }
    const { data: reservation, error: reserveError } = await supabase.rpc("reserve_ai_usage", {
      p_user_id: user.id,
      p_tool: tool,
      p_idempotency_key: idempotencyKey,
    });

    if (reserveError) {
      return jsonResponse(503, { code: "entitlement_unavailable", message: "AI kullanım hakkı doğrulanamadı." });
    }
    if (reservation?.allowed !== true) {
      if (typeof reservation?.reason === "string" && reservation.reason.startsWith("idempotency_")) {
        return jsonResponse(409, {
          code: reservation.reason,
          message: reservation.message ?? "Bu idempotency anahtarı için istek zaten işlendi veya işleniyor.",
        });
      }
      return jsonResponse(402, {
        code: reservation?.reason ?? "insufficient_entitlement",
        message: energyEntitlementMessage(reservation?.message),
        requiredCredits: reservation?.required_credits ?? null,
        remainingCredits: reservation?.remaining_credits ?? null,
      });
    }
    usageEventId = reservation.usage_event_id;

    const geminiApiKey = requiredEnv("GEMINI_API_KEY", [
      "Gemini_API_Key",
      "Gemini_API_KEY",
      "GEMINI_API_Key",
      "GEMINI_APIKEY",
    ]);

    let upstreamJson: any | null = null;
    let lastProviderStatus = 0;
    let lastProviderCode = "ai_provider_error";

    for (const model of configuredModels()) {
      const upstream = await fetch(
        `${GEMINI_API_BASE}/${model}:generateContent?key=${encodeURIComponent(geminiApiKey)}`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(sanitized),
        },
      );

      upstreamJson = await upstream.json().catch(() => ({}));
      lastProviderStatus = upstream.status;
      lastProviderCode = typeof upstreamJson?.error === "object" &&
        upstreamJson.error !== null &&
        "status" in upstreamJson.error
          ? String((upstreamJson.error as { status?: unknown }).status)
          : "ai_provider_error";

      if (upstream.ok && !upstreamJson?.error) break;
      upstreamJson = null;
    }

    if (!upstreamJson) {
      console.error("Gemini provider failed", {
        status: lastProviderStatus,
        code: lastProviderCode,
      });
      throw new Error(lastProviderStatus === 429 ? "ai_quota_exhausted" : "ai_provider_error");
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
    if (code === "ai_quota_exhausted") {
      return jsonResponse(429, { code, message: "AI kotası doldu. Bir süre sonra tekrar dene." });
    }
    return jsonResponse(502, { code: "ai_provider_error", message: "AI servisi şu anda yanıt veremiyor." });
  }
});
