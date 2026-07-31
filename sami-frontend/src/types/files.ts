export interface ManagedFile {
  fileUuid: string
  fileCode: string
  name: string
  originalFilename: string
  description?: string
  categoryCode: string
  categoryName: string
  statusCode: string
  statusName: string
  downloadable: boolean
  folderId?: number
  folderPath?: string
  sizeBytes: number
  mimeType?: string
  currentVersion?: string
  deleted: boolean
  createdAt: string
  updatedAt: string
}

export interface FileFolder { id: number; parentId?: number; name: string; path: string; description?: string; system: boolean }
export interface FileCategory { id: number; code: string; name: string; maxBytes?: number; allowedMimeTypes: string[]; allowedExtensions: string[] }
export interface FileCatalog { categories: FileCategory[] }
export interface FileVersion { id: number; label: string; sizeBytes: number; mimeType?: string; comment?: string; current: boolean; createdAt: string }
