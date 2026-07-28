/** Mirrors the backend's standard response envelope. */
export interface ApiResponse<T> {
  success: boolean
  data: T | null
  error: ApiError | null
  timestamp: string
}

export interface ApiError {
  code: string
  message: string
  fieldErrors?: FieldError[] | null
}

export interface FieldError {
  field: string
  message: string
}

/** Mirrors the backend's PageResponse. */
export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

/** Common query params for paginated/sorted/filtered list endpoints. */
export interface PageQuery {
  page?: number
  size?: number
  /** e.g. "createdAt,desc" */
  sort?: string
}
