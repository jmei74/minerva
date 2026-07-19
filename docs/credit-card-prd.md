# 信用卡核心系统 PRD 与技术方案

**版本**: v3.0
**作者**: 小晶
**日期**: 2025-05-31（v2.0 修订：2026-07-19；v3.0 修订：2026-07-19）
**状态**: 初稿

---

## 1. 背景与目标

本系统面向银行或支付机构，提供信用卡全生命周期管理能力，覆盖账户管理、交易处理、额度控制和账单生成四大核心域。

**核心目标**：
- 支持高并发交易（目标 TPS ≥ 3000）
- 账务数据零差错，事后对账偏差率 < 0.001%
- 系统可用性 99.95%（年度停机 < 4.38 小时）
- 满足 PCI-DSS Level 1 合规要求

---

## 2. 功能范围

### 2.1 账户管理（Account Management）

| 功能 | 描述 |
|------|------|
| 开卡 | 审批通过后创建账户，初始化额度、账单周期 |
| 销户 | 结清全部欠款后关闭账户，标记为销户状态 |
| 账户信息查询 | 查询账户基本信息、当前额度、可用额度 |
| 额度调整 | 支持永久额度和临时额度调整，需审批流 |
| 账户状态变更 | 激活 / 封卡 / 冻结 / 注销状态机 |

### 2.2 交易处理（Transaction Processing）

| 功能 | 描述 |
|------|------|
| 消费 | 商户刷卡/插卡/挥卡，支持主扫、被扫 |
| 取现 | 预借现金，收取手续费和利息 |
| 还款 | 全额还款、部分还款、溢缴款还款 |
| 分期 | 消费分期（3/6/12/24 期）、账单分期 |
| 冲正/撤销 | 当日交易撤销，隔日冲正 |
| 退货 | 退货交易，退款至账户 |

### 2.3 额度控制（Credit Control）

| 功能 | 描述 |
|------|------|
| 可用额度计算 | 实时计算：可用额度 = 固定额度 - 已用额度 - 冻结金额 |
| 预授权 | 酒店/租车等场景的额度预占 |
| 授权完成 | 预授权转实际消费 |
| 超额控制 | 支持设置超额比例（如 10%），超限交易拒绝 |
| 分期占用 | 分期本金在还款前持续占用额度 |
| 分期退货 | 分期交易退货：全额退货取消分期计划并释放额度；部分退货扣减剩余本金，调整还款计划 |

### 2.4 账单生成（Billing）

| 功能 | 描述 |
|------|------|
| 账单生成 | 每月固定账单日生成账单，包含上周期消费 |
| 账单查询 | 查询历史账单、账单明细 |
| 最低还款额计算 | 最低还款额 = 10% 本金 + 100% 费用 + 100% 利息 |
| 滞纳金计算 | 未还最低额时收取，比例遵循监管规定 |
| 利息计算 | 未全额还款时，全额从消费日起计息 |

---

## 3. 系统模块划分

```
┌─────────────────────────────────────────────────────────────┐
│                      接入层（Gateway）                        │
│   REST API  │  消息队列（Kafka）  │  批量作业（Scheduler）    │
└──────────┬────────────────┬──────────────────┬──────────────┘
           │                │                  │
┌──────────▼────┐  ┌───────▼──────┐  ┌────────▼────────────┐
│ 账户服务       │  │ 交易服务      │  │ 账单服务             │
│ Account Svc   │  │ Transaction   │  │ Billing Svc         │
│               │  │ Service       │  │                    │
│ - 开卡/销户   │  │ - 消费/还款   │  │ - 账单生成          │
│ - 额度管理   │  │ - 授权处理   │  │ - 利息/滞纳金计算   │
│ - 账户状态   │  │ - 冲正/退货   │  │ - 最低还款额计算    │
│               │  │ - 分期管理   │  │                    │
└─────────┬────┘  └───────┬───────┘  └────────┬────────────┘
          │               │                   │
┌─────────▼───────────────▼───────────────────▼──────────────┐
│                    公共能力层                                 │
│  额度引擎（Credit Engine）│ 定价引擎（Pricing）│ 事件总线    │
└─────────────────────────────────────────────────────────────┘
          │               │                   │
┌─────────▼───────────────▼───────────────────▼──────────────┐
│                    数据层                                     │
│     PostgreSQL（账务核心）  │  Redis（缓存/额度快照）        │
│     MongoDB（日志/流水）    │  Kafka（事件驱动）            │
└─────────────────────────────────────────────────────────────┘
```

