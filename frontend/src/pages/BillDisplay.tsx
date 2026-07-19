import React, { useState, useEffect } from 'react';
import dayjs from 'dayjs';
import apiService from '../services/api';
import type { Bill, BillItem } from '../types';
import './BillDisplay.css';

// Mock data
const mockBills: Bill[] = [
  {
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
    items: [
      { txn_id: 'txn-001', txn_type: 'PURCHASE', txn_amount: 258.00, txn_date: '2025-07-19', description: '星巴克咖啡' },
      { txn_id: 'txn-002', txn_type: 'PURCHASE', txn_amount: 899.00, txn_date: '2025-07-18', description: '京东商城' },
      { txn_id: 'txn-003', txn_type: 'INSTALLMENT', txn_amount: 300.00, txn_date: '2025-07-17', description: '苹果官网 - 分期' },
      { txn_id: 'txn-007', txn_type: 'PURCHASE', txn_amount: 156.50, txn_date: '2025-07-13', description: '美团外卖' },
      { txn_id: 'txn-010', txn_type: 'PURCHASE', txn_amount: 688.00, txn_date: '2025-07-10', description: '拼多多' },
      { txn_id: 'txn-011', txn_type: 'PURCHASE', txn_amount: 328.00, txn_date: '2025-07-08', description: '滴滴出行' },
      { txn_id: 'txn-012', txn_type: 'PURCHASE', txn_amount: 125.50, txn_date: '2025-07-05', description: '盒马鲜生' },
    ],
    created_at: '2025-07-15T00:00:00Z',
  },
  {
    bill_id: 'bill-202506',
    account_id: 'demo-account-001',
    bill_month: '202506',
    statement_date: '2025-06-15',
    due_date: '2025-07-05',
    opening_balance: 0,
    total_purchase: 6230.00,
    total_repayment: 6230.00,
    total_installment: 900.00,
    min_due: 0,
    statement_balance: 0,
    interest: 0,
    late_fee: 0,
    status: 'PAID',
    items: [
      { txn_id: 'txn-020', txn_type: 'PURCHASE', txn_amount: 1200.00, txn_date: '2025-06-10', description: '天猫商城' },
      { txn_id: 'txn-021', txn_type: 'PURCHASE', txn_amount: 580.00, txn_date: '2025-06-08', description: '饿了么' },
      { txn_id: 'txn-022', txn_type: 'INSTALLMENT', txn_amount: 300.00, txn_date: '2025-06-05', description: '分期还款' },
    ],
    created_at: '2025-06-15T00:00:00Z',
  },
  {
    bill_id: 'bill-202505',
    account_id: 'demo-account-001',
    bill_month: '202505',
    statement_date: '2025-05-15',
    due_date: '2025-06-05',
    opening_balance: 0,
    total_purchase: 4500.00,
    total_repayment: 4500.00,
    total_installment: 600.00,
    min_due: 0,
    statement_balance: 0,
    interest: 0,
    late_fee: 0,
    status: 'PAID',
    items: [],
    created_at: '2025-05-15T00:00:00Z',
  },
];

interface BillDisplayProps {
  accountId: string;
}

