import type { ApiResponse, PageQuery, PageResponse } from '@/types/api'
import type {
  CreateProductPayload,
  Product,
  ProductFilter,
  UpdateProductPayload,
} from '@/types/models'
import { http, unwrap } from './http'

export type ProductListParams = PageQuery & ProductFilter

/** Product catalogue API calls. */
export const productsApi = {
  list: (params: ProductListParams = {}): Promise<PageResponse<Product>> =>
    unwrap(http.get<ApiResponse<PageResponse<Product>>>('/v1/products', { params })),

  get: (id: number): Promise<Product> =>
    unwrap(http.get<ApiResponse<Product>>(`/v1/products/${id}`)),

  create: (payload: CreateProductPayload): Promise<Product> =>
    unwrap(http.post<ApiResponse<Product>>('/v1/products', payload)),

  update: (id: number, payload: UpdateProductPayload): Promise<Product> =>
    unwrap(http.put<ApiResponse<Product>>(`/v1/products/${id}`, payload)),

  remove: (id: number): Promise<void> => http.delete(`/v1/products/${id}`).then(() => undefined),

  variants: (productId: number): Promise<Array<{ id: number; productId: number; variantCode: string; name: string; sku?: string; status: string }>> =>
    unwrap(http.get<ApiResponse<Array<{ id: number; productId: number; variantCode: string; name: string; sku?: string; status: string }>>>(`/v1/products/${productId}/variants`)),
}
