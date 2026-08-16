import type { ApiResponse, PageResponse } from '@/types/api'
import { http, unwrap } from './http'
export interface Option { id:number; label:string; parentId:number|null }
export interface AttendanceContext { companies:Option[]; branches:Option[]; users:Option[] }
export interface Employee { id:number; employeeCode:string; firstName:string; lastName:string; fullName:string; userId:number|null; companyId:number; branchId:number; jobTitle:string|null; mobile:string|null; hireDate:string|null; status:'ACTIVE'|'INACTIVE'; version:number }
export interface EmployeePayload { employeeCode:string; firstName:string; lastName:string; userId?:number|null; companyId:number|null; branchId:number|null; jobTitle?:string; mobile?:string; hireDate?:string|null; status:'ACTIVE'|'INACTIVE' }
export interface AttendanceRecord { id:number; employeeId:number; employeeCode:string; employeeName:string; workDate:string; clockIn:string; clockOut:string|null; status:string; source:string; notes:string|null; version:number }
export const attendanceApi={
 context:()=>unwrap(http.get<ApiResponse<AttendanceContext>>('/v1/attendance/context')),
 employees:(params:Record<string,unknown>)=>unwrap(http.get<ApiResponse<PageResponse<Employee>>>('/v1/attendance/employees',{params})),
 saveEmployee:(id:number|null,p:EmployeePayload)=>unwrap(id?http.put<ApiResponse<Employee>>(`/v1/attendance/employees/${id}`,p):http.post<ApiResponse<Employee>>('/v1/attendance/employees',p)),
 records:(params:Record<string,unknown>)=>unwrap(http.get<ApiResponse<PageResponse<AttendanceRecord>>>('/v1/attendance/records',{params})),
 clockIn:(employeeId:number)=>unwrap(http.post<ApiResponse<AttendanceRecord>>('/v1/attendance/clock-in',{employeeId})),
 clockOut:(employeeId:number)=>unwrap(http.post<ApiResponse<AttendanceRecord>>('/v1/attendance/clock-out',{employeeId})),
}
