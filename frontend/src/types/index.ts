// API Types for Credit Card Core System

// ============ Account Types ============
export interface Account {
  account_id: string;
  customer_id: string;
  token: string;
  credit_limit: number;
  temp_limit: number;
  used_amount: number;
  frozen_amount: number;
  installment_used: number;
  available_amount: number;
  billing_day: number;
  due_day: number;
  status: 'ACTIVE' | 'FROZEN' | 'CLOSED';
  open_date: string;
  close_date?: string;
  created_at: string;
  updated_at: string;
}

export interface AccountResponse {
  account_id: string;
  token: string;
  status: 'ACTIVE' | 'FROZEN' | 'CLOSED';
}

export interface LimitAdjustmentRequest {
  new_limit: number;
  type: 'PERMANENT' | 'TEMP';
  reason: string;
}

export interface StatusChangeRequest {
  status: 'ACTIVE' | 'FROZEN' | 'CLOSED';
  reason: string;
}

// ============ Transaction Types ============
export type TransactionType = 
  | 'PURCHASE' 
  | 'WITHDRAWAL' 
  | 'REFUND' 
  | 'REPAYMENT' 
  | 'REVERSAL' 
  | 'INSTALLMENT';

export type TransactionStatus = 
  | 'PENDING' 
  | 'COMPLETED' 
  | 'REVERSED' 
  | 'REFUNDED';

export interface Transaction {
  txn_id: string;
  account_id: string;
  installment_id?: string;
  txn_type: TransactionType;
  txn_amount: number;
  principal_amount: number;
  fee_amount: number;
  available_amount_before: number;
  available_amount_after: number;
  merchant_id: string;
  merchant_name: string;
  merchant_category: string;
  terminal_id: string;
  auth_code: string;
  reference_no: string;
  txn_time: string;
  settlement_date?: string;
  status: TransactionStatus;
  created_at: string;
}

export interface TransactionListResponse {
  transactions: Transaction[];
  total: number;
  page: number;
  page_size: number;
}

export interface AuthorizeRequest {
  token: string;
  amount: number;
  currency: string;
  merchant_id: string;
  merchant_category: string;
  terminal_id: string;
  txn_type: 'PURCHASE' | 'WITHDRAWAL';
  channel: 'POS' | 'APP' | 'Web' | 'Recurring';
}

export interface AuthorizeResponse {
  auth_code: string;
  reference_no: string;
  status: 'APPROVED' | 'DECLINED';
  account_id: string;
  available_amount: number;
}

// ============ Bill Types ============
export type BillStatus = 'UNPAID' | 'PARTIAL_PAID' | 'PAID';

export interface BillItem {
  txn_id: string;
  txn_type: TransactionType;
  txn_amount: number;
  txn_date: string;
  description?: string;
}

export interface Bill {
  bill_id: string;
  account_id: string;
  bill_month: string;
  statement_date: string;
  due_date: string;
  opening_balance: number;
  total_purchase: number;
  total_repayment: number;
  total_installment: number;
  min_due: number;
  statement_balance: number;
  interest: number;
  late_fee: number;
  status: BillStatus;
  items: BillItem[];
  created_at: string;
}

export interface BillListResponse {
  bills: Bill[];
  total: number;
}

// ============ Installment Types ============
export type InstallmentPlanType = 'CONSUMPTION' | 'BILL';
export type InstallmentStatus = 
  | 'ACTIVE' 
  | 'COMPLETED' 
  | 'EARLY_SETTLED' 
  | 'DEFAULTED' 
  | 'CANCELLED';

export type ScheduleStatus = 'PENDING' | 'OVERDUE' | 'PAID' | 'CANCELLED';

export interface InstallmentSchedule {
  schedule_id: string;
  installment_id: string;
  period_no: number;
  due_date: string;
  principal_due: number;
  interest_due: number;
  total_due: number;
  principal_paid: number;
  interest_paid: number;
  total_paid: number;
  status: ScheduleStatus;
  paid_date?: string;
  created_at: string;
}

export interface Installment {
  installment_id: string;
  account_id: string;
  origin_txn_id: string;
  plan_type: InstallmentPlanType;
  tenure: number;
  principal_amount: number;
  interest_rate: number;
  total_interest: number;
  monthly_payment: number;
  remaining_principal: number;
  installments_paid: number;
  installments_remaining: number;
  first_due_date: string;
  status: InstallmentStatus;
  start_date: string;
  schedules: InstallmentSchedule[];
  created_at: string;
}

export interface CreateInstallmentRequest {
  origin_txn_id: string;
  plan_type: InstallmentPlanType;
  tenure: number;
  amount: number;
}

// ============ API Error ============
export interface ApiError {
  code: string;
  message: string;
  details?: Record<string, unknown>;
}

// ============ Credit Limit ============
export interface CreditLimitInfo {
  credit_limit: number;
  temp_limit: number;
  used_amount: number;
  frozen_amount: number;
  installment_used: number;
  available_amount: number;
  utilization_rate: number;
}

// ============ Dashboard Summary ============
export interface DashboardSummary {
  account: Account;
  current_bill?: Bill;
  recent_transactions: Transaction[];
  credit_limit_info: CreditLimitInfo;
}
