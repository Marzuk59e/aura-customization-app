# Aura Launcher — Project Structure Guide

এই ডকুমেন্টে `com.aura.launcher` প্যাকেজের প্রতিটা ফোল্ডার আর ফাইলের কাজ ব্যাখ্যা করা হয়েছে। এটা একটা **Clean Architecture** স্টাইলে সাজানো প্রজেক্ট — মানে UI, business logic, আর data আলাদা আলাদা layer-এ ভাগ করা।

---

## 📁 Package-এর মূলে থাকা দুইটা ফাইল

| ফাইল | কাজ |
|---|---|
| `AuraLauncherApp.kt` | পুরো app-এর entry point (`Application` class) — app চালু হওয়ার সাথে সাথে সবার আগে এটাই তৈরি হয়। এখানে সাধারণত global objects (যেমন database, network client) initialize করা হয়। |
| `MainActivity.kt` | app-এর একমাত্র Activity (এটা একটা single-activity Compose app)। এখানে `LauncherScreen` নামের enum দিয়ে ঠিক করা হয় কোন স্ক্রিন (Home/AppDrawer/Settings ইত্যাদি) কখন দেখাবে — নেভিগেশনের কেন্দ্র। |

---

## 📁 `auth/` — লগইন ও ইউজার যাচাইকরণ

ইউজার log in/sign up আর device-ভিত্তিক security যাচাই সংক্রান্ত সব UI screen ও logic এখানে।

| ফাইল | কাজ |
|---|---|
| `WelcomeScreen.kt` | app প্রথম খুললে যে welcome/onboarding স্ক্রিন দেখায় |
| `AuraAuthScreen.kt` | মূল login/register স্ক্রিন (UI) |
| `AuthScreens.kt` | ইউজার প্রোফাইল দেখানোর স্ক্রিন (`ProfileScreen`) |
| `AuthViewModel.kt` | Login/register-এর state ও logic — সফল হলো, error এলো, loading চলছে ইত্যাদি ট্র্যাক করে |
| `DeviceAuthViewModel.kt` | Device-ভিত্তিক verification (নতুন ডিভাইস থেকে লগইন করলে সেটা verify/register করা) নিয়ন্ত্রণ করে |
| `GuestGateDialog.kt` | Guest (লগইন ছাড়া) ইউজার কোনো লগইন-দরকারি ফিচার ব্যবহার করতে চাইলে যে popup দেখায় |

---

## 📁 `core/` — সাধারণ, পুরো app জুড়ে ব্যবহৃত টুলস

এমন সব জিনিস যা কোনো একটা feature-এর না, বরং পুরো app-এ বিভিন্ন জায়গায় দরকার হয় (তাই আপনার screenshot-এ এই ফোল্ডারটাই highlight করা ছিল)।

**`core/audio/`**
| ফাইল | কাজ |
|---|---|
| `SoundEngine.kt` | App-এর ভেতরের sound effect (ক্লিক, সোয়াইপ ইত্যাদি সাউন্ড) চালানোর জন্য |

**`core/database/`**
| ফাইল | কাজ |
|---|---|
| `AppDatabase.kt` | Room database সেটআপ — লোকাল ডিভাইসে ডেটা সংরক্ষণের মূল কনফিগারেশন |

**`core/datastore/`**
| ফাইল | কাজ |
|---|---|
| `Preferences.kt` | ইউজারের settings/preferences (থিম, লগইন স্টেট ইত্যাদি) DataStore দিয়ে সংরক্ষণ করার জন্য `LauncherPreferences` ও `AuthPreferences` ক্লাস |

**`core/network/`**
| ফাইল | কাজ |
|---|---|
| `ApiClient.kt` | Retrofit/OkHttp দিয়ে network call করার জন্য মূল client সেটআপ |
| `AuthInterceptor.kt` | প্রতিটা network request-এ automatic ভাবে auth token জুড়ে দেয় |
| `NetworkResult.kt` | Network call-এর ফলাফল (Success/Error/Loading) সুন্দরভাবে represent করার জন্য একটা wrapper type |

**`core/security/`**
| ফাইল | কাজ |
|---|---|
| `BiometricAuthHelper.kt` | ফিঙ্গারপ্রিন্ট/ফেস আনলক দিয়ে biometric যাচাই করার হেল্পার |
| `DeviceCredentialManager.kt` | ডিভাইসের PIN/pattern/password দিয়ে যাচাই পরিচালনা করে |

