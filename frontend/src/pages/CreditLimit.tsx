import React, { useState, useEffect } from 'react';
import apiService from '../services/api';
import type { CreditLimitInfo, Installment } from '../types';
import './CreditLimit.css';

// Mock data
const mockCreditLimit: CreditLimitInfo = {
  credit_limit: 50000,
  temp_limit: 0,
  used_amount: 12500,
  frozen_amount: 500,
  installment_used: 3600,
  available_amount: 33900,
  utilization_rate: 25,
};

const mockInstallments: Installment[] = [
  {
    installment_id: 'inst-001',
    account_id: 'demo-account-001',
    origin_txn_id: 'txn-003',
    plan_type: 'CONSUMPTION',
    tenure: 12,
    principal_amount: 3600,
    interest_rate: 0.006,
    total_interest: 259.20,
    monthly_payment: 321.60,
    remaining_principal: 3000,
    installments_paid: 2,
    installments_remaining: 10,
    first_due_date: '2025-06-05',
    status: 'ACTIVE',
    start_date: '2025-05-05',
    schedules: [
      { schedule_id: 's1', installment_id: 'inst-001', period_no: 1, due_date: '2025-06-05', principal_due: 280, interest_due: 21.60, total_due: 301.60, principal_paid: 280, interest_paid: 21.60, total_paid: 301.60, status: 'PAID', paid_date: '2025-06-04', created_at: '2025-05-05T00:00:00Z' },
      { schedule_id: 's2', installment_id: 'inst-001', period_no: 2, due_date: '2025-07-05', principal_due: 280, interest_due: 19.20, total_due: 299.20, principal_paid: 280, interest_paid: 19.20, total_paid: 299.20, status: 'PAID', paid_date: '2025-07-03', created_at: '2025-06-05T00:00:00Z' },
      { schedule_id: 's3', installment_id: 'inst-001', period_no: 3, due_date: '2025-08-05', principal_due: 280, interest_due: 16.80, total_due: 296.80, principal_paid: 0, interest_paid: 0, total_paid: 0, status: 'PENDING', created_at: '2025-07-05T00:00:00Z' },
      { schedule_id: 's4', installment_id: 'inst-001', period_no: 4, due_date: '2025-09-05', principal_due: 280, interest_due: 14.40, total_due: 294.40, principal_paid: 0, interest_paid: 0, total_paid: 0, status: 'PENDING', created_at: '2025-08-05T00:00:00Z' },
      { schedule_id: 's5', installment_id: 'inst-001', period_no: 5, due_date: '2025-10-05', principal_due: 280, interest_due: 12.00, total_due: 292.00, principal_paid: 0, interest_paid: 0, total_paid: 0, status: 'PENDING', created_at: '2025-09-05T00:00:00Z' },
    ],
    created_at: '2025-05-05T00:00:00Z',
  },
  {
    installment_id: 'inst-002',
    account_id: 'demo-account-001',
    origin_txn_id: 'txn-008',
    plan_type: 'CONSUMPTION',
    tenure: 6,
    principal_amount: 1800,
    interest_rate: 0.006,
    total_interest: 64.80,
    monthly_payment: 310.80,
    remaining_principal: 1200,
    installments_paid: 2,
    installments_remaining: 4,
    first_due_date: '2025-05-05',
    status: 'ACTIVE',
    start_date: '2025-04-05',
    schedules: [],
    created_at: '2025-04-05T00:00:00Z',
  },
];

interface CreditLimitProps {
  accountId: string;
}

