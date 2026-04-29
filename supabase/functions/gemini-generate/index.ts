import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "npm:@supabase/supabase-js@2";

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

const GEMINI_ENDPOINT =
  "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

const MAX_REQUEST_BYTES = Number(Deno.env.get("AI_MAX_REQUEST_BYTES") ?? "1500000");
const MAX_TEXT_CHARS = Number(Deno.env.get("AI_MAX_TEXT_CHARS") ?? "30000");
const MAX_INLINE_BYTES = Number(Deno.env.get("AI_MAX_INLINE_BYTES") ?? "1200000");
const RATE_LIMIT_PER_HOUR = Number(Deno.env.get("AI_RATE_LIMIT_PER_HOUR") ?? "60");

const ALLOWED_INLINE_MIME = new Set([
  "image/jpeg",
  "image/png",
  "image/webp",
  "application/pdf",
]);

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

function jsonResponse(status: number, body: Record<string, unknown>): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

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
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  if (req.method !== "POST") {
    return jsonResponse(405, { code: "method_not_allowed", message: "Method not allowed." });
  }

  const contentLength = Number(req.headers.get("content-length") ?? "0");
  if (Number.isFinite(contentLength) && contentLength > MAX_REQUEST_BYTES) {
    return jsonResponse(413, { code: "request_too_large", message: "AI isteği çok büyük." });
  }

  const geminiApiKey = Deno.env.get("GEMINI_API_KEY");
  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const supabaseAnonKey = Deno.env.get("SUPABASE_ANON_KEY");
  const supabaseServiceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");

  if (!geminiApiKey || !supabaseUrl || !supabaseAnonKey || !supabaseServiceRoleKey) {
    return jsonResponse(503, { code: "service_not_configured", message: "AI servisi henüz yapılandırılmadı." });
  }

  const authorization = req.headers.get("authorization") ?? "";
  const token = authorization.replace(/^Bearer\s+/i, "");
  if (!token) {
    return jsonResponse(401, { code: "unauthorized", message: "Oturum gerekli." });
  }

  const supabaseForUser = createClient(supabaseUrl, supabaseAnonKey, {
    global: { headers: { Authorization: authorization } },
  });
  const { data: userData, error: userError } = await supabaseForUser.auth.getUser(token);
  if (userError || !userData.user) {
    return jsonResponse(401, { code: "unauthorized", message: "Oturum doğrulanamadı." });
  }

  const supabaseAdmin = createClient(supabaseUrl, supabaseServiceRoleKey, {
    auth: { persistSession: false, autoRefreshToken: false },
  });
  const { data: allowed, error: rateLimitError } = await supabaseAdmin.rpc("check_ai_rate_limit", {
    p_user_id: userData.user.id,
    p_limit: RATE_LIMIT_PER_HOUR,
    p_window_seconds: 3600,
  });

  if (rateLimitError) {
    return jsonResponse(503, { code: "rate_limit_unavailable", message: "AI servisi şu anda yanıt veremiyor." });
  }
  if (allowed !== true) {
    return jsonResponse(429, { code: "rate_limited", message: "AI kullanım limiti doldu. Bir süre sonra tekrar dene." });
  }

  let sanitized: GeminiRequest;
  try {
    sanitized = sanitizeRequest(await req.json());
  } catch (_) {
    return jsonResponse(400, { code: "invalid_request", message: "AI isteği geçersiz." });
  }

  const upstream = await fetch(`${GEMINI_ENDPOINT}?key=${encodeURIComponent(geminiApiKey)}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(sanitized),
  });

  const upstreamJson = await upstream.json().catch(() => ({}));
  if (!upstream.ok || upstreamJson?.error) {
    return jsonResponse(502, { code: "ai_provider_error", message: "AI servisi şu anda yanıt veremiyor." });
  }

  const text = upstreamJson?.candidates?.[0]?.content?.parts?.[0]?.text;
  if (typeof text !== "string" || !text.trim()) {
    return jsonResponse(502, { code: "empty_ai_response", message: "AI boş yanıt döndürdü." });
  }

  return jsonResponse(200, { text: text.trim() });
});