---

## 4. 数据模型

### 4.1 核心实体

#### Account（账户）

| 字段 | 类型 | 说明 |
|------|------|------|
| account_id | UUID | 主键 |
| customer_id | UUID | 客户ID（关联CRM） |
| token_id | VARCHAR(50) | 卡号 token（由卡组织或 Tokenization Service 生成，替代明文 PAN） |
| credit_limit | DECIMAL(15,2) | 固定额度 |
| temp_limit | DECIMAL(15,2) | 临时额度 |
| used_amount | DECIMAL(15,2) | 已使用金额 |
| frozen_amount | DECIMAL(15,2) | 冻结金额（预授权） |
| installment_used | DECIMAL(15,2) | 分期占用金额（本金未还部分） |
| available_amount | DECIMAL(15,2) | 可用金额（计算字段，实时刷新） |
| billing_day | INT | 账单日（1-31） |
| due_day | INT | 到期还款日 |
| status | ENUM | ACTIVE/FROZEN/CLOSED |
| open_date | DATE | 开卡日期 |
| close_date | DATE | 销户日期（nullable） |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |

> **PCI-DSS 说明**：账户表中不存储明文卡号（PAN）。卡号 token 由卡组织（Visa Token Service / Mastercard DSP）或内部 Tokenization Service 生成，系统仅持有 token。PAN 与 token 的映射关系由 Token Vault 保管，PCI-DSS 范围外存储。

#### Transaction（交易流水）

| 字段 | 类型 | 说明 |
|------|------|------|
| txn_id | UUID | 主键 |
| account_id | UUID | 账户ID |
| installment_id | UUID | 分期ID（nullable，普通消费为空） |
| txn_type | ENUM | PURCHASE/WITHDRAWAL/REFUND/REPAYMENT/REVERSAL/INSTALLMENT |
| txn_amount | DECIMAL(15,2) | 交易原始金额 |
| principal_amount | DECIMAL(15,2) | 本金（去除手续费后的实收金额） |
| fee_amount | DECIMAL(15,2) | 手续费（取现/分期手续费） |
| available_amount_before | DECIMAL(15,2) | 交易前可用额度 |
| available_amount_after | DECIMAL(15,2) | 交易后可用额度 |
| merchant_id | VARCHAR(50) | 商户ID |
| merchant_name | VARCHAR(200) | 商户名称 |
| merchant_category | VARCHAR(10) | MCC 商户类别码 |
| terminal_id | VARCHAR(50) | 终端ID |
| auth_code | VARCHAR(20) | 授权码（用于联机交易） |
| reference_no | VARCHAR(50) | 参考号 |
| txn_time | TIMESTAMP | 交易时间 |
| settlement_date | DATE | 清算日期（可延后） |
| status | ENUM | PENDING/COMPLETED/REVERSED/REFUNDED |
| created_at | TIMESTAMP | 创建时间 |

#### Bill（账单）

| 字段 | 类型 | 说明 |
|------|------|------|
| bill_id | UUID | 主键 |
| account_id | UUID | 账户ID |
| bill_month | VARCHAR(6) | 账单月份（YYYYMM） |
| statement_date | DATE | 账单日 |
| due_date | DATE | 到期还款日 |
| opening_balance | DECIMAL(15,2) | 上期余额 |
| total_purchase | DECIMAL(15,2) | 本期消费合计（含分期本金摊销） |
| total_repayment | DECIMAL(15,2) | 本期还款合计 |
| total_installment | DECIMAL(15,2) | 本期分期摊销本金 |
| min_due | DECIMAL(15,2) | 最低还款额 |
| statement_balance | DECIMAL(15,2) | 账单应还款额 |
| interest | DECIMAL(15,2) | 利息 |
| late_fee | DECIMAL(15,2) | 滞纳金 |
| status | ENUM | UNPAID/PARTIAL_PAID/PAID |
| created_at | TIMESTAMP | 创建时间 |

#### Authorization（授权）

