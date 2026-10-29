# معماری یادداشتی (Yaddashty)

نسخهٔ ۱ — یک اپلیکیشن یادداشت آفلاین برای اندروید با Kotlin و Jetpack Compose.

## ۱. اهداف طراحی

| هدف | تصمیم |
| --- | --- |
| آفلاین‌بودن کامل | هیچ سرور و API خارجی وجود ندارد |
| سادگی نسخهٔ اول | تک‌ماژول (`:app`)، بدون Hilt و بدون کد تولیدی پیچیده |
| مقیاس‌پذیری داده | Room + Flow؛ UI همیشه به‌روز می‌ماند |
| راست‌به‌چپ واقعی | `supportsRtl` + `LocalLayoutDirection = Rtl` در ریشهٔ Compose |
| ظاهر ثابت و آرام | پالت کاغذی/سبز، کارت‌های نرم، بدون تصویر سنگین |

## ۲. لایه‌ها

```
┌──────────────────────────────────────────────┐
│ Presentation — Jetpack Compose               │
│ NotesListScreen · EditorScreen · Settings    │
└───────────────────────┬──────────────────────┘
                        │ StateFlow / رویدادها
┌───────────────────────▼──────────────────────┐
│ NotesViewModel (MVVM)                        │
│ فهرست‌ها، شمارنده‌ها، تم، عملیات CRUD          │
└───────────────────────┬──────────────────────┘
                        │ suspend / Flow
┌───────────────────────▼──────────────────────┐
│ NoteRepository + SettingsRepository          │
└──────────────┬─────────────────────┬─────────┘
               │                     │
┌──────────────▼─────────┐  ┌────────▼─────────┐
│ Room (yaddashty.db)    │  │ DataStore         │
│ NoteDao · Note         │  │ theme_mode        │
└────────────────────────┘  └───────────────────┘
```

### Presentation
- `MainActivity` تنها Activity برنامه است. با `NavHost` مسیرها تعریف می‌شوند: `notes`، `archive`، `trash`، `settings`، `editor/{noteId}`.
- هر صفحه یک composable مستقل است. `NotesListScreen` با پارامتر `NoteMode` برای سه حالت فهرست فعال، بایگانی و سطل زباله استفاده می‌شود (بدون تکرار کد).
- جستجو و فیلتر برچسب در حافظه و روی فهرست جاری انجام می‌شود؛ اندازهٔ داده‌ها در نسخهٔ اول کوچک است.

### ViewModel
- فهرست‌ها با `stateIn(viewModelScope, WhileSubscribed(5s))` از Room جمع می‌شوند تا هم UI همیشه زنده باشد و هم در پس‌زمینه کار نکند.
- عملیات CRUD در `viewModelScope.launch` اجرا می‌شوند تا خروج از صفحه باعث لغو ذخیره‌سازی نشود.
- `NotesViewModelFactory` وابستگی‌ها را از `YaddashtyApp.container` می‌گیرد؛ بنابراین هیچ کتابخانهٔ تزریق وابستگی لازم نیست.

### Data
- `Note` با کلید اصلی خودکار: عنوان، متن، رنگ، برچسب‌ها (رشتهٔ جداشده با ویرگول)، سنجاق، بایگانی، سطل زباله، زمان ساخت و زمان ویرایش.
- DAO برای هر حالت فهرست query جدا دارد تا ترتیب و بهینه‌بودن مشخص باشد.
- حذف نرم: `trashed = true`؛ حذف واقعی فقط از سطل زباله انجام می‌شود.
- سنجاق: `ORDER BY pinned DESC, updatedAt DESC` باعث می‌شود یادداشت‌های مهم بالای فهرست بمانند.
- `SettingsRepository` با DataStore فقط یک کلید (`theme_mode`) دارد: `system` / `light` / `dark`.

## ۳. جریان ذخیره‌سازی

1. کاربر در ویرایشگر تایپ می‌کند؛ وضعیت محلی `dirty = true` می‌شود.
2. پس از ۹۰۰ میلی‌ثانیه سکون (debounce در `LaunchedEffect`)، `saveEdited` فراخوانی می‌شود.
3. بستن صفحه یا دکمهٔ بازگشت همیشه پیش از خروج ذخیره می‌کند (`BackHandler`).
4. Repository ابتدا رکورد موجود را می‌خواند و فقط فیلدهای قابل ویرایش را تغییر می‌دهد؛ `createdAt`، بایگانی و وضعیت سطل زباله حفظ می‌شوند.

## ۴. مدل داده

```sql
CREATE TABLE notes (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  title TEXT NOT NULL,
  body TEXT NOT NULL,
  color INTEGER NOT NULL,
  tags TEXT NOT NULL,
  pinned INTEGER NOT NULL,
  archived INTEGER NOT NULL,
  trashed INTEGER NOT NULL,
  createdAt INTEGER NOT NULL,
  updatedAt INTEGER NOT NULL
);
```

## ۵. بیلد و توزیع

- Gradle 8.9، AGP 8.7.3، Kotlin 2.0.21، Compose BOM 2024.10.01، Room 2.6.1 با KSP.
- `versionCode = 1` و `versionName = "1"` تا پایان مرحلهٔ پیش‌انتشار.
- GitHub Actions (`.github/workflows/android.yml`) در هر push روی `main`:
  1. JDK 17 + Android SDK 35 را نصب می‌کند.
  2. `gradle :app:assembleDebug` را اجرا می‌کند.
  3. APK را به‌عنوان `releases/yaddashty-v1.apk` داخل مخزن می‌گذارد.
  4. Release با تگ `v1` را می‌سازد/به‌روزرسانی می‌کند و همان APK را به آن پیوست می‌کند.
  5. `releases/build-info.json` را با حجم، SHA-256 و زمان بیلد می‌سازد و بلوک «وضعیت بیلد» در README را به‌روز می‌کند.

## ۶. نکات امنیتی و حریم خصوصی

- بدون مجوز اینترنت در Manifest؛ اپلیکیشن فیزیکاً نمی‌تواند داده‌ای بفرستد.
- بدون آنالیتیکس و بدون تبلیغات.
- پشتیبان‌گیری فقط با اکشن `ACTION_SEND` و انتخاب کاربر انجام می‌شود.
- `allowBackup=true` برای انتقال بین گوشی‌ها فعال است و `data_extraction_rules` فقط دیتابیس یادداشت‌ها را شامل می‌شود.

## ۷. نقشهٔ راه فنی

- مهاجرت از برچسب رشته‌ای به جدول `tags` با رابطهٔ چندبه‌چند.
- `Paging 3` در صورت بزرگ‌شدن فهرست.
- یادآور با `AlarmManager`/`WorkManager` (افزودن مجوز `POST_NOTIFICATIONS` برای API 33+).
- تست UI با `androidx.compose.ui:ui-test-junit4` و تست DAO با `room-testing`.
