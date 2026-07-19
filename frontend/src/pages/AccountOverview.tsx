import React, { useState, useEffect } from 'react';
import dayjs from 'dayjs';
import apiService from '../services/api';
import type { Account, Transaction, Bill } from '../types';
import './AccountOverview.css';

// Mock data for demo (since backend is not ready)
const mockAccount: Account = {
  account_id: 'demo-account-001',
  customer_id: 'customer-001',
  token: 'tkn_****_****1234',
  credit_limit: 50000,
  temp_limit: 0,
  used_amount: 12500,
  frozen_amount: 0,
  installment_used: 3600,
  available_amount: 33900,
  billing_day: 15,
  due_day: 5,
  status: 'ACTIVE',
  open_date: '2024-01-15',
  close_date: undefined,
  created_at: '2024-01-15T10:00:00Z',
  updated_at: '2025-07-19T09:00:00Z',
};

const mockRecentTransactions: Transaction[] = [
  {
    txn_id: 'txn-001',
    account_id: 'demo-account-001',
    txn_type: 'PURCHASE',
    txn_amount: 258.00,
    principal_amount: 258.00,
    fee_amount: 0,
    available_amount_before: 34158,
    available_amount_after: 33900,
    merchant_id: 'M001',
    merchant_name: '星巴克咖啡',
    merchant_category: '5812',
    terminal_id: 'T001',
    auth_code: 'A12345',
    reference_no: 'RN20250719001',
    txn_time: '2025-07-19T08:30:00Z',
    status: 'COMPLETED',
    created_at: '2025-07-19T08:30:00Z',
  },
  {
    txn_id: 'txn-002',
    account_id: 'demo-account-001',
    txn_type: 'PURCHASE',
    txn_amount: 899.00,
    principal_amount: 899.00,
    fee_amount: 0,
    available_amount_before: 35057,
    available_amount_after: 34158,
    merchant_id: 'M002',
    merchant_name: '京东商城',
    merchant_category: '5311',
    terminal_id: 'T002',
    auth_code: 'A12346',
    reference_no: 'RN20250718001',
    txn_time: '2025-07-18T14:20:00Z',
    status: 'COMPLETED',
    created_at: '2025-07-18T14:20:00Z',
  },
  {
    txn_id: 'txn-003',
    account_id: 'demo-account-001',
    txn_type: 'INSTALLMENT',
    txn_amount: 300.00,
    principal_amount: 280.00,
    fee_amount: 20.00,
    available_amount_before: 35357,
    available_amount_after: 35057,
    merchant_id: 'M003',
    merchant_name: '苹果官网',
    merchant_category: '5732',
    terminal_id: 'T003',
    auth_code: 'A12347',
    reference_no: 'RN20250717001',
    txn_time: '2025-07-17T10:00:00Z',
    status: 'COMPLETED',
    created_at: '2025-07-17T10:00:00Z',
  },
];

const mockCurrentBill: Bill = {
  bill_id: 'bill-202507',
  account_id: 'demo-account-001',
  bill_month: '202507',
  statement_date: '2025-07-15',
  due_date: '2025-08-05',
  opening_balance: 0,
  total_purchase: 8547.00,
  total_repayment: 0,
  total_installment: 1200.00,
  min_due: 1074.70,
  statement_balance: 9747.00,
  interest: 0,
  late_fee: 0,
  status: 'UNPAID',
  items: [],
  created_at: '2025-07-15T00:00:00Z',
};

interface AccountOverviewProps {
  accountId: string;
}

