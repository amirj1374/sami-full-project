# Security

## Confirmed controls

- Spring Security JWT authentication for staff.
- Separate portal token service/secret contract.
- Database-driven RBAC and method-level permission checks.
- Password hashing through Spring Security.
- Bean validation and centralized error handling.
- Production Compose fail-fast requirements for principal secrets.
- Backend container runs as a non-root user.

## Known risks

- Staff access and refresh tokens are stored in browser `localStorage`.
- Tenant isolation is transitional rather than centrally mandatory.
- Example development credentials are intentionally convenient and must never
  reach production.
- Direct production startup outside Compose may not have equivalent fail-fast
  safeguards.
- Swagger/public endpoint exposure requires deployment review.
- External provider secret handling is undefined because adapters are absent.
- General in-process events are not crash-durable.

## Review checklist

- Authenticate and authorize every sensitive endpoint server-side.
- Derive tenant/company/branch scope from trusted context.
- Avoid logging credentials, tokens, OTPs, personal data or file content.
- Validate upload type, size, path, ownership and download authorization.
- Use placeholders in documentation and examples.
- Assess XSS, CSRF, CORS and token revocation together before changing auth.
- Audit lifecycle, approval, export, impersonation and cross-tenant actions.
