-- Keep internal private-schema helpers callable by authenticated app users through
-- public wrappers, but remove unnecessary default PUBLIC/anon execute grants.
grant usage on schema private to authenticated;
revoke usage on schema private from public, anon;

grant execute on all functions in schema private to authenticated;
revoke execute on all functions in schema private from public, anon;

-- Future private helpers should not inherit callable PUBLIC defaults.
alter default privileges in schema private revoke execute on functions from public;
alter default privileges in schema private grant execute on functions to authenticated;

