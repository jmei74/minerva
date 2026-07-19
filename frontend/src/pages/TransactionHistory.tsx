import React, { useState, useEffect } from 'react';
import dayjs from 'dayjs';
import apiService from '../services/api';
import type { Transaction, TransactionType } from '../types';
import './TransactionHistory.css';

// Mock data for demo
const mockTransactions: Transaction[] = [
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
  {
    txn_id: 'txn-004',
    account_id: 'demo-account-001',
    txn_type: 'REFUND',
    txn_amount: 150.00,
    principal_amount: 150.00,
    fee_amount: 0,
    available_amount_before: 35507,
    available_amount_after: 35357,
    merchant_id: 'M004',
    merchant_name: '天猫商城',
    merchant_category: '5311',
    terminal_id: 'T004',
    auth_code: 'A12348',
    reference_no: 'RN20250716001',
    txn_time: '2025-07-16T16:00:00Z',
    status: 'COMPLETED',
    created_at: '2025-07-16T16:00:00Z',
  },
  {
    txn_id: 'txn-005',
    account_id: 'demo-account-001',
    txn_type: 'WITHDRAWAL',
    txn_amount: 2000.00,
    principal_amount: 1950.00,
    fee_amount: 50.00,
    available_amount_before: 37507,
    available_amount_after: 35507,
    merchant_id: 'ATM001',
    merchant_name: '招商银行ATM',
    merchant_category: '6011',
    terminal_id: 'ATM001',
    auth_code: 'A12349',
    reference_no: 'RN20250715001',
    txn_time: '2025-07-15T12:00:00Z',
    status: 'COMPLETED',
    created_at: '2025-07-15T12:00:00Z',
  },
  {
    txn_id: 'txn-006',
    account_id: 'demo-account-001',
    txn_type: 'REPAYMENT',
    txn_amount: 5000.00,
    principal_amount: 5000.00,
    fee_amount: 0,
    available_amount_before: 32507,
    available_amount_after: 37507,
    merchant_id: 'SYS',
    merchant_name: '主动还款',
    merchant_category: '0000',
    terminal_id: 'SYS',
    auth_code: 'R12345',
    reference_no: 'RP20250714001',
    txn_time: '2025-07-14T09:00:00Z',
    status: 'COMPLETED',
    created_at: '2025-07-14T09:00:00Z',
  },
  {
    txn_id: 'txn-007',
    account_id: 'demo-account-001',
    txn_type: 'PURCHASE',
    txn_amount: 156.50,
    principal_amount: 156.50,
    fee_amount: 0,
    available_amount_before: 32663.50,
    available_amount_after: 32507,
    merchant_id: 'M005',
    merchant_name: '美团外卖',
    merchant_category: '5812',
    terminal_id: 'T005',
    auth_code: 'A12350',
    reference_no: 'RN20250713001',
    txn_time: '2025-07-13T19:30:00Z',
    status: 'COMPLETED',
    created_at: '2025-07-13T19:30:00Z',
  },
];

interface TransactionHistoryProps {
  accountId: string;
}

