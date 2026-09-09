# راهنمای Release و Deployment خودکار SAMI ERP

چرخهٔ عملیاتی در `scripts/deploy.ps1` متمرکز است و wrapperهای Git Bash برای ویندوز در اختیار هستند:

```bash
./scripts/release-deploy.sh 0.4.0
```

این دستور ابتدا وضعیت Git و validationهای backend/frontend را بررسی می‌کند، imageهای `linux/amd64` را با revision و version می‌سازد، آن‌ها را export و checksum می‌کند، به سرور منتقل می‌کند، قبل از جایگزینی از PostgreSQL backup می‌گیرد، فقط سرویس‌های backend/frontend را recreate می‌کند و health، Flyway، Spring Boot، nginx، login و API محافظت‌شده را smoke-test می‌کند. خطای critical باعث توقف فوری می‌شود.

## پیش‌نیاز و تنظیمات

تنظیمات غیرحساس را در `scripts/release-config.ps1` (از روی `release-config.example.ps1`) قرار دهید. این فایل عمداً ignore شده است. رمز، token، محتوای `.env` یا کلید خصوصی را در repository، script یا log قرار ندهید. اسکریپت موجود از OpenSSH key/agent استفاده می‌کند؛ برای اجرای unattended، کلید deploy را در `ssh-agent` یا مسیر استاندارد `~/.ssh` آماده کنید و در config فقط مسیر آن را ثبت کنید.

در Git Bash، مسیرهای دارای فاصله با wrapperها بدون تغییر کار می‌کنند. برای مشاهدهٔ مراحل بدون build/upload/deploy:

```bash
./scripts/deploy.ps1 -Mode Full -DryRun
```

یا از PowerShell:

```powershell
.\scripts\deploy.ps1 -Mode Full -DryRun -ConfigFile .\scripts\release-config.ps1
```

## Rollback

```bash
./scripts/rollback.sh 0.3.0
```

Rollback بر اساس آخرین history معتبر، imageهای application را برمی‌گرداند و database یا volumeها را restore نمی‌کند. چون Flyway forward migration است، restore دیتابیس فقط emergency procedure مستند و دستی است.

## ایمنی

- imageها با SHA256 محلی و remote مقایسه می‌شوند.
- backup در `/root/backups/` با version و timestamp ایجاد می‌شود و backup قبلی حذف نمی‌شود.
- database، volumeهای persistent و سرویس db حذف یا recreate نمی‌شوند.
- logها با فیلتر redaction تولید می‌شوند؛ با این حال secret در command line یا فایل tracked قرار ندهید.
- `-DryRun` برای بررسی preflight و مسیر اجراست و به SSH یا Docker mutation وصل نمی‌شود.

## اعتبارسنجی محلی

```powershell
.\scripts\deploy.ps1 -Mode Validate
.\scripts\tests\deployment-automation.tests.ps1
```

برای cleanup امن artifact/logهای قدیمی فقط از `-Mode Cleanup -ConfirmCleanup` استفاده کنید؛ منابع Docker نامرتبط یا volumeهای persistent حذف نمی‌شوند.