**`core/theme/`**
| ফাইল | কাজ |
|---|---|
| `Color.kt` | App-এর color palette ও থিম প্রয়োগ করার ফাংশন |
| `Theme.kt` | পুরো app-এর জন্য Compose থিম (`AuraLauncherTheme`) সংজ্ঞায়িত করে |
| `Type.kt` | Font/Typography সেটআপ |
| `AuraAnimation.kt` | কাস্টম বাটন animation (যেমন চাপ দিলে বাউন্স হওয়া) |

**`core/utils/`**
| ফাইল | কাজ |
|---|---|
| `AppIconBitmap.kt` | কোনো app-এর আইকন bitmap হিসেবে লোড করার হেল্পার |
| `AppWidgetHostHelper.kt` | Android widget হোস্ট করার সাহায্যকারী ফাংশন |
| `PackageManagerHelper.kt` | ফোনে ইনস্টল করা app-গুলোর তথ্য (নাম, আইকন ইত্যাদি) বের করার হেল্পার |

**`core/viewmodel/`**
| ফাইল | কাজ |
|---|---|
| `ViewModelFactory.kt` | সব ViewModel তৈরির জন্য কেন্দ্রীয় factory — dependency injection ছাড়াই ViewModel-এ constructor argument পাঠানো যায় |

---

## 📁 `customization/` — লঞ্চার কাস্টমাইজেশন ফিচার

ইউজার হোম স্ক্রিনের থিম, ওয়ালপেপার, আইকন, উইজেট কাস্টমাইজ করার সব ফিচার এখানে।

| ফাইল | কাজ |
|---|---|
| `EditableCustomizationScreen.kt` | কাস্টমাইজেশন এডিট করার মূল স্ক্রিন |

**`customization/explore/`** — নতুন থিম/ওয়ালপেপার আবিষ্কার করার সেকশন
| ফাইল | কাজ |
|---|---|
| `ExploreScreen.kt` | Explore ট্যাবের মূল স্ক্রিন |
| `ExploreComponents.kt` | Explore স্ক্রিনের ছোট ছোট UI অংশ (ক্যাটাগরি গ্রিড, ব্যাজ ইত্যাদি) |
| `ExploreTabsContent.kt` | Explore-এর ভেতরের বিভিন্ন ট্যাবের কনটেন্ট |
| `ExploreViewModel.kt` | Explore স্ক্রিনের state ও data ম্যানেজ করে |
| `WallpaperExploreScreen.kt` | শুধু ওয়ালপেপার browse করার জন্য আলাদা স্ক্রিন |

**`customization/icons/`**
| ফাইল | কাজ |
|---|---|
| `IconComponents.kt` | আইকনের shape (গোল/স্কয়ার) ও style pack বেছে নেওয়ার UI |

**`customization/setups/`** — সংরক্ষিত setup/theme combo
| ফাইল | কাজ |
|---|---|
| `SavedSetupsScreens.kt` | ইউজারের সেভ করা কাস্টমাইজেশন setup-গুলো দেখানোর স্ক্রিন |
| `SavedSetupsViewModel.kt` | সেভ করা setup-এর data ম্যানেজ করে |

**`customization/vibesync/`**
| ফাইল | কাজ |
|---|---|
| `VibeSyncComponents.kt` | ওয়ালপেপারের রঙ বিশ্লেষণ করে সেই অনুযায়ী থিম রঙ মিলিয়ে দেওয়ার ফিচার (`VibeSyncEngine`) |

**`customization/widgets/`**
| ফাইল | কাজ |
|---|---|
| `WidgetModels.kt` | Widget-এর টাইপ ও তথ্যের ডেটা মডেল |
| `WidgetPickerSheet.kt` | Widget বেছে নেওয়ার bottom sheet UI |
| `widgets/components/DigitalClockWidget.kt` | ডিজিটাল ঘড়ি widget |
| `widgets/components/AdditionalWidgets.kt` | অন্যান্য widget (analog clock, battery gauge ইত্যাদি) |

---

## 📁 `data/` — ডেটা লেয়ার (Repository Pattern-এর "Data" অংশ)

Database, network, আর অন্য যেকোনো raw data source-এর সাথে সরাসরি কথা বলা কোড এখানে থাকে।

**`data/local/`** — ফোনের মধ্যে সংরক্ষিত ডেটা (Room database)
| ফাইল | কাজ |
|---|---|
| `dao/Daos.kt` | Database query করার interface (`AppDao`, `HomeItemDao`, `FolderDao`, `UserDao`, `TrustedDeviceDao`) |
| `entities/Entities.kt` | Database টেবিলের structure define করে (`AppEntity`, `HomeItemEntity` ইত্যাদি) |

