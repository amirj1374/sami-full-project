import type { ApiResponse } from '@/types/api'
import type { MenuItem } from '@/types/models'
import { http, unwrap } from './http'

/** Navigation menu API calls. */
export const menuApi = {
  /** Enabled modules the current user may view, ordered by display order. */
  list: (): Promise<MenuItem[]> => unwrap(http.get<ApiResponse<MenuItem[]>>('/v1/menu')),
}