const BillDisplay: React.FC<BillDisplayProps> = ({ accountId }) => {
  const [bills, setBills] = useState<Bill[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedBill, setSelectedBill] = useState<Bill | null>(null);
  const [selectedMonth, setSelectedMonth] = useState<string>('202507');

  useEffect(() => {
    loadBills();
  }, [accountId]);

  const loadBills = async () => {
    setLoading(true);
    setError(null);
    try {
      // In production, this would be a real API call
      // const data = await apiService.getBills(accountId);
      
      // Using mock data
      setBills(mockBills);
      const currentBill = mockBills.find(b => b.bill_month === selectedMonth);
      setSelectedBill(currentBill || mockBills[0]);
    } catch (err) {
      console.error('Failed to load bills:', err);
      setError('加载账单失败');
      setBills(mockBills);
      setSelectedBill(mockBills[0]);
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

  const formatMonth = (month: string) => {
    return dayjs(month + '01').format('YYYY年MM月');
  };

  const getStatusLabel = (status: string) => {
    const labels: Record<string, string> = {
      UNPAID: '未还款',
      PARTIAL_PAID: '部分还款',
      PAID: '已还清',
    };
    return labels[status] || status;
  };

  const getStatusBadgeClass = (status: string) => {
    const classes: Record<string, string> = {
      UNPAID: 'badge-danger',
      PARTIAL_PAID: 'badge-warning',
      PAID: 'badge-success',
    };
    return classes[status] || 'badge-neutral';
  };

  const getTransactionTypeLabel = (type: string) => {
    const labels: Record<string, string> = {
      PURCHASE: '消费',
      WITHDRAWAL: '取现',
      REFUND: '退款',
      REPAYMENT: '还款',
      INSTALLMENT: '分期',
    };
    return labels[type] || type;
  };

  const handleMonthChange = (month: string) => {
    setSelectedMonth(month);
    const bill = bills.find(b => b.bill_month === month);
    if (bill) {
      setSelectedBill(bill);
    }
  };

  const daysUntilDue = selectedBill ? dayjs(selectedBill.due_date).diff(dayjs(), 'day') : 0;
  const isOverdue = selectedBill && dayjs(selectedBill.due_date).isBefore(dayjs()) && selectedBill.status !== 'PAID';

  if (loading) {
    return (
      <div className="loading-container">
        <div className="spinner"></div>
        <p>加载中...</p>
      </div>
    );
  }

  const currentBill = bills.find(b => b.status === 'UNPAID' || b.status === 'PARTIAL_PAID');

  return (
    <div className="bill-display">
      {/* Bill Month Selector */}
      <div className="card mb-lg">
        <div className="card-body">
          <div className="month-selector">
            {bills.map((bill) => (
              <button
                key={bill.bill_id}
                className={`month-tab ${selectedBill?.bill_id === bill.bill_id ? 'active' : ''}`}
                onClick={() => handleMonthChange(bill.bill_month)}
              >
                <span className="month-label">{formatMonth(bill.bill_month)}</span>
                <span className={`badge badge-sm ${getStatusBadgeClass(bill.status)}`}>
                  {getStatusLabel(bill.status)}
                </span>
              </button>
            ))}
          </div>
        </div>
      </div>

      {selectedBill && (
        <>
          {/* Current Bill Summary */}
          <div className="bill-summary-grid">
            <div className="card bill-main-card">
              <div className="card-body">
                <div className="bill-header">
                  <div className="bill-period">
                    <h3>{formatMonth(selectedBill.bill_month)} 账单</h3>
                    <span className={`badge ${getStatusBadgeClass(selectedBill.status)}`}>
                      {getStatusLabel(selectedBill.status)}
                    </span>
                  </div>
                  {isOverdue && (
                    <div className="overdue-warning">
                      已逾期 {Math.abs(daysUntilDue)} 天
                    </div>
                  )}
                  {!isOverdue && selectedBill.status !== 'PAID' && (
                    <div className="due-countdown">
                      距到期还款日还有 {daysUntilDue} 天
                    </div>
                  )}
                </div>

                <div className="bill-balance">
                  <div className="balance-label">账单应还款</div>
                  <div className={`balance-amount ${selectedBill.status === 'PAID' ? 'text-success' : 'text-danger'}`}>
                    {formatCurrency(selectedBill.statement_balance)}
                  </div>
                </div>

                <div className="bill-dates">
                  <div className="date-item">
                    <span className="date-label">账单日</span>
                    <span className="date-value">{selectedBill.statement_date}</span>
                  </div>
                  <div className="date-item">
                    <span className="date-label">到期还款日</span>
                    <span className={`date-value ${isOverdue ? 'text-danger' : ''}`}>
                      {selectedBill.due_date}
                    </span>
                  </div>
                </div>

                {selectedBill.status !== 'PAID' && (
                  <div className="bill-actions">
                    <button className="btn btn-primary btn-lg">
                      立即还款
                    </button>
                    <button className="btn btn-secondary">
                      分期还款
                    </button>
                  </div>
                )}
              </div>
            </div>

            {/* Bill Stats */}
            <div className="bill-stats">
              <div className="stat-card">
                <div className="stat-icon">🛒</div>
                <div className="stat-content">
                  <div className="stat-label">消费合计</div>
                  <div className="stat-value">{formatCurrency(selectedBill.total_purchase)}</div>
                </div>
              </div>
              <div className="stat-card">
                <div className="stat-icon">💸</div>
                <div className="stat-content">
                  <div className="stat-label">已还款</div>
                  <div className="stat-value text-success">{formatCurrency(selectedBill.total_repayment)}</div>
                </div>
              </div>
              <div className="stat-card">
                <div className="stat-icon">📑</div>
                <div className="stat-content">
                  <div className="stat-label">分期摊销</div>
                  <div className="stat-value">{formatCurrency(selectedBill.total_installment)}</div>
                </div>
              </div>
              <div className="stat-card">
                <div className="stat-icon">💵</div>
                <div className="stat-content">
                  <div className="stat-label">最低还款额</div>
                  <div className="stat-value">{formatCurrency(selectedBill.min_due)}</div>
                </div>
              </div>
            </div>
          </div>

          {/* Bill Details */}
          <div className="card mt-lg">
            <div className="card-header">
              <h3>账单明细</h3>
            </div>
            <div className="card-body">
              {selectedBill.items.length === 0 ? (
                <p className="text-muted text-center">本期无交易记录</p>
              ) : (
                <table className="table">
                  <thead>
                    <tr>
                      <th>交易日期</th>
                      <th>交易类型</th>
                      <th>描述</th>
                      <th className="text-right">金额</th>
                    </tr>
                  </thead>
                  <tbody>
                    {selectedBill.items.map((item, index) => (
                      <tr key={`${item.txn_id}-${index}`}>
                        <td className="text-muted">{item.txn_date}</td>
                        <td>
                          <span className="badge badge-info">
                            {getTransactionTypeLabel(item.txn_type)}
                          </span>
                        </td>
                        <td>{item.description || '-'}</td>
                        <td className="text-right">
                          {item.txn_type === 'REFUND' || item.txn_type === 'REPAYMENT' ? (
                            <span className="text-success">+{formatCurrency(item.txn_amount)}</span>
                          ) : (
                            formatCurrency(item.txn_amount)
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                  <tfoot>
                    <tr>
                      <td colSpan={3} className="text-right"><strong>本期消费合计</strong></td>
                      <td className="text-right"><strong>{formatCurrency(selectedBill.total_purchase)}</strong></td>
                    </tr>
                  </tfoot>
                </table>
              )}
            </div>
          </div>

          {/* Bill Calculation */}
          <div className="card mt-lg">
            <div className="card-header">
              <h3>账单计算</h3>
            </div>
            <div className="card-body">
              <div className="calculation-list">
                <div className="calc-item">
                  <span className="calc-label">上期余额</span>
                  <span className="calc-value">{formatCurrency(selectedBill.opening_balance)}</span>
                </div>
                <div className="calc-item">
                  <span className="calc-label">本期消费</span>
                  <span className="calc-value">+ {formatCurrency(selectedBill.total_purchase)}</span>
                </div>
                <div className="calc-item">
                  <span className="calc-label">分期摊销</span>
                  <span className="calc-value">+ {formatCurrency(selectedBill.total_installment)}</span>
                </div>
                <div className="calc-item">
                  <span className="calc-label">本期还款</span>
                  <span className="calc-value text-success">- {formatCurrency(selectedBill.total_repayment)}</span>
                </div>
                {selectedBill.interest > 0 && (
                  <div className="calc-item">
                    <span className="calc-label">利息</span>
                    <span className="calc-value">+ {formatCurrency(selectedBill.interest)}</span>
                  </div>
                )}
                {selectedBill.late_fee > 0 && (
                  <div className="calc-item">
                    <span className="calc-label">滞纳金</span>
                    <span className="calc-value">+ {formatCurrency(selectedBill.late_fee)}</span>
                  </div>
                )}
                <div className="calc-item calc-total">
                  <span className="calc-label">账单应还款</span>
                  <span className="calc-value">{formatCurrency(selectedBill.statement_balance)}</span>
                </div>
              </div>
            </div>
          </div>

          {/* Payment Guide */}
          {selectedBill.status !== 'PAID' && (
            <div className="card mt-lg">
              <div className="card-header">
                <h3>还款指南</h3>
              </div>
              <div className="card-body">
                <div className="payment-methods">
                  <div className="method-item">
                    <div className="method-icon">🏦</div>
                    <div className="method-content">
                      <div className="method-title">自动还款</div>
                      <div className="method-desc">绑定借记卡自动扣款，永不逾期</div>
                    </div>
                    <button className="btn btn-secondary btn-sm">设置</button>
                  </div>
                  <div className="method-item">
                    <div className="method-icon">📱</div>
                    <div className="method-content">
                      <div className="method-title">手机银行</div>
                      <div className="method-desc">使用手机银行 APP 快速还款</div>
                    </div>
                    <button className="btn btn-secondary btn-sm">立即还款</button>
                  </div>
                  <div className="method-item">
                    <div className="method-icon">💳</div>
                    <div className="method-content">
                      <div className="method-title">转账还款</div>
                      <div className="method-desc">汇款至信用卡专用还款账户</div>
                    </div>
                    <button className="btn btn-secondary btn-sm">查看账号</button>
                  </div>
                </div>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
};

export default BillDisplay;
