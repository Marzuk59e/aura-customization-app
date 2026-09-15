# Device Auth Backend — Setup (Bangla + English)

তোমার `DEVICE_AUTH_BACKEND_CONTRACT.md`-এর ৩টা endpoint এখানে পুরোপুরি implement করা আছে। এই ফোল্ডারের ভেতরের structure পুরোটাই self-contained — শুধু নিচের ধাপগুলো ফলো করলেই `aura-launcher.unaux.com`-এ বসে যাবে।

## 1. Composer dependency install (তোমার কম্পিউটারে)

তোমার কম্পিউটারে (hosting-এ না) Composer দিয়ে চালাও:

```
composer install
```

এতে `vendor/` ফোল্ডার তৈরি হবে (Firebase Admin SDK সহ)। **পুরো ফোল্ডার (vendor সহ)** FTP/File Manager দিয়ে হোস্টিং-এ আপলোড করবে। যদি তোমার কম্পিউটারে Composer না থাকে, বলো — আমি `vendor/` সহ পুরো জিপ বানিয়ে দিতে পারি (সাইজ একটু বড় হবে)।

## 2. Database — schema.sql ইমপোর্ট করো

phpMyAdmin খুলে তোমার existing database সিলেক্ট করে SQL ট্যাবে `schema.sql`-এর কন্টেন্ট পেস্ট করে Run করো। এটা ৩টা নতুন টেবিল বানাবে (`trusted_devices`, `device_challenges`, `rate_limit_hits`) — তোমার আগের কোনো টেবিল ছোঁবে না।

## 3. `config.php` ভরাও

```php
'db' => [
    'host' => '...',      // সাধারণত localhost বা তোমার hosting-এর DB host
    'name' => '...',
    'user' => '...',
    'pass' => '...',
],
```

## 4. Firebase service-account key বসাও

Firebase Console → Project Settings → Service Accounts → **Generate new private key** — ডাউনলোড হওয়া JSON ফাইলটা রাখো:

```
secure/service-account.json
```

(এই ফোল্ডার `.htaccess` দিয়ে আগে থেকেই ব্লক করা, ওয়েব থেকে কেউ এটা ডাউনলোড করতে পারবে না)

## 5. আপলোড লোকেশন

পুরো ফোল্ডারের কন্টেন্ট (এই README-এর ভাইবোন ফাইলগুলো — `config.php`, `composer.json`, `vendor/`, `lib/`, `secure/`, `api/`) তোমার হোস্টিং-এর সেই রুটে বসাও যেখানে `/api/v1/auth/me` এখন কাজ করছে — অর্থাৎ `api/v1/auth/devices/register.php` ফাইলটা ঠিক `https://aura-launcher.unaux.com/api/v1/auth/devices/register.php` URL-এ hit হওয়া উচিত। যদি তোমার existing backend কোনো router/front-controller ব্যবহার করে (single index.php দিয়ে সব route হ্যান্ডল হয়, আলাদা .php ফাইল না), তাহলে জানিও — সেই অনুযায়ী adjust করে দেব।

## 6. ⚠️ গুরুত্বপূর্ণ: Authorization header

কিছু শেয়ার্ড হোস্টিং (PHP-CGI/FastCGI মোডে চললে) `Authorization` header PHP পর্যন্ত পৌঁছাতে দেয় না — ফলে `/auth/devices/register` সবসময় 401 দেবে যদিও app ঠিকমতোই টোকেন পাঠাচ্ছে। যদি এমন হয়, root `.htaccess`-এ এই লাইনটা যোগ করো:

```
CGIPassAuth On
```

(তোমার existing `/auth/me` যদি ইতিমধ্যে টোকেন ঠিকমতো পড়তে পারছে, তাহলে এটা লাগবে না — ওটা একই মেকানিজম ব্যবহার করে।)

## 7. টেস্ট করা (curl দিয়ে)

```bash
# 2. Challenge (email/deviceId ভুল দিলে না-ট্রাস্টেড আসবে, normal)
curl -X POST https://aura-launcher.unaux.com/api/v1/auth/devices/challenge \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","deviceId":"some-uuid"}'
# expect: {"deviceTrusted":false}
```

আসল টেস্ট app দিয়েই করা ভালো (register → login → "enable device recovery" প্রম্পট → পরে logout করে forgot-password flow-এ device verify)।

## যা এই backend কখনো স্টোর করে না
প্রাইভেট কি, ফিঙ্গারপ্রিন্ট/ফেস ডেটা, PIN — এগুলোর কোনোটাই কোনো endpoint-এ আসে না, শুধু public key আর একটা signature আসে।

## Verify করার সময় যা ঠিক করা হয়েছে
- **`api/v1/auth/devices/.htaccess` (নতুন)** — app extension ছাড়া URL কল করে (`.../auth/devices/register`, `.../auth/me`-এর প্যাটার্নেই)। এই rewrite rule সেটাকে `.php` ফাইলে ম্যাপ করে। তোমার `/auth/me` যদি ইতিমধ্যে এভাবেই কাজ করে (MultiViews বা router-এর কারণে), এই ফাইল redundant কিন্তু নিরীহ।
- **`lib/request.php` — `client_ip()`** — আগে `X-Forwarded-For` header সরাসরি trust করা হচ্ছিল, যেটা client নিজেই বদলাতে পারে (rate-limit বাইপাসের রিস্ক)। এখন শুধু `REMOTE_ADDR` ব্যবহার করে, যদি না তুমি নিশ্চিত হও যে reverse proxy-র পেছনে আছো (তখন `TRUST_FORWARDED_FOR = true` করে দিও)।

বাকি সবকিছু (crypto verification, atomic challenge consumption, transaction ordering, Firebase Admin SDK কল, error messaging) সঠিক এবং spec অনুযায়ী — কোনো পরিবর্তন লাগেনি।