const TransactionHistory: React.FC<TransactionHistoryProps> = ({ accountId }) => {
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(1);
  const [pageSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [selectedType, setSelectedType] = useState<string>('ALL');
  const [dateRange, setDateRange] = useState({ start: '', end: '' });
  const [selectedTxn, setSelectedTxn] = useState<Transaction | null>(null);

  useEffect(() => {
    loadTransactions();
  }, [accountId, page, selectedType, dateRange]);

  const loadTransactions = async () => {
    setLoading(true);
    setError(null);
    try {
      // In production, this would be a real API call
      // const data = await apiService.getTransactions(accountId, {
      //   page,
      //   page_size: pageSize,
      //   start_date: dateRange.start || undefined,
      //   end_date: dateRange.end || undefined,
      //   txn_type: selectedType !== 'ALL' ? selectedType : undefined,
      // });
      
      // Using mock data
      let filtered = [...mockTransactions];
      if (selectedType !== 'ALL') {
        filtered = filtered.filter(t => t.txn_type === selectedType);
      }
      if (dateRange.start) {
        filtered = filtered.filter(t => dayjs(t.txn_time).isAfter(dayjs(dateRange.start)));
      }
      if (dateRange.end) {
        filtered = filtered.filter(t => dayjs(t.txn_time).isBefore(dayjs(dateRange.end).add(1, 'day')));
      }
      
      setTransactions(filtered);
      setTotal(filtered.length);
    } catch (err) {
      console.error('Failed to load transactions:', err);
      setError('加载交易记录失败');
      setTransactions(mockTransactions);
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

  const getTransactionTypeLabel = (type: TransactionType) => {
    const labels: Record<TransactionType, string> = {
      PURCHASE: '消费',
      WITHDRAWAL: '取现',
      REFUND: '退款',
      REPAYMENT: '还款',
      REVERSAL: '冲正',
      INSTALLMENT: '分期',
    };
    return labels[type];
  };

  const getTransactionTypeBadgeClass = (type: TransactionType) => {
    const classes: Record<TransactionType, string> = {
      PURCHASE: 'badge-danger',
      WITHDRAWAL: 'badge-warning',
      REFUND: 'badge-success',
      REPAYMENT: 'badge-info',
      REVERSAL: 'badge-neutral',
      INSTALLMENT: 'badge-primary',
    };
    return classes[type] || 'badge-neutral';
  };

  const getStatusLabel = (status: string) => {
    const labels: Record<string, string> = {
      COMPLETED: '已完成',
      PENDING: '处理中',
      REVERSED: '已冲正',
      REFUNDED: '已退款',
    };
    return labels[status] || status;
  };

  const totalPages = Math.ceil(total / pageSize);

  const transactionTypes = [
    { value: 'ALL', label: '全部' },
    { value: 'PURCHASE', label: '消费' },
    { value: 'WITHDRAWAL', label: '取现' },
    { value: 'REFUND', label: '退款' },
    { value: 'REPAYMENT', label: '还款' },
    { value: 'INSTALLMENT', label: '分期' },
  ];

  return (
    <div className="transaction-history">
      {/* Filters */}
      <div className="card mb-lg">
        <div className="card-body">
          <div className="filters">
            <div className="filter-group">
              <label className="form-label">交易类型</label>
              <select 
                className="form-select"
                value={selectedType}
                onChange={(e) => setSelectedType(e.target.value)}
              >
                {transactionTypes.map(type => (
                  <option key={type.value} value={type.value}>{type.label}</option>
                ))}
              </select>
            </div>
            <div className="filter-group">
              <label className="form-label">开始日期</label>
              <input 
                type="date" 
                className="form-input"
                value={dateRange.start}
                onChange={(e) => setDateRange(prev => ({ ...prev, start: e.target.value }))}
              />
            </div>
            <div className="filter-group">
              <label className="form-label">结束日期</label>
              <input 
                type="date" 
                className="form-input"
                value={dateRange.end}
                onChange={(e) => setDateRange(prev => ({ ...prev, end: e.target.value }))}
              />
            </div>
            <div className="filter-actions">
              <button className="btn btn-secondary" onClick={() => {
                setSelectedType('ALL');
                setDateRange({ start: '', end: '' });
              }}>
                重置
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Transaction List */}
      <div className="card">
        <div className="card-header flex justify-between items-center">
          <h3>交易记录</h3>
          <span className="text-muted">共 {total} 条记录</span>
        </div>
        <div className="card-body">
          {loading ? (
            <div className="loading-container">
              <div className="spinner"></div>
              <p>加载中...</p>
            </div>
          ) : error ? (
            <div className="error-container">
              <p className="text-danger">{error}</p>
            </div>
          ) : transactions.length === 0 ? (
            <div className="empty-state">
              <p className="text-muted">暂无交易记录</p>
            </div>
          ) : (
            <>
              <table className="table">
                <thead>
                  <tr>
                    <th>交易时间</th>
                    <th>交易类型</th>
                    <th>商户名称</th>
                    <th>交易金额</th>
                    <th>可用额度</th>
                    <th>状态</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  {transactions.map((txn) => (
                    <tr key={txn.txn_id}>
                      <td className="text-muted">{formatDate(txn.txn_time)}</td>
                      <td>
                        <span className={`badge ${getTransactionTypeBadgeClass(txn.txn_type)}`}>
                          {getTransactionTypeLabel(txn.txn_type)}
                        </span>
                      </td>
                      <td>{txn.merchant_name}</td>
                      <td className={['REFUND', 'REPAYMENT'].includes(txn.txn_type) ? 'text-success' : ''}>
                        {['REFUND', 'REPAYMENT'].includes(txn.txn_type) ? '+' : '-'}
                        {formatCurrency(txn.txn_amount)}
                      </td>
                      <td className="text-muted">{formatCurrency(txn.available_amount_after)}</td>
                      <td>
                        <span className={`badge badge-${txn.status === 'COMPLETED' ? 'success' : 'warning'}`}>
                          {getStatusLabel(txn.status)}
                        </span>
                      </td>
                      <td>
                        <button 
                          className="btn btn-secondary btn-sm"
                          onClick={() => setSelectedTxn(txn)}
                        >
                          详情
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>

              {/* Pagination */}
              {totalPages > 1 && (
                <div className="pagination">
                  <button 
                    className="btn btn-secondary btn-sm"
                    disabled={page === 1}
                    onClick={() => setPage(p => p - 1)}
                  >
                    上一页
                  </button>
                  <span className="pagination-info">
                    第 {page} / {totalPages} 页
                  </span>
                  <button 
                    className="btn btn-secondary btn-sm"
                    disabled={page === totalPages}
                    onClick={() => setPage(p => p + 1)}
                  >
                    下一页
                  </button>
                </div>
              )}
            </>
          )}
        </div>
      </div>

      {/* Transaction Detail Modal */}
      {selectedTxn && (
        <div className="modal-overlay" onClick={() => setSelectedTxn(null)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3>交易详情</h3>
              <button className="modal-close" onClick={() => setSelectedTxn(null)}>×</button>
            </div>
            <div className="modal-body">
              <div className="detail-grid">
                <div className="detail-item">
                  <span className="detail-label">交易 ID</span>
                  <span className="detail-value font-mono">{selectedTxn.txn_id}</span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">交易类型</span>
                  <span className="detail-value">
                    <span className={`badge ${getTransactionTypeBadgeClass(selectedTxn.txn_type)}`}>
                      {getTransactionTypeLabel(selectedTxn.txn_type)}
                    </span>
                  </span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">交易金额</span>
                  <span className="detail-value text-lg">
                    {['REFUND', 'REPAYMENT'].includes(selectedTxn.txn_type) ? '+' : '-'}
                    {formatCurrency(selectedTxn.txn_amount)}
                  </span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">交易时间</span>
                  <span className="detail-value">{formatDate(selectedTxn.txn_time)}</span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">商户名称</span>
                  <span className="detail-value">{selectedTxn.merchant_name}</span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">商户 ID</span>
                  <span className="detail-value font-mono">{selectedTxn.merchant_id}</span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">终端 ID</span>
                  <span className="detail-value font-mono">{selectedTxn.terminal_id}</span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">授权码</span>
                  <span className="detail-value font-mono">{selectedTxn.auth_code}</span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">参考号</span>
                  <span className="detail-value font-mono">{selectedTxn.reference_no}</span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">状态</span>
                  <span className="detail-value">
                    <span className={`badge badge-${selectedTxn.status === 'COMPLETED' ? 'success' : 'warning'}`}>
                      {getStatusLabel(selectedTxn.status)}
                    </span>
                  </span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">交易前可用额度</span>
                  <span className="detail-value">{formatCurrency(selectedTxn.available_amount_before)}</span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">交易后可用额度</span>
                  <span className="detail-value">{formatCurrency(selectedTxn.available_amount_after)}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default TransactionHistory;