const CreditLimit: React.FC<CreditLimitProps> = ({ accountId }) => {
  const [creditInfo, setCreditInfo] = useState<CreditLimitInfo | null>(null);
  const [installments, setInstallments] = useState<Installment[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showAdjustModal, setShowAdjustModal] = useState(false);
  const [adjustAmount, setAdjustAmount] = useState('');
  const [adjustType, setAdjustType] = useState<'PERMANENT' | 'TEMP'>('PERMANENT');
  const [adjustReason, setAdjustReason] = useState('');

  useEffect(() => {
    loadCreditData();
  }, [accountId]);

  const loadCreditData = async () => {
    setLoading(true);
    setError(null);
    try {
      // In production, these would be real API calls
      // const [creditData, installmentsData] = await Promise.all([
      //   apiService.getCreditLimit(accountId),
      //   apiService.getInstallments(accountId),
      // ]);
      
      // Using mock data
      setCreditInfo(mockCreditLimit);
      setInstallments(mockInstallments);
    } catch (err) {
      console.error('Failed to load credit data:', err);
      setError('加载额度信息失败');
      setCreditInfo(mockCreditLimit);
      setInstallments(mockInstallments);
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

  const handleAdjustLimit = async () => {
    if (!adjustAmount || !adjustReason) return;
    
    try {
      // In production, this would be a real API call
      // await apiService.adjustLimit(accountId, {
      //   new_limit: Number(adjustAmount),
      //   type: adjustType,
      //   reason: adjustReason,
      // });
      
      alert('额度调整申请已提交');
      setShowAdjustModal(false);
      setAdjustAmount('');
      setAdjustReason('');
      loadCreditData();
    } catch (err) {
      console.error('Failed to adjust limit:', err);
      alert('额度调整失败，请稍后重试');
    }
  };

  const getStatusLabel = (status: string) => {
    const labels: Record<string, string> = {
      ACTIVE: '进行中',
      COMPLETED: '已完成',
      EARLY_SETTLED: '已提前结清',
      DEFAULTED: '逾期',
      CANCELLED: '已取消',
    };
    return labels[status] || status;
  };

  const getStatusBadgeClass = (status: string) => {
    const classes: Record<string, string> = {
      ACTIVE: 'badge-success',
      COMPLETED: 'badge-neutral',
      EARLY_SETTLED: 'badge-info',
      DEFAULTED: 'badge-danger',
      CANCELLED: 'badge-neutral',
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

  const normalUsed = creditInfo!.used_amount - creditInfo!.frozen_amount - creditInfo!.installment_used;

  return (
    <div className="credit-limit">
      {/* Credit Overview */}
      <div className="credit-overview-grid">
        <div className="card credit-main-card">
          <div className="card-body">
            <div className="credit-header">
              <h3>信用额度概览</h3>
              <button className="btn btn-primary btn-sm" onClick={() => setShowAdjustModal(true)}>
                申请调整额度
              </button>
            </div>
            
            <div className="credit-total">
              <div className="credit-total-label">总信用额度</div>
              <div className="credit-total-value">{formatCurrency(creditInfo!.credit_limit)}</div>
            </div>

            <div className="credit-breakdown">
              <div className="breakdown-item">
                <div className="breakdown-header">
                  <span className="breakdown-dot" style={{ backgroundColor: '#3b82f6' }}></span>
                  <span>普通消费</span>
                </div>
                <div className="breakdown-value">{formatCurrency(normalUsed)}</div>
                <div className="breakdown-percent">{((normalUsed / creditInfo!.credit_limit) * 100).toFixed(1)}%</div>
              </div>
              <div className="breakdown-item">
                <div className="breakdown-header">
                  <span className="breakdown-dot" style={{ backgroundColor: '#f59e0b' }}></span>
                  <span>预授权冻结</span>
                </div>
                <div className="breakdown-value">{formatCurrency(creditInfo!.frozen_amount)}</div>
                <div className="breakdown-percent">{((creditInfo!.frozen_amount / creditInfo!.credit_limit) * 100).toFixed(1)}%</div>
              </div>
              <div className="breakdown-item">
                <div className="breakdown-header">
                  <span className="breakdown-dot" style={{ backgroundColor: '#10b981' }}></span>
                  <span>分期占用</span>
                </div>
                <div className="breakdown-value">{formatCurrency(creditInfo!.installment_used)}</div>
                <div className="breakdown-percent">{((creditInfo!.installment_used / creditInfo!.credit_limit) * 100).toFixed(1)}%</div>
              </div>
            </div>

            <div className="credit-available">
              <div className="available-label">可用额度</div>
              <div className="available-value text-success">{formatCurrency(creditInfo!.available_amount)}</div>
              <div className="available-percent">
                {((creditInfo!.available_amount / creditInfo!.credit_limit) * 100).toFixed(1)}%
              </div>
            </div>

            <div className="utilization-bar-container">
              <div className="utilization-bar">
                <div 
                  className="utilization-segment normal"
                  style={{ width: `${(normalUsed / creditInfo!.credit_limit) * 100}%` }}
                ></div>
                <div 
                  className="utilization-segment frozen"
                  style={{ width: `${(creditInfo!.frozen_amount / creditInfo!.credit_limit) * 100}%` }}
                ></div>
                <div 
                  className="utilization-segment installment"
                  style={{ width: `${(creditInfo!.installment_used / creditInfo!.credit_limit) * 100}%` }}
                ></div>
              </div>
              <div className="utilization-legend">
                <span><span className="legend-dot" style={{ backgroundColor: '#3b82f6' }}></span>普通消费</span>
                <span><span className="legend-dot" style={{ backgroundColor: '#f59e0b' }}></span>预授权冻结</span>
                <span><span className="legend-dot" style={{ backgroundColor: '#10b981' }}></span>分期占用</span>
              </div>
            </div>
          </div>
        </div>

        {/* Quick Stats */}
        <div className="credit-stats">
          <div className="stat-card">
            <div className="stat-icon">💰</div>
            <div className="stat-content">
              <div className="stat-label">临时额度</div>
              <div className="stat-value">{formatCurrency(creditInfo!.temp_limit)}</div>
            </div>
          </div>
          <div className="stat-card">
            <div className="stat-icon">📊</div>
            <div className="stat-content">
              <div className="stat-label">使用率</div>
              <div className="stat-value">{creditInfo!.utilization_rate}%</div>
            </div>
          </div>
          <div className="stat-card">
            <div className="stat-icon">📑</div>
            <div className="stat-content">
              <div className="stat-label">分期数</div>
              <div className="stat-value">{installments.length}</div>
            </div>
          </div>
        </div>
      </div>

      {/* Installments */}
      <div className="card mt-lg">
        <div className="card-header">
          <h3>分期计划</h3>
        </div>
        <div className="card-body">
          {installments.length === 0 ? (
            <p className="text-muted text-center">暂无分期计划</p>
          ) : (
            <div className="installments-list">
              {installments.map((inst) => (
                <div key={inst.installment_id} className="installment-item">
                  <div className="installment-header">
                    <div className="installment-info">
                      <span className="installment-id font-mono">{inst.installment_id}</span>
                      <span className={`badge ${getStatusBadgeClass(inst.status)}`}>
                        {getStatusLabel(inst.status)}
                      </span>
                    </div>
                    <div className="installment-type">
                      {inst.plan_type === 'CONSUMPTION' ? '消费分期' : '账单分期'} · {inst.tenure}期
                    </div>
                  </div>
                  
                  <div className="installment-details">
                    <div className="detail">
                      <span className="detail-label">分期本金</span>
                      <span className="detail-value">{formatCurrency(inst.principal_amount)}</span>
                    </div>
                    <div className="detail">
                      <span className="detail-label">月利率</span>
                      <span className="detail-value">{(inst.interest_rate * 100).toFixed(2)}%</span>
                    </div>
                    <div className="detail">
                      <span className="detail-label">月供</span>
                      <span className="detail-value text-primary">{formatCurrency(inst.monthly_payment)}</span>
                    </div>
                    <div className="detail">
                      <span className="detail-label">剩余本金</span>
                      <span className="detail-value">{formatCurrency(inst.remaining_principal)}</span>
                    </div>
                    <div className="detail">
                      <span className="detail-label">已还期数</span>
                      <span className="detail-value">{inst.installments_paid}/{inst.tenure}</span>
                    </div>
                    <div className="detail">
                      <span className="detail-label">剩余期数</span>
                      <span className="detail-value">{inst.installments_remaining}</span>
                    </div>
                  </div>

                  {inst.schedules.length > 0 && (
                    <div className="installment-schedule">
                      <div className="schedule-header">还款计划</div>
                      <table className="table table-sm">
                        <thead>
                          <tr>
                            <th>期次</th>
                            <th>到期日</th>
                            <th>应还本金</th>
                            <th>应还利息</th>
                            <th>应还总额</th>
                            <th>状态</th>
                          </tr>
                        </thead>
                        <tbody>
                          {inst.schedules.slice(0, 5).map((schedule) => (
                            <tr key={schedule.schedule_id}>
                              <td>第 {schedule.period_no} 期</td>
                              <td>{schedule.due_date}</td>
                              <td>{formatCurrency(schedule.principal_due)}</td>
                              <td>{formatCurrency(schedule.interest_due)}</td>
                              <td>{formatCurrency(schedule.total_due)}</td>
                              <td>
                                <span className={`badge badge-${schedule.status === 'PAID' ? 'success' : schedule.status === 'OVERDUE' ? 'danger' : 'warning'}`}>
                                  {schedule.status === 'PAID' ? '已还' : schedule.status === 'OVERDUE' ? '逾期' : '待还'}
                                </span>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Adjust Modal */}
      {showAdjustModal && (
        <div className="modal-overlay" onClick={() => setShowAdjustModal(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3>申请调整额度</h3>
              <button className="modal-close" onClick={() => setShowAdjustModal(false)}>×</button>
            </div>
            <div className="modal-body">
              <div className="form-group">
                <label className="form-label">调整类型</label>
                <div className="radio-group">
                  <label className="radio-item">
                    <input 
                      type="radio" 
                      name="adjustType" 
                      value="PERMANENT"
                      checked={adjustType === 'PERMANENT'}
                      onChange={() => setAdjustType('PERMANENT')}
                    />
                    <span>永久额度</span>
                  </label>
                  <label className="radio-item">
                    <input 
                      type="radio" 
                      name="adjustType" 
                      value="TEMP"
                      checked={adjustType === 'TEMP'}
                      onChange={() => setAdjustType('TEMP')}
                    />
                    <span>临时额度</span>
                  </label>
                </div>
              </div>
              <div className="form-group">
                <label className="form-label">调整额度 (元)</label>
                <input 
                  type="number" 
                  className="form-input"
                  placeholder="请输入调整后的额度"
                  value={adjustAmount}
                  onChange={(e) => setAdjustAmount(e.target.value)}
                />
                <div className="form-hint">
                  当前额度: {formatCurrency(creditInfo!.credit_limit)}
                </div>
              </div>
              <div className="form-group">
                <label className="form-label">申请理由</label>
                <textarea 
                  className="form-textarea"
                  placeholder="请输入申请理由"
                  rows={3}
                  value={adjustReason}
                  onChange={(e) => setAdjustReason(e.target.value)}
                ></textarea>
              </div>
              <div className="modal-actions">
                <button className="btn btn-secondary" onClick={() => setShowAdjustModal(false)}>
                  取消
                </button>
                <button 
                  className="btn btn-primary"
                  onClick={handleAdjustLimit}
                  disabled={!adjustAmount || !adjustReason}
                >
                  提交申请
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default CreditLimit;
