# Authentication and authorization

## Staff authentication

Staff authentication uses JWT access and refresh tokens. Backend entry points
are `auth/AuthController`, `AuthService`, `RefreshTokenService`, the JWT filter,
and `security/jwt/JwtService`. Password hashing is delegated to Spring Security
password encoding.

The Vue auth store persists both tokens through `src/api/tokenStorage.ts`.
`src/api/http.ts` attaches access tokens and coordinates one refresh request
when concurrent calls receive 401.

## RBAC

Roles and permissions are database-driven. Backend methods use
`@PreAuthorize("@authz...")`; `Authz` grants super-admin bypass. The frontend
uses route metadata, `auth.can`, directives, and dynamic menu data only to
present allowed features. These checks never replace backend enforcement.

Permission codes generally follow `<resource>:<action>`. New permissions
require a forward Flyway migration, backend enforcement, frontend presentation
checks, menu/module linkage where relevant, and English/Persian labels.

## Portal

Portal tokens use a separate service and must use a secret distinct from the
staff JWT secret. Portal domain/service foundations exist, but no verified
complete public controller/UI flow exists.

## Public surface and risks

Inspect `SecurityConfig` before changing public endpoints. CORS is configured,
while the stateless bearer-token design disables CSRF. Because tokens are in
`localStorage`, an XSS can expose them; moving to HttpOnly cookies requires an
explicit CSRF and backend contract change.
