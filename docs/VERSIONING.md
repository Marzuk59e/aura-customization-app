# App Versioning Guide — Aura Launcher

এই প্রজেক্টে app version manage করার নিয়ম এখানে লেখা আছে।

## কোথায় বদলাবেন

`app/build.gradle.kts` ফাইলে, `defaultConfig` ব্লকের ভেতরে **সরাসরি হাতে** বদলাবেন:

```kotlin
defaultConfig {
    ...
    versionCode = 2
    versionName = "1.0.0"
    ...
}
```

কোনো auto-calculation বা আলাদা `gradle.properties` entry নেই — দুটো লাইনই সরাসরি এখানে edit করবেন।

## নিয়ম — Semantic Versioning (MAJOR.MINOR.PATCH) for versionName

```
1  .  0  .  0
│     │     │
│     │     └── PATCH — bug fix
│     └──────── MINOR — নতুন feature (backward compatible)
└────────────── MAJOR — breaking change / বড় redesign
```

| আপনি কী করলেন | কোনটা বাড়াবেন | উদাহরণ |
|---|---|---|
| একটা crash বা ছোট bug ঠিক করলেন | PATCH | `1.0.0` → `1.0.1` |
| নতুন launcher customization feature যোগ করলেন | MINOR | `1.0.1` → `1.1.0` |
| পুরো architecture বা UI redesign করলেন | MAJOR | `1.1.0` → `2.0.0` |

**PATCH/MINOR বাড়ালে ডানের সংখ্যাগুলো ০-তে রিসেট হয়:** `1.4.8` থেকে নতুন feature দিলে → `1.5.0`, MAJOR বাড়ালে → `2.0.0`।

## versionCode-এর নিয়ম

- **অবশ্যই আগের release-এর চেয়ে বড় সংখ্যা** হতে হবে — Google Play Store এটা মানে না মানলে update reject করে দেবে।
- প্রতিটা নতুন release-এ শুধু **১ করে বাড়িয়ে যাওয়াই সবচেয়ে সহজ ও নিরাপদ**:
  ```
  Release 1 → versionCode = 1
  Release 2 → versionCode = 2   (বর্তমান)
  Release 3 → versionCode = 3
  ```
- চাইলে versionName-এর সাথে সংগতি রেখেও রাখতে পারেন, কিন্তু সেটা বাধ্যতামূলক না — শুধু মনে রাখবেন **কখনো কমানো বা repeat করা যাবে না**।

## নতুন version release করার ধাপ

1. `app/build.gradle.kts`-এ `versionCode` এবং `versionName` দুটোই হাতে বদলান
2. Gradle sync করুন (elephant আইকন বা File > Sync Project with Gradle Files)
3. Git-এ commit ও tag করুন:
   ```bash
   git add app/build.gradle.kts
   git commit -m "chore: bump version to 1.0.1 (versionCode 3)"
   git tag v1.0.1
   git push && git push --tags
   ```

Git tag রাখলে ভবিষ্যতে যেকোনো release-এর exact code state-এ ফিরে যেতে পারবেন।

> ⚠️ মনে রাখবেন: versionCode বাড়াতে ভুলে গেলে Play Store নতুন build reject করবে। প্রতিবার release দেওয়ার আগে দুটো লাইনই check করে নেবেন।