| 字段 | 类型 | 说明 |
|------|------|------|
| auth_id | UUID | 主键 |
| account_id | UUID | 账户ID（从 token 解析得到） |
| auth_code | VARCHAR(20) | 授权码 |
| auth_amount | DECIMAL(15,2) | 授权金额 |
| auth_type | ENUM | PRE_AUTH/COMPLETION/CANCEL |
| merchant_id | VARCHAR(50) | 商户ID |
| status | ENUM | PENDING/COMPLETED/EXPIRED/CANCELLED |
| expire_time | TIMESTAMP | 过期时间（通常30天） |
| created_at | TIMESTAMP | 创建时间 |

#### Installment（分期计划）

| 字段 | 类型 | 说明 |
|------|------|------|
| installment_id | UUID | 主键 |
| account_id | UUID | 账户ID |
| origin_txn_id | UUID | 触发分期的原始消费交易ID |
| plan_type | ENUM | CONSUMPTION/BILL（消费分期/账单分期） |
| tenure | INT | 期数（3/6/12/24） |
| principal_amount | DECIMAL(15,2) | 分期本金 |
| interest_rate | DECIMAL(6,4) | 月利率（如 0.0060 = 月息 0.6%） |
| total_interest | DECIMAL(15,2) | 总利息 |
| monthly_payment | DECIMAL(15,2) | 每期还款额（含本金+利息） |
| remaining_principal | DECIMAL(15,2) | 剩余未还本金（随还款递减） |
| installments_paid | INT | 已还期数 |
| installments_remaining | INT | 剩余期数 |
| first_due_date | DATE | 首次到期还款日 |
| status | ENUM | ACTIVE/COMPLETED/EARLY_SETTLED/DEFAULTED/CANCELLED |
| start_date | DATE | 分期开始日期 |
| created_at | TIMESTAMP | 创建时间 |

#### InstallmentSchedule（分期还款计划）

| 字段 | 类型 | 说明 |
|------|------|------|
| schedule_id | UUID | 主键 |
| installment_id | UUID | 关联分期计划ID |
| period_no | INT | 期次编号（1-based） |
| due_date | DATE | 应还日期 |
| principal_due | DECIMAL(15,2) | 当期应还本金 |
| interest_due | DECIMAL(15,2) | 当期应还利息 |
| total_due | DECIMAL(15,2) | 当期应还总额 |
| principal_paid | DECIMAL(15,2) | 当期已还本金 |
| interest_paid | DECIMAL(15,2) | 当期已还利息 |
| total_paid | DECIMAL(15,2) | 当期已还总额 |
| status | ENUM | PENDING/OVERDUE/PAID |
| paid_date | DATE | 实际还款日期（nullable） |
| created_at | TIMESTAMP | 创建时间 |

### 4.2 索引设计

```sql
-- 按 token 查账户（授权链路关键路径）
CREATE INDEX idx_account_token ON account(token_id);

-- 按客户查账户
CREATE INDEX idx_account_customer ON account(customer_id);

-- 交易流水查询（账户+时间范围）
CREATE INDEX idx_txn_account_time ON transaction(account_id, txn_time DESC);

-- 按分期查交易
CREATE INDEX idx_txn_installment ON transaction(installment_id) WHERE installment_id IS NOT NULL;

-- 账单查询
CREATE UNIQUE INDEX idx_bill_account_month ON bill(account_id, bill_month);

-- 授权码唯一性（防重复授权）
CREATE UNIQUE INDEX idx_auth_code ON authorization(auth_code);

-- 分期还款计划（按到期日查询催收）
CREATE INDEX idx_installment_schedule_due ON installment_schedule(status, due_date)
  WHERE status = 'OVERDUE';
```

---

## 5. API 接口概要设计

### 5.0 令牌化（Tokenization）原则

> **PCI-DSS 合规核心要求**：系统所有接口严禁传递或存储明文 PAN。所有外部调用方（收单机构、商户系统）必须通过卡组织 TSP（Token Service Provider）或内部 Tokenization Service 将 PAN 转换为 token，再提交至本系统。

**Token 生命周期**：
1. 持卡人在线上/线下支付时，收单机构调用卡组织 TSP，将 PAN 转为 token
2. 商户系统携带 token 调用本系统授权接口
3. 本系统通过 token 解析对应的 account_id，完成授权校验
4. 所有日志、流水、响应中均使用 token，永不出现明文 PAN

