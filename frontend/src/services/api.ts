import axios, { AxiosError, AxiosInstance } from 'axios';
import type {
  Account,
  AccountResponse,
  LimitAdjustmentRequest,
  StatusChangeRequest,
  Transaction,
  TransactionListResponse,
  Bill,
  BillListResponse,
  Installment,
  CreateInstallmentRequest,
  DashboardSummary,
  CreditLimitInfo,
} from '../types';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1';

class ApiService {
  private client: AxiosInstance;

  constructor() {
    this.client = axios.create({
      baseURL: API_BASE_URL,
      timeout: 30000,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    // Response interceptor for error handling
    this.client.interceptors.response.use(
      (response) => response,
      (error: AxiosError) => {
        console.error('API Error:', error.response?.data || error.message);
        return Promise.reject(error);
      }
    );
  }

  // ============ Account APIs ============
  
  /**
   * Get account details by account ID
   * GET /api/v1/accounts/{account_id}
   */
  async getAccount(accountId: string): Promise<Account> {
    const response = await this.client.get<Account>(`/accounts/${accountId}`);
    return response.data;
  }

  /**
   * Get account summary for dashboard
   * GET /api/v1/accounts/{account_id}/summary
   */
  async getAccountSummary(accountId: string): Promise<DashboardSummary> {
    const response = await this.client.get<DashboardSummary>(
      `/accounts/${accountId}/summary`
    );
    return response.data;
  }

  /**
   * Adjust credit limit
   * PUT /api/v1/accounts/{account_id}/limit
   */
  async adjustLimit(
    accountId: string,
    data: LimitAdjustmentRequest
  ): Promise<{ account_id: string; credit_limit: number }> {
    const response = await this.client.put<{ account_id: string; credit_limit: number }>(
      `/accounts/${accountId}/limit`,
      data
    );
    return response.data;
  }

  /**
   * Change account status
   * PUT /api/v1/accounts/{account_id}/status
   */
  async changeAccountStatus(
    accountId: string,
    data: StatusChangeRequest
  ): Promise<{ account_id: string; status: string; updated_at: string }> {
    const response = await this.client.put<{
      account_id: string;
      status: string;
      updated_at: string;
    }>(`/accounts/${accountId}/status`, data);
    return response.data;
  }

  // ============ Transaction APIs ============

  /**
   * Get transaction history
   * GET /api/v1/accounts/{account_id}/transactions
   */
  async getTransactions(
    accountId: string,
    params?: {
      page?: number;
      page_size?: number;
      start_date?: string;
      end_date?: string;
      txn_type?: string;
    }
  ): Promise<TransactionListResponse> {
    const response = await this.client.get<TransactionListResponse>(
      `/accounts/${accountId}/transactions`,
      { params }
    );
    return response.data;
  }

  /**
   * Get transaction details
   * GET /api/v1/transactions/{txn_id}
   */
  async getTransaction(txnId: string): Promise<Transaction> {
    const response = await this.client.get<Transaction>(`/transactions/${txnId}`);
    return response.data;
  }

  // ============ Bill APIs ============

  /**
   * Get bills for an account
   * GET /api/v1/accounts/{account_id}/bills
   */
  async getBills(
    accountId: string,
    params?: { month?: string }
  ): Promise<BillListResponse> {
    const response = await this.client.get<BillListResponse>(
      `/accounts/${accountId}/bills`,
      { params }
    );
    return response.data;
  }

  /**
   * Get specific bill details
   * GET /api/v1/accounts/{account_id}/bills/{bill_id}
   */
  async getBill(accountId: string, billId: string): Promise<Bill> {
    const response = await this.client.get<Bill>(
      `/accounts/${accountId}/bills/${billId}`
    );
    return response.data;
  }

  // ============ Credit Limit APIs ============

  /**
   * Get credit limit details
   * GET /api/v1/accounts/{account_id}/credit-limit
   */
  async getCreditLimit(accountId: string): Promise<CreditLimitInfo> {
    const response = await this.client.get<CreditLimitInfo>(
      `/accounts/${accountId}/credit-limit`
    );
    return response.data;
  }

  // ============ Installment APIs ============

  /**
   * Get installments for an account
   * GET /api/v1/accounts/{account_id}/installments
   */
  async getInstallments(accountId: string): Promise<Installment[]> {
    const response = await this.client.get<Installment[]>(
      `/accounts/${accountId}/installments`
    );
    return response.data;
  }

  /**
   * Get installment details
   * GET /api/v1/accounts/{account_id}/installments/{installment_id}
   */
  async getInstallment(
    accountId: string,
    installmentId: string
  ): Promise<Installment> {
    const response = await this.client.get<Installment>(
      `/accounts/${accountId}/installments/${installmentId}`
    );
    return response.data;
  }

  /**
   * Create new installment
   * POST /api/v1/accounts/{account_id}/installments
   */
  async createInstallment(
    accountId: string,
    data: CreateInstallmentRequest
  ): Promise<Installment> {
    const response = await this.client.post<Installment>(
      `/accounts/${accountId}/installments`,
      data
    );
    return response.data;
  }

  /**
   * Early settle installment
   * POST /api/v1/accounts/{account_id}/installments/{installment_id}/early-settle
   */
  async earlySettleInstallment(
    accountId: string,
    installmentId: string
  ): Promise<{
    installment_id: string;
    remaining_principal: number;
    early_settlement_fee: number;
    total_settlement_amount: number;
    status: string;
  }> {
    const response = await this.client.post<{
      installment_id: string;
      remaining_principal: number;
      early_settlement_fee: number;
      total_settlement_amount: number;
      status: string;
    }>(`/accounts/${accountId}/installments/${installmentId}/early-settle`);
    return response.data;
  }
}

// Export singleton instance
export const apiService = new ApiService();
export default apiService;