const AccountOverview: React.FC<AccountOverviewProps> = ({ accountId }) => {
  const [account, setAccount] = useState<Account | null>(null);
  const [recentTransactions, setRecentTransactions] = useState<Transaction[]>([]);
  const [currentBill, setCurrentBill] = useState<Bill | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadAccountData();
  }, [accountId]);

  const loadAccountData = async () => {
    setLoading(true);
    setError(null);
    try {
      // In production, these would be real API calls
      // const [accountData, transactionsData, billsData] = await Promise.all([
      //   apiService.getAccount(accountId),
      //   apiService.getTransactions(accountId, { page_size: 5 }),
      //   apiService.getBills(accountId),
      // ]);
      
      // Using mock data for demo
      setAccount(mockAccount);
      setRecentTransactions(mockRecentTransactions);
      setCurrentBill(mockCurrentBill);
    } catch (err) {
      console.error('Failed to load account data:', err);
      setError('加载账户数据失败，请稍后重试');
      // Fallback to mock data
      setAccount(mockAccount);
      setRecentTransactions(mockRecentTransactions);
      setCurrentBill(mockCurrentBill);
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('zh-CN', {
      style: 'currency',
      currency: 'CNY',
    }).format(amount);
  };

  const formatDate = (date: string) => {
    return dayjs(date).format('YYYY-MM-DD HH:mm');
  };

  const getTransactionTypeLabel = (type: string) => {
    const labels: Record<string, string> = {
      PURCHASE: '消费',
      WITHDRAWAL: '取现',
      REFUND: '退款',
      REPAYMENT: '还款',
      REVERSAL: '冲正',
      INSTALLMENT: '分期',
    };
    return labels[type] || type;
  };

  const getStatusBadgeClass = (status: string) => {
    const classes: Record<string, string> = {
      ACTIVE: 'badge-success',
      FROZEN: 'badge-warning',
      CLOSED: 'badge-neutral',
      COMPLETED: 'badge-success',
      PENDING: 'badge-warning',
      UNPAID: 'badge-danger',
      PARTIAL_PAID: 'badge-warning',
      PAID: 'badge-success',
    };
    return classes[status] || 'badge-neutral';
  };

  if (loading) {
    return (
      <div className="loading-container">
        <div className="spinner"></div>
        <p>加载中...</p>
      </div>
    );
  }

  if (error && !account) {
    return (
      <div className="error-container">
        <p className="text-danger">{error}</p>
        <button className="btn btn-primary" onClick={loadAccountData}>
          重试
        </button>
      </div>
    );
  }

  return (
    <div className="account-overview">
      {/* Account Summary Card */}
      <div className="overview-grid">
        <div className="card account-summary-card">
          <div className="card-body">
            <div className="account-header">
              <div className="account-info">
                <div className="account-token font-mono">
                  卡号: {account?.token}
                </div>
                <div className="account-status">
                  <span className={`badge ${getStatusBadgeClass(account?.status || '')}`}>
                    <span className={`status-dot status-dot-${account?.status?.toLowerCase()}`}></span>
                    {account?.status === 'ACTIVE' ? '正常' : account?.status === 'FROZEN' ? '冻结' : '已关闭'}
                  </span>
                </div>
              </div>
              <div className="account-stats">
                <div className="stat-item">
                  <div className="stat-label">信用额度</div>
                  <div className="stat-value text-primary">{formatCurrency(account?.credit_limit || 0)}</div>
                </div>
                <div className="stat-item">
                  <div className="stat-label">可用额度</div>
                  <div className="stat-value text-success">{formatCurrency(account?.available_amount || 0)}</div>
                </div>
                <div className="stat-item">
                  <div className="stat-label">已用额度</div>
                  <div className="stat-value text-warning">{formatCurrency(account?.used_amount || 0)}</div>
                </div>
              </div>
            </div>
            <div className="credit-utilization">
              <div className="utilization-header">
                <span>额度使用率</span>
                <span>{Math.round(((account?.used_amount || 0) / (account?.credit_limit || 1)) * 100)}%</span>
              </div>
              <div className="utilization-bar">
                <div 
                  className="utilization-fill" 
                  style={{ width: `${((account?.used_amount || 0) / (account?.credit_limit || 1)) * 100}%` }}
                ></div>
              </div>
            </div>
          </div>
        </div>

        {/* Current Bill Card */}
        {currentBill && (
          <div className="card bill-summary-card">
            <div className="card-header">
              <h3>本期账单</h3>
              <span className={`badge ${getStatusBadgeClass(currentBill.status)}`}>
                {currentBill.status === 'UNPAID' ? '未还款' : currentBill.status === 'PARTIAL_PAID' ? '部分还款' : '已还清'}
              </span>
            </div>
            <div className="card-body">
              <div className="bill-amount">
                <div className="bill-label">账单应还款</div>
                <div className="bill-value">{formatCurrency(currentBill.statement_balance)}</div>
              </div>
              <div className="bill-details">
                <div className="bill-detail-item">
                  <span>账单日</span>
                  <span>{currentBill.statement_date}</span>
                </div>
                <div className="bill-detail-item">
                  <span>到期还款日</span>
                  <span className="text-danger">{currentBill.due_date}</span>
                </div>
                <div className="bill-detail-item">
                  <span>最低还款额</span>
                  <span>{formatCurrency(currentBill.min_due)}</span>
                </div>
              </div>
              <div className="bill-actions mt-lg">
                <button className="btn btn-primary">立即还款</button>
                <button className="btn btn-secondary">查看详情</button>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Recent Transactions */}
      <div className="card mt-lg">
        <div className="card-header flex justify-between items-center">
          <h3>最近交易</h3>
          <a href="/transactions" className="btn btn-secondary btn-sm">查看全部</a>
        </div>
        <div className="card-body">
          {recentTransactions.length === 0 ? (
            <p className="text-muted text-center">暂无交易记录</p>
          ) : (
            <table className="table">
              <thead>
                <tr>
                  <th>时间</th>
                  <th>交易类型</th>
                  <th>商户</th>
                  <th>金额</th>
                  <th>状态</th>
                </tr>
              </thead>
              <tbody>
                {recentTransactions.map((txn) => (
                  <tr key={txn.txn_id}>
                    <td className="text-muted">{formatDate(txn.txn_time)}</td>
                    <td>
                      <span className="badge badge-info">
                        {getTransactionTypeLabel(txn.txn_type)}
                      </span>
                    </td>
                    <td>{txn.merchant_name}</td>
                    <td className={txn.txn_type === 'REFUND' || txn.txn_type === 'REPAYMENT' ? 'text-success' : ''}>
                      {txn.txn_type === 'REFUND' || txn.txn_type === 'REPAYMENT' ? '+' : '-'}
                      {formatCurrency(txn.txn_amount)}
                    </td>
                    <td>
                      <span className={`badge ${getStatusBadgeClass(txn.status)}`}>
                        {txn.status === 'COMPLETED' ? '已完成' : txn.status === 'PENDING' ? '处理中' : txn.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {/* Account Details */}
      <div className="card mt-lg">
        <div className="card-header">
          <h3>账户信息</h3>
        </div>
        <div className="card-body">
          <div className="info-grid">
            <div className="info-item">
              <div className="info-label">账户 ID</div>
              <div className="info-value font-mono">{account?.account_id}</div>
            </div>
            <div className="info-item">
              <div className="info-label">客户 ID</div>
              <div className="info-value font-mono">{account?.customer_id}</div>
            </div>
            <div className="info-item">
              <div className="info-label">开卡日期</div>
              <div className="info-value">{account?.open_date}</div>
            </div>
            <div className="info-item">
              <div className="info-label">账单日</div>
              <div className="info-value">每月 {account?.billing_day} 日</div>
            </div>
            <div className="info-item">
              <div className="info-label">到期还款日</div>
              <div className="info-value">每月 {account?.due_day} 日</div>
            </div>
            <div className="info-item">
              <div className="info-label">分期占用</div>
              <div className="info-value">{formatCurrency(account?.installment_used || 0)}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AccountOverview;
