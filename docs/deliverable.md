# 产品设计交付物

## 文档版本

- **文件**: `docs/credit-card-prd.md`
- **分支**: `feature/credit-card-prd`
- **最终版本**: v3.0 (commit: `b8ae5f7`)
- **状态**: ✅ 完成

## 完成内容

### 1. 四大核心功能域
- 账户管理 — 开卡、额度、状态变更
- 交易处理 — 授权、清算、退货、还款
- 额度控制 — 永久/临时额度调整
- 账单生成 — 月度账单、分期付款

### 2. 系统模块划分
- Gateway → 服务层 → 公共能力层 → 数据层

### 3. 核心数据模型（6个实体）
- **Account**: token_id, credit_limit, billing_day, due_days, installment_used
- **Transaction**: txn_type, txn_amount, settlement_date, auth_code
- **Bill**: billing_period, total_installment, due_amount
- **Authorization**: auth_code, auth_amount, consumed_amount, expire_time
- **Installment**: 完整分期计划实体（principal, tenure_months, monthly_rate）
- **InstallmentSchedule**: 分期还款计划（每期本金/利息/还款日）

### 4. RESTful API 设计
涵盖账户、交易、授权、还款、退货、额度调整、账单、分期六大模块，共 30+ 接口

### 5. 令牌化原则（PCI-DSS 合规）
- §5.0 独立章节说明：系统不接收/存储明文 PAN
- 外部系统通过 TSP 将 PAN 转为 token 后调用本系统
- Account.token_id 替换原 card_no 字段
- 响应字段 token 明确来源为 TSP 返回

### 6. 分期退货业务逻辑
- 全额退货：分期状态→CANCELLED + 释放 installment_used + 未到期 Schedule 标记 CANCELLED
- 部分退货：remaining_principal -= 退货金额 + 重新摊销
- 退货超出原始金额：拒绝，返回 REFUND_EXCEEDS_ORIGINAL
- 分期有逾期未还：拒绝退货，返回 REFUND_BLOCKED_BY_OVERDUE

### 7. 非功能性需求
- PCI-DSS §3/§7/§8/§11 安全合规
- TPS ≥ 3000
- 可用性 99.95%

### 8. 项目规划
四阶段 8 个月开发计划