# گزارش آزمایشی گردش‌کار Verification

این گزارش نخستین اجرای Skill آزمایشی `sami-verification-workflow` روی مشکل
واقعی Sales Save است. این Skill فقط برای ثبت شواهد و پیشنهاد سطح آزمون است و
هیچ Release Gate یا مانع اجباری ایجاد نمی‌کند.

## Skillهای بررسی‌شده

| Skill | تمرکز و نوع شواهد |
| --- | --- |
| `sami-release-gate` | آمادگی انتشار؛ build، تست، مهاجرت، امنیت، Docker و در صورت امکان Browser؛ نتیجه رسمی gate می‌دهد. |
| `sami-backend-builder` | پیاده‌سازی/بازبینی Spring Boot، API، تراکنش، امنیت، migration و تست‌های backend. |
| `sami-migration-guardian` | صحت و سازگاری Flyway، schema، constraint، seed و migration history. |
| `sami-contract-validator` | تطبیق قرارداد REST/DTO/validation/error/permission بین backend و frontend. |
| `sami-frontend-builder` | پیاده‌سازی و بررسی Vue/TypeScript، فرم، state، API، RTL و responsive. |
| `sami-project-context` | استخراج context معتبر پروژه و فرمان‌های واقعی، بدون تغییر فایل. |
| `sami-architecture-auditor` | audit تحلیلی و evidence-based معماری؛ ذاتاً read-only. |
| `sami-ui-workflow-tester` | اجرای journey واقعی Browser، Console/Network، persistence، permission و viewport. |

هم‌پوشانی اصلی در build و تست frontend/backend است؛ تفاوت این است که release
gate برای تصمیم انتشار، builderها برای اجرای تغییر، contract برای مرز دو طرف،
و UI tester برای رفتار واقعی کاربر شواهد تولید می‌کنند. کمبود هر مرز (مثلاً
نداشتن Browser یا persistence) باید جداگانه ثبت شود و از روی source یا build
به‌طور ضمنی PASS تلقی نشود.

## گردش‌کار قبلی و گردش‌کار پیشنهادی

گردش‌کار قبلی معمولاً روی source review، تست frontend، type-check و build
متمرکز بود. گردش‌کار آزمایشی ابتدا رفتار و مرز آن را تعیین می‌کند، سپس سطح
مناسب را پیشنهاد می‌دهد و شواهد را به‌صورت مستقل ثبت می‌کند:

`Source → Unit/Contract → Integration → Browser → Network/API → Persistence → Production-like → Server`

برای Sales Save، سطح 1 و 2 انجام‌شده‌اند؛ سطح 4 برای اثبات کلیک و request لازم
است و سطوح 3، 5 و 6 فقط در صورت وجود backend/محیط مربوط پیشنهاد می‌شوند.

## اجرای آزمایشی Sales Save

| Evidence | Status | شواهد |
| --- | --- | --- |
| Source | PASS | `SalesView.vue` دیگر به `crypto.randomUUID()` در مسیر ثبت فروش وابسته نیست؛ قرارداد کلید idempotency اختیاری است و helper مشترک فقط در مصرف‌های لازم استفاده می‌شود. |
| Frontend unit/contract | PASS | در `sami-frontend`، فرمان `npm test -- --run` با 46 تست، 46 موفق، 0 شکست، 0 خطا و 0 skip اجرا شد. تست `Sales Save is independent of secure-context UUID support` نیز موفق بود. |
| Type-check | NOT RUN | در این اجرای آزمایشی خروجی نهایی مستقل برای type-check ثبت نشد. |
| Build | NOT RUN | در این اجرای آزمایشی خروجی نهایی مستقل برای build ثبت نشد. |
| Browser | NOT RUN | Browser automation با خطای محیطی `failed to write kernel assets: The system cannot find the path specified` قابل اجرا نشد. |
| Network/API | NOT RUN | چون Browser journey اجرا نشد، POST واقعی `/api/v1/sales` مشاهده نشد. |
| Persistence | NOT RUN | بدون ثبت واقعی از UI، ماندگاری پس از refresh اثبات نشد. |
| Production-like | NOT RUN | در این مرحله Docker/release اجرا نشد. |
| Server | NOT RUN | سرور واقعی در scope این آزمایش نبود. |

## مقایسه نتیجه

بهبود اصلی، تفکیک روشن شواهد است: تست موفق frontend دیگر به‌عنوان اثبات
Browser، Network یا persistence گزارش نمی‌شود و سطح لازم برای یک مشکل Save
به‌صراحت Level 4 مشخص است. این قالب همچنین مانع گم‌شدن تفاوت بین source، تست
قرارداد و رفتار runtime می‌شود.

در این اجرای آزمایشی Browser، Network، persistence، production-like و server
بهبود نیافتند چون محیط مربوط در دسترس نبود؛ این موارد به‌صورت `NOT RUN` ثبت
شده‌اند، نه به‌عنوان شکست اجباری و نه به‌عنوان PASS ضمنی.

## پیشنهاد

**YES** — این Skill ارزش دارد در مرحله بعد به‌صورت کنترل‌شده در QA/release
policy بررسی شود؛ پیش از رسمی‌کردن باید دامنه الزام‌ها، روش نگهداری evidence و
رفتار آن در CI جداگانه تصمیم‌گیری شود. در نسخه فعلی هیچ policy یا gate اجباری
به پروژه اضافه نشده است.

