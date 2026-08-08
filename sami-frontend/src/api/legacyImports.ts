import type { ApiResponse } from '@/types/api'
import type { LegacyBatch, LegacyDataset } from '@/types/legacyImports'
import { http, unwrap } from './http'

export const legacyImportsApi = {
  list: (): Promise<LegacyBatch[]> => unwrap(http.get<ApiResponse<LegacyBatch[]>>('/v1/legacy-imports')),
  upload: (file: File): Promise<LegacyBatch> => {
    const data = new FormData(); data.append('file', file)
    return unwrap(http.post<ApiResponse<LegacyBatch>>('/v1/legacy-imports', data))
  },
  analyze: (id: number): Promise<LegacyBatch> => unwrap(http.post<ApiResponse<LegacyBatch>>(`/v1/legacy-imports/${id}/analyze`)),
  execute: (id: number): Promise<LegacyBatch> => unwrap(http.post<ApiResponse<LegacyBatch>>(`/v1/legacy-imports/${id}/import`)),
  datasets: (id: number): Promise<LegacyDataset[]> => unwrap(http.get<ApiResponse<LegacyDataset[]>>(`/v1/legacy-imports/${id}/datasets`)),
  files: (id: number): Promise<Array<Record<string, unknown>>> => unwrap(http.get<ApiResponse<Array<Record<string, unknown>>>>(`/v1/legacy-imports/${id}/files`)),
  messages: (id: number): Promise<Array<Record<string, unknown>>> => unwrap(http.get<ApiResponse<Array<Record<string, unknown>>>>(`/v1/legacy-imports/${id}/messages`)),
  records: (id: number): Promise<Array<Record<string, unknown>>> => unwrap(http.get<ApiResponse<Array<Record<string, unknown>>>>(`/v1/legacy-imports/${id}/records`)),
  compare: (id: number): Promise<Record<string, unknown>> => unwrap(http.post<ApiResponse<Record<string, unknown>>>(`/v1/legacy-imports/${id}/comparisons`)),
}
