export interface SalaryRequest {
  amount: number;
  currency: string;
  effectiveFrom: string;
}

export interface SalaryResponse {
  id: number;
  employeeId: number;
  amount: number;
  currency: string;
  effectiveFrom: string;
  createdAt: string;
}
