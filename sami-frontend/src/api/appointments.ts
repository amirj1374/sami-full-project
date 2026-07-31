import type { ApiResponse } from '@/types/api'
import type { AppointmentCatalog, AppointmentResource, BookingPayload, BookingResult, TimeSlot } from '@/types/appointments'
import { http, unwrap } from './http'

export const appointmentsApi = {
  catalog: (): Promise<AppointmentCatalog> => unwrap(http.get<ApiResponse<AppointmentCatalog>>('/v1/appointments/catalog')),
  resources: (): Promise<AppointmentResource[]> => unwrap(http.get<ApiResponse<AppointmentResource[]>>('/v1/appointments/resources')),
  availability: (payload: { appointmentTypeId: number; resourceIds: number[]; requiredSkills: string[]; desiredStart: string; desiredEnd: string; durationMinutes?: number }, limit = 20): Promise<TimeSlot[]> =>
    unwrap(http.post<ApiResponse<TimeSlot[]>>('/v1/appointments/availability', payload, { params: { limit } })),
  create: (payload: BookingPayload): Promise<BookingResult> => unwrap(http.post<ApiResponse<BookingResult>>('/v1/appointments', payload)),
}