**`data/remote/`** — সার্ভার থেকে আসা ডেটা
| ফাইল | কাজ |
|---|---|
| `api/Apis.kt` | সার্ভার API endpoint-এর interface (`WallpaperApi`, `IconPackApi`, `ThemeApi`, `WidgetApi`, `RemoteConfigApi`) |
| `dto/RemoteDtos.kt` | সার্ভার থেকে আসা JSON response-এর structure (`WallpaperDto`, `ThemeDto` ইত্যাদি) |

**`data/repository/`** — Domain layer আর data source-এর মাঝে সেতু
| ফাইল | কাজ |
|---|---|
| `RepositoryImpls.kt` | লোকাল ফিচারগুলোর repository বাস্তবায়ন (App, Home, Auth, DeviceAuth, SavedSetup) |
| `RemoteRepositoryImpls.kt` | সার্ভার-নির্ভর ফিচারগুলোর repository বাস্তবায়ন (Wallpaper, IconPack, Theme) |

---

## 📁 `domain/` — Business Logic লেয়ার (framework-independent)

এখানে থাকা কোড Android, Database, বা Network কিছুর উপরই নির্ভর করে না — শুধু pure business rule।

| ফাইল | কাজ |
|---|---|
| `model/Models.kt` | মূল business মডেল (`AppInfo`, `HomeItem`, `Folder`, `User` ইত্যাদি) |
| `model/RemoteModels.kt` | সার্ভার-সম্পর্কিত business মডেল (`RemoteWallpaper`, `RemoteTheme` ইত্যাদি) |
| `repository/Repositories.kt` | Repository-গুলোর contract/interface (আসল implementation `data/` ফোল্ডারে) |
| `repository/RemoteRepositories.kt` | সার্ভার-নির্ভর repository-র interface |
| `usecase/UseCases.kt` | একেকটা নির্দিষ্ট কাজ (যেমন "ইনস্টল করা app লিস্ট আনা", "একটা app চালু করা", "লগইন করা") আলাদা আলাদা ক্লাসে ভাগ করা |

---

## 📁 `launcher/` — মূল হোম-স্ক্রিন লঞ্চার ফিচার

এই অ্যাপের আসল "লঞ্চার" অংশ — হোম স্ক্রিন, অ্যাপ ড্রয়ার, ফোল্ডার, সেটিংস।

**`launcher/home/`**
| ফাইল | কাজ |
|---|---|
| `HomeScreen.kt` | মূল হোম স্ক্রিন UI |
| `HomeGridComponents.kt` | হোম স্ক্রিনের গ্রিড, ডক ইত্যাদি ছোট UI অংশ |
| `HomeViewModel.kt` | হোম স্ক্রিনের state/logic |

**`launcher/appdrawer/`**
| ফাইল | কাজ |
|---|---|
| `AppDrawerScreen.kt` | সব ইনস্টল করা app দেখানোর ড্রয়ার UI |
| `AppDrawerViewModel.kt` | App drawer-এর data/state |

**`launcher/folder/`**
| ফাইল | কাজ |
|---|---|
| `FolderDialog.kt` | হোম স্ক্রিনে app folder খুললে যে popup দেখায় |

**`launcher/settings/`**
| ফাইল | কাজ |
|---|---|
| `SettingsScreen.kt` | App-এর সেটিংস স্ক্রিন ও তার ViewModel |

**`launcher/widget/`**
| ফাইল | কাজ |
|---|---|
| `AuraAppWidgetHost.kt` | Android system widget (যেমন ঘড়ি, ক্যালেন্ডার widget) হোম স্ক্রিনে বসানোর মূল লজিক |

---

## 📁 `receiver/` — সিস্টেম ইভেন্ট শোনা

| ফাইল | কাজ |
|---|---|
| `PackageChangeReceiver.kt` | ফোনে কোনো app ইনস্টল/আনইনস্টল হলে Android system থেকে সেই সিগন্যাল ধরার `BroadcastReceiver` |
| `AppChangeCallbackManager.kt` | সেই সিগন্যাল পাওয়ার পর app-এর ভেতরে কাকে কাকে জানাতে হবে তা ম্যানেজ করে |

---

## 🗺️ সংক্ষেপে — এই architecture-টা কেন এভাবে সাজানো

```
UI (auth, customization, launcher folders)
        ↓ ব্যবহার করে
domain/usecase  →  domain/repository (interface)
        ↑ বাস্তবায়ন করে
data/repository  →  data/local (database) / data/remote (server)
```

এটাকে বলে **Clean Architecture / MVVM** — প্রতিটা layer শুধু তার ঠিক নিচের layer-কে চেনে, উপরেরটাকে না। এর সুবিধা: ভবিষ্যতে যদি database বদলাতে চান (Room থেকে অন্য কিছু), শুধু `data/` ফোল্ডার বদলালেই হবে — `domain/` বা UI-এর কোনো কোড টাচ করতে হবে না।
