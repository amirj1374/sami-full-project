import type { ApiResponse } from '@/types/api'
import type { LegacyBatch, LegacyDataset } from '@/types/legacyImports'
import { http, unwrap } from './http'

export const legacyImportsApi = {
  list: (): Promise<LegacyBatch[]> => unwrap(http.get<ApiResponse<LegacyBatch[]>>('/v1/legacy-imports')),
  upload: (file: File): Promise<LegacyBatch> => {
    const data = new FormData(); data.append('file', file)
    // The shared client defaults JSON requests to application/json.  This
    // endpoint accepts a multipart part named `file`, so it must opt into the
    // established upload convention used by the file and customer clients.
    return unwrap(http.post<ApiResponse<LegacyBatch>>('/v1/legacy-imports', data, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }))
  },
  analyze: (id: number): Promise<LegacyBatch> => unwrap(http.post<ApiResponse<LegacyBatch>>(`/v1/legacy-imports/${id}/analyze`)),
  execute: (id: number): Promise<LegacyBatch> => unwrap(http.post<ApiResponse<LegacyBatch>>(`/v1/legacy-imports/${id}/import`)),
  datasets: (id: number): Promise<LegacyDataset[]> => unwrap(http.get<ApiResponse<LegacyDataset[]>>(`/v1/legacy-imports/${id}/datasets`)),
  files: (id: number): Promise<Array<Record<string, unknown>>> => unwrap(http.get<ApiResponse<Array<Record<string, unknown>>>>(`/v1/legacy-imports/${id}/files`)),
  messages: (id: number): Promise<Array<Record<string, unknown>>> => unwrap(http.get<ApiResponse<Array<Record<string, unknown>>>>(`/v1/legacy-imports/${id}/messages`)),
  records: (id: number): Promise<Array<Record<string, unknown>>> => unwrap(http.get<ApiResponse<Array<Record<string, unknown>>>>(`/v1/legacy-imports/${id}/records`)),
  compare: (id: number): Promise<Record<string, unknown>> => unwrap(http.post<ApiResponse<Record<string, unknown>>>(`/v1/legacy-imports/${id}/comparisons`)),
}