### 5.1 账户管理

#### 创建账户
```
POST /api/v1/accounts
Request: { "customer_id": "uuid", "credit_limit": 50000, "billing_day": 15 }
Response: {
  "account_id": "uuid",
  "token": "tkn_****_****1234",   ← Tokenization Service 返回的 token（格式已脱敏）
  "status": "ACTIVE"
}
```

> **token 数据来源说明**：`POST /api/v1/accounts` 的响应字段为 `token`，而非 `masked_card_no`。开卡时，Tokenization Service（TSP，如 Visa Token Service / Mastercard DSP）生成 token 并返回，其格式自带脱敏掩码（如 `tkn_****_****1234`），系统将此 token 存入 Account.token_id 字段，后续所有 API 响应中均使用该 token，从不暴露明文 PAN。

#### 查询账户
```
GET /api/v1/accounts/{account_id}
Response: { "account_id": "...", "credit_limit": 50000, "available_amount": 45000, "status": "ACTIVE" }
```

#### 额度调整
```
PUT /api/v1/accounts/{account_id}/limit
Request: { "new_limit": 60000, "type": "PERMANENT", "reason": "提额审批" }
Response: { "account_id": "...", "credit_limit": 60000 }
```

#### 账户状态变更
```
PUT /api/v1/accounts/{account_id}/status
Request: { "status": "FROZEN", "reason": "疑似欺诈" }
Response: { "account_id": "...", "status": "FROZEN", "updated_at": "..." }
```

### 5.2 交易处理

#### 消费授权（联机）— 使用 token，不接受明文 PAN
```
POST /api/v1/transactions/authorize
Request: {
  "token": "4592-xxxx-xxxx-1234",     ← 卡组织 TSP 生成的 token（必填）
  "amount": 500,
  "currency": "CNY",
  "merchant_id": "M001",
  "merchant_category": "5411",
  "terminal_id": "T001",
  "txn_type": "PURCHASE",
  "channel": "POS"                     ← POS/APP/Web/Recurring
}
Response: {
  "auth_code": "A12345",
  "reference_no": "RN20250531001",
  "status": "APPROVED",
  "account_id": "uuid",
  "available_amount": 44500
}
Error: {
  "code": "INSUFFICIENT_CREDIT",
  "message": "额度不足",
  "available_amount": 200
}
```

> **安全说明**：`token` 字段由收单机构通过卡组织 TSP 接口获得，格式为 "NetworkToken"（如 Visa Token Service 的格式），本系统不接收明文 PAN。

#### 消费确认（清算）
```
POST /api/v1/transactions/settle
Request: { "auth_code": "A12345", "settle_amount": 500 }
Response: { "txn_id": "uuid", "status": "COMPLETED" }
```

#### 还款
```
POST /api/v1/accounts/{account_id}/repayment
Request: { "amount": 5000, "method": "FULL" }
Response: { "txn_id": "uuid", "remaining_balance": 0 }
```

#### 退货
```
POST /api/v1/transactions/{txn_id}/refund
Request: { "amount": 500 }
Response: { "refund_txn_id": "uuid", "status": "COMPLETED" }
```

**分期交易退货处理逻辑**（当 `txn_id` 对应的 Transaction.installment_id 非空时触发）：

| 场景 | 处理逻辑 |
|------|----------|
| 全额退货（退货金额 = 原始消费金额） | ① 生成退款交易（REVERSE）并更新 Transaction.status = REFUNDED；② Installment.status → CANCELLED；③ 将 Installment.remaining_principal 从 used_amount 中释放（扣减 account.installment_used）；④ 将所有未到期 InstallmentSchedule 记录标记为 CANCELLED |
| 部分退货（退货金额 < 原始消费金额） | ① 生成退款交易，金额为部分退货额；② 按比例扣减 Installment.remaining_principal（remaining_principal -= 退货金额）；③ 重新计算剩余期次的每期应还本金（prorate）；④ Installment.installments_remaining 和对应 InstallmentSchedule 数量不变，仅金额调整；⑤ Installment.status 保持 ACTIVE |
| 退货不支持超过原始消费金额 | 超出部分拒绝，返回错误码 REFUND_EXCEEDS_ORIGINAL |

