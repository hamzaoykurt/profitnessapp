# Android App Links for password reset

Password reset should move from the old custom scheme to a verified HTTPS App Link without breaking existing users.

Production settings:

- App package: `com.avonix.profitness`
- Redirect URL: `https://cosmibit.com/reset-password`
- Manifest host override: `RESET_PASSWORD_LINK_HOST`
- Supabase redirect override: `RESET_PASSWORD_REDIRECT_URL`

Rollout rules:

1. Keep the app able to open both `https://<host>/reset-password` and old `profitness://reset-password` links.
2. Publish `assetlinks.json` first.
3. Add the HTTPS redirect URL to Supabase Auth URL Configuration.
4. Set GitHub repository variables `RESET_PASSWORD_LINK_HOST=cosmibit.com` and `RESET_PASSWORD_REDIRECT_URL=https://cosmibit.com/reset-password`.
5. After one stable release cycle, stop issuing old custom-scheme links. Remove the legacy manifest filter only after no active recovery emails rely on it.

Before switching release builds to HTTPS, publish this file at:

`https://<RESET_PASSWORD_LINK_HOST>/.well-known/assetlinks.json`

Use the release signing SHA-256 fingerprint from the GitHub Actions release workflow.

```json
[
  {
    "relation": ["delegate_permission/common.handle_all_urls"],
    "target": {
      "namespace": "android_app",
      "package_name": "com.avonix.profitness",
      "sha256_cert_fingerprints": ["REPLACE_WITH_RELEASE_SIGNING_SHA256"]
    }
  }
]
```

Supabase Auth URL Configuration must allow the exact `RESET_PASSWORD_REDIRECT_URL`.
