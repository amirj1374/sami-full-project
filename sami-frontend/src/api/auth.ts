import type { ApiResponse } from '@/types/api'
import type {
  AuthResult,
  ChangePasswordPayload,
  CurrentUser,
  ForgotPasswordPayload,
  ResetPasswordPayload,
} from '@/types/models'
import { http, unwrap, unwrapVoid } from './http'

export interface LoginPayload {
  email: string
  password: string
}

export interface RegisterPayload {
  email: string
  password: string
  fullName: string
}

/** Authentication API calls. */
export const authApi = {
  login: (payload: LoginPayload): Promise<AuthResult> =>
    unwrap(http.post<ApiResponse<AuthResult>>('/v1/auth/login', payload)),

  register: (payload: RegisterPayload): Promise<AuthResult> =>
    unwrap(http.post<ApiResponse<AuthResult>>('/v1/auth/register', payload)),

  me: (): Promise<CurrentUser> => unwrap(http.get<ApiResponse<CurrentUser>>('/v1/users/me')),

  logout: (refreshToken: string): Promise<void> =>
    unwrapVoid(http.post<ApiResponse<unknown>>('/v1/auth/logout', { refreshToken })),

  changePassword: (payload: ChangePasswordPayload): Promise<void> =>
    unwrapVoid(http.post<ApiResponse<unknown>>('/v1/auth/change-password', payload)),

  forgotPassword: (payload: ForgotPasswordPayload): Promise<void> =>
    unwrapVoid(http.post<ApiResponse<unknown>>('/v1/auth/forgot-password', payload)),

  resetPassword: (payload: ResetPasswordPayload): Promise<void> =>
    unwrapVoid(http.post<ApiResponse<unknown>>('/v1/auth/reset-password', payload)),
}