**约束**：
- 若分期已有部分期次逾期，退货操作需先完成逾期还款，否则拒绝（REFUND_BLOCKED_BY_OVERDUE）
- 账单日之后发起的分期退货，不影响本期账单最低还款额计算（退货金额在下期账单冲抵）

### 5.3 分期业务

#### 申请分期
```
POST /api/v1/accounts/{account_id}/installments
Request: {
  "origin_txn_id": "uuid",        ← 消费分期：原始消费交易ID
  "plan_type": "CONSUMPTION",    ← 或 BILL（账单分期）
  "tenure": 12,                   ← 期数：3/6/12/24
  "amount": 12000                 ← 分期本金（消费分期时与 origin_txn_id 关联；账单分期时为申请金额）
}
Response: {
  "installment_id": "uuid",
  "tenure": 12,
  "monthly_payment": 1068.00,
  "total_interest": 816.00,
  "principal_amount": 12000.00,
  "first_due_date": "2025-07-25",
  "status": "ACTIVE"
}
```

#### 查询分期计划
```
GET /api/v1/accounts/{account_id}/installments/{installment_id}
Response: {
  "installment_id": "uuid",
  "plan_type": "CONSUMPTION",
  "tenure": 12,
  "principal_amount": 12000.00,
  "monthly_payment": 1068.00,
  "installments_paid": 3,
  "installments_remaining": 9,
  "remaining_principal": 9000.00,
  "status": "ACTIVE",
  "schedules": [
    { "period_no": 4, "due_date": "2025-10-25", "total_due": 1068.00, "status": "PENDING" },
    ...
  ]
}
```

#### 提前结清分期
```
POST /api/v1/accounts/{account_id}/installments/{installment_id}/early-settle
Response: {
  "installment_id": "uuid",
  "remaining_principal": 9000.00,
  "early_settlement_fee": 45.00,
  "total_settlement_amount": 9045.00,
  "status": "EARLY_SETTLED"
}
```

### 5.4 账单查询

```
GET /api/v1/accounts/{account_id}/bills?month=202505
Response: {
  "bill_id": "uuid",
  "bill_month": "202505",
  "statement_balance": 10000,
  "total_purchase": 8500,
  "total_installment": 1500,
  "min_due": 1000,
  "due_date": "2025-06-25",
  "status": "UNPAID",
  "items": [
    { "txn_id": "uuid", "txn_type": "PURCHASE", "txn_amount": 500, "txn_date": "2025-05-10" },
    { "txn_id": "uuid", "txn_type": "INSTALLMENT", "installment_id": "uuid", "principal": 125, "interest": 43, "txn_date": "2025-05-01" }
  ]
}
```

### 5.5 批量接口

| 接口 | 描述 | 说明 |
|------|------|------|
| POST /api/v1/batch/statements | 批量生成账单 | 定时任务触发，含分期摊销计算 |
| POST /api/v1/batch/interest | 批量计息 | 账单日后一天 |
| POST /api/v1/batch/installment-amortize | 批量分期摊销 | 每日摊销分期本金，更新 account.used_amount |
| GET /api/v1/reports/daily | 日终报表 | 下载 CSV/Excel |

---

## 6. 非功能性需求

### 6.1 安全性

| 需求 | 实现方案 |
|------|----------|
| PAN 令牌化 | 外部系统通过卡组织 TSP 将 PAN 转为 token；系统只接收/存储 token，永不接触明文 PAN |
| 数据加密 | 卡号 AES-256 加密存储（仅限 Token Vault 内部），传输全程 TLS 1.3 |
| 访问控制 | API Gateway 做 Token 鉴权，服务间 mTLS |
| 审计日志 | 所有写操作写入审计表，保留 7 年；日志中 token 做掩码处理（保留前6后4） |
| PCI-DSS Level 1 | 令牌化 + 掩码展示 + TSP 集成 + 密钥管理，满足 PCI-DSS v4.0 第 3.3/3.4/6.5 条 |
| 密钥管理 | 使用 Vault 或云 KMS 管理密钥，密钥轮转 90 天 |
| 渗透测试 | 上线前由第三方完成渗透测试，每年复测 |

### 6.2 并发与性能

