export interface Loan {
  id: number;
  applicantName: string;
  applicantEmail: string;
  birthDate: string;       // ISO date string
  monthlyIncome: number;
  creditScore: number;
  amount: number;
  termMonths: number;
  monthlyInstallment: number;
  status: LoanStatus;
  createdAt: string;       // ISO datetime string
  notes: string | null;
}

export type LoanStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'DISBURSED';

export interface LoanRequest {
  applicantName: string;
  applicantEmail: string;
  birthDate: string;
  monthlyIncome: number;
  creditScore: number;
  amount: number;
  termMonths: number;
}

export interface LoanStats {
  total: number;
  pending: number;
  approved: number;
  rejected: number;
  portfolio: number;
}
