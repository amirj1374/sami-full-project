import type { ApiResponse, PageQuery, PageResponse } from '@/types/api'
import type { FileCatalog, FileFolder, FileVersion, ManagedFile } from '@/types/files'
import { http, unwrap } from './http'

export const filesApi = {
  search: (params: PageQuery & { q?: string; category?: string; folderId?: number } = {}): Promise<PageResponse<ManagedFile>> =>
    unwrap(http.get<ApiResponse<PageResponse<ManagedFile>>>('/v1/files', { params })),
  catalog: (): Promise<FileCatalog> => unwrap(http.get<ApiResponse<FileCatalog>>('/v1/files/catalog')),
  folders: (parentId?: number): Promise<FileFolder[]> => unwrap(http.get<ApiResponse<FileFolder[]>>('/v1/files/folders', { params: { parentId } })),
  versions: (uuid: string): Promise<FileVersion[]> => unwrap(http.get<ApiResponse<FileVersion[]>>(`/v1/files/${uuid}/versions`)),
  upload: (payload: FormData): Promise<ManagedFile> => unwrap(http.post<ApiResponse<ManagedFile>>('/v1/files', payload, { headers: { 'Content-Type': 'multipart/form-data' } })),
  createFolder: (payload: { parentId?: number; name: string; description?: string }): Promise<FileFolder> =>
    unwrap(http.post<ApiResponse<FileFolder>>('/v1/files/folders', { ...payload, virtual: false, smart: false })),
  remove: (uuid: string): Promise<void> => http.delete(`/v1/files/${uuid}`).then(() => undefined),
  download: async (file: ManagedFile): Promise<void> => {
    const response = await http.get<Blob>(`/v1/files/${file.fileUuid}/content`, { responseType: 'blob' })
    const url = URL.createObjectURL(response.data)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = file.originalFilename || file.name
    anchor.click()
    URL.revokeObjectURL(url)
  },
}
