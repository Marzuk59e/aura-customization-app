# Device Authentication — Backend Contract

**UX note (no backend change needed for this part):** the forgot-password
flow no longer asks for an email address up front. The app already has the
account's email cached locally (`TrustedDeviceEntity`, stored at
registration time) and uses that automatically. `/challenge` and
`/verify-login` still take `email` in the request body exactly as before —
the app just fills it in from local storage instead of a text field. If no
local key exists at all, or biometric verification fails three times, the
app falls back to asking for the email and sending the existing
Firebase link-based reset email (`sendFallbackResetEmail` — unchanged for
now).

The Android app now replaces the old simulated OTP password-reset step with
secure, Keystore-backed device authentication. The client side is fully
implemented; three new endpoints are needed on the PHP backend
(`aura-launcher.unaux.com/api/v1/`) to complete the flow. The app never
sends a private key, fingerprint/face data, or a PIN — only a public key
and, later, a signature over a server-issued challenge.

## 1. `POST /auth/devices/register`
**Auth:** required — `Authorization: Bearer <FirebaseIdToken>` (already sent
by the app's existing `AuthInterceptor`, same as `/auth/me`).

Request body:
```json
{ "deviceId": "uuid", "publicKey": "base64-x509-spki", "deviceLabel": "Pixel 8 Pro", "algorithm": "SHA256withECDSA" }
```
Action: verify the Firebase ID token, resolve the `uid`, and upsert a row
`(uid, deviceId, publicKey, deviceLabel, algorithm, created_at)`. Never store
anything but the public key.

Response:
```json
{ "registered": true, "deviceId": "uuid" }
```

## 2. `POST /auth/devices/challenge`
**Auth:** none (the user is logged out at this point by definition).

Request body:
```json
{ "email": "user@example.com", "deviceId": "uuid" }
```
Action: look up whether an active device registration exists for
`(email, deviceId)`. If yes, generate a random challenge (e.g. 32 bytes,
base64), store it server-side with a short TTL (e.g. 2–5 minutes) tied to
that device registration, and return it. If no match, return
`deviceTrusted: false` — do not reveal whether the email exists at all.

Response (trusted):
```json
{ "deviceTrusted": true, "deviceId": "uuid", "challenge": "base64", "expiresInSeconds": 180 }
```
Response (not trusted):
```json
{ "deviceTrusted": false }
```

## 3. `POST /auth/devices/verify-reset`
**Auth:** none.

Request body:
```json
{ "email": "user@example.com", "deviceId": "uuid", "challenge": "base64", "signature": "base64", "newPassword": "..." }
```
Action:
1. Look up the stored public key for `(email, deviceId)`.
2. Verify `challenge` matches the one issued and hasn't expired or been
   used already (consume it — one-time use).
3. Verify `signature` against the stored public key using
   `SHA256withECDSA` over the exact `challenge` bytes.
4. Only if the signature verifies, call the **Firebase Admin SDK**
   (`updateUser(uid, { password: newPassword })`) to set the new password.
5. Optionally mint a Firebase custom token (`createCustomToken(uid)`) so the
   app can sign the user straight back in.

Response:
```json
{ "success": true, "signInToken": "firebase-custom-token-optional" }
```
On any failure (bad signature, expired/used challenge, no such device):
```json
{ "success": false, "message": "Device verification was unsuccessful. Please try again." }
```
Keep this message generic — don't leak which specific check failed.

## 4. `POST /auth/devices/verify-login`
**Auth:** none.

Added so a successful device verification can sign the user straight in
without asking for a new password (the forgot-password flow no longer has
an email-first step or a "set new password" screen — see the app's
`ForgotPasswordDialog`). Lighter sibling of `/verify-reset`: same signature
check, but never touches the password.

Request body:
```json
{ "email": "user@example.com", "deviceId": "uuid", "challenge": "base64", "signature": "base64" }
```
Action:
1. Look up the stored public key (and `uid`) for `(email, deviceId)`.
2. Verify `challenge` matches the one issued and hasn't expired or been
   used already (consume it — one-time use), exactly like `/verify-reset`.
3. Verify `signature` against the stored public key.
4. Only if the signature verifies, call the **Firebase Admin SDK**
   (`createCustomToken(uid)`) and return it — no password is read or written.

Response:
```json
{ "success": true, "signInToken": "firebase-custom-token" }
```
On any failure, same generic shape as `/verify-reset`:
```json
{ "success": false, "message": "Device verification was unsuccessful. Please try again." }
```

## Security notes
- The app treats a "trusted" response from step 2 as nothing more than
  permission to *attempt* step 3 — the actual password change only happens
  if the signature in step 3 verifies against the stored public key. A
  local "biometric succeeded" flag is never sufficient by itself.
- Challenges must be single-use and short-lived to prevent replay.
- Consider rate-limiting `/challenge` and `/verify-reset` per email/IP.
- To revoke a device (lost/stolen phone), simply delete or deactivate its
  row — the app has no way to force a reset without a valid signature.
