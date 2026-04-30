import { createClient } from "npm:@supabase/supabase-js@2";

export type SupabaseUser = {
  id: string;
};

export function requiredEnv(name: string): string {
  const value = Deno.env.get(name);
  if (!value) throw new Error(`missing_${name.toLowerCase()}`);
  return value;
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