| 指标 | 目标值 |
|------|--------|
| 授权接口 P99 | < 100ms |
| 交易处理 TPS | ≥ 3000 |
| 分期查询 P99 | < 300ms |
| 批量账单生成 | 10 万账户 < 30 分钟 |
| 批量分期摊销 | 10 万分期 < 15 分钟 |
| 额度快照缓存更新延迟 | < 5 秒 |

**技术手段**：
- 授权链路：Redis 缓存当前可用额度 + token→account_id 映射，避免每次查 DB
- 乐观锁更新：防止并发额度扣减导致超发
- 连接池优化：HikariCP，核心链路 50 连接，批量 200 连接
- 异步写流水：交易成功后先写 Kafka，消费端落库，平衡性能与一致性

### 6.3 可用性

| 指标 | 目标值 |
|------|--------|
| 系统可用性 | 99.95%（年度停机 < 4.38 小时） |
| 数据持久性 | 99.999%（RPO = 0，零数据丢失） |
| 故障恢复时间 | RTO < 15 分钟 |

**技术手段**：
- 数据库：主从 + 跨 AZ 部署，读写分离
- 缓存：Redis Cluster，哨兵模式 failover
- 服务：无状态部署，K8s HPA 自动扩缩容
- 跨机房容灾：双活部署，主机房故障切备机房
- 幂等设计：所有写操作支持幂等，消息消费去重

### 6.4 可扩展性

- 微服务架构，每个服务独立部署、独立扩缩容
- 事件驱动：通过 Kafka 解耦，交易与账务异步处理
- 配置中心：开关、限流阈值、额度规则可通过配置中心热更新
- 灰度发布：按账户号 Hash 分组灰度，控制在 10%/50%/100%

---

## 7. 关键技术选型

| 组件 | 选型 | 理由 |
|------|------|------|
| 应用框架 | Spring Boot 3.x | 生态成熟，社区活跃 |
| 数据库 | PostgreSQL 16 | 支持 JSON，高可用方案成熟 |
| 缓存 | Redis Cluster 7.x | 额度热点数据，高并发支持 |
| 消息队列 | Kafka 3.x | 高吞吐，事件溯源 |
| 批量调度 | XXL-Job | 任务可视化，HA 支持 |
| 链路追踪 | SkyWalking | 无代码侵入，全链路追踪 |
| 日志 | ELK Stack | 集中日志，检索分析 |

---

## 8. 项目规划

### Phase 1（基础能力，第 1-2 月）
- 账户管理（开卡、销户、状态变更）
- 额度引擎（额度初始化、实时查询）
- 授权服务（联机授权、消费确认）

### Phase 2（交易能力，第 3-4 月）
- 交易核心（消费、退货、还款）
- 定价引擎（手续费、利息计算）
- 冲正/撤销

### Phase 3（账单能力，第 5-6 月）
- 账单生成（含分期摊销）
- 最低还款额计算
- 滞纳金与利息
- 历史账单查询

### Phase 4（高级功能，第 7-8 月）
- 分期业务（Installment + InstallmentSchedule 全套）
- 预授权（酒店/租车）
- 超额控制
- 欺诈监控

---

## 9. 风险与依赖

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| Tokenization 依赖 | 卡组织 TSP 不可用时授权链路中断 | 与卡组织签订 SLA；本地做 token→account_id 缓存，TSP 恢复后同步 |
| 额度计算复杂度 | 预授权+分期+临时额度交叉影响 | 先抽象模型，Phase 1 后再迭代 |
| 对账压力 | 每日千万级流水对账 | T+1 对账任务分离，预留 4 小时窗口 |
| 合规监管 | 利息/滞纳金计算规则可能调整 | 配置化实现，规则可热更新 |
| 数据迁移 | 历史账户数据迁移 | 迁移工具双写验证，上线前全量校验 |

---

*文档版本：v3.0 | 最后更新：2026-07-19*
*修订说明：*
- *v2.0：① 授权 API 改用 token 而非明文 PAN，明确令牌化原则；② 新增 Installment/InstallmentSchedule 分期数据模型*
- *v3.0：① 补充分期退货业务逻辑（全额退货取消分期 + 释放 installment_used；部分退货按比例扣减 remaining_principal）；② 明确 Account.create 响应字段为 TSP 返回的 token（含格式说明），移除 masked_card_no 及相关推导逻辑；③ Installment.status 新增 CANCELLED 状态*