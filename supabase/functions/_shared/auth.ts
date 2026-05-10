import { createClient } from "npm:@supabase/supabase-js@2";

export type SupabaseUser = {
  id: string;
};

function firstEnv(name: string, aliases: string[] = []): string | null {
  for (const candidate of [name, ...aliases]) {
    const value = Deno.env.get(candidate);
    if (value?.trim()) return value;
  }
  return null;
}

export function requiredEnv(name: string, aliases: string[] = []): string {
  const value = firstEnv(name, aliases);
  if (!value) throw new Error(`missing_${name.toLowerCase()}`);
  return value;
}

export function truthyEnv(name: string, aliases: string[] = []): boolean {
  const value = firstEnv(name, aliases);
  if (!value) return false;

  const normalized = value.trim().replace(/^["']|["']$/g, "").toLowerCase();
  return normalized === "true" || normalized === "1" || normalized === "yes" || normalized === "on";
}

export function adminClient() {
  return createClient(
    requiredEnv("SUPABASE_URL"),
    requiredEnv("SUPABASE_SERVICE_ROLE_KEY"),
    { auth: { persistSession: false, autoRefreshToken: false } },
  );
}

export async function authenticatedUser(req: Request): Promise<{
  user: SupabaseUser;
  authorization: string;
}> {
  const authorization = req.headers.get("authorization") ?? "";
  const token = authorization.replace(/^Bearer\s+/i, "");
  if (!token) throw new Error("unauthorized");

  const supabaseForUser = createClient(requiredEnv("SUPABASE_URL"), requiredEnv("SUPABASE_ANON_KEY"), {
    global: { headers: { Authorization: authorization } },
    auth: { persistSession: false, autoRefreshToken: false },
  });

  const { data, error } = await supabaseForUser.auth.getUser(token);
  if (error || !data.user) throw new Error("unauthorized");

  return { user: { id: data.user.id }, authorization };
}
