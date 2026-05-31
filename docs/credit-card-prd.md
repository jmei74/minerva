# 信用卡核心系统 PRD 与技术方案

**版本**: v1.0
**作者**: 小晶
**日期**: 2025-05-31
**状态**: 初稿

---

## 1. 背景与目标

本系统面向银行或支付机构，提供信用卡全生命周期管理能力，覆盖账户管理、交易处理、额度控制和账单生成四大核心域。

**核心目标**：
- 支持高并发交易（目标 TPS ≥ 3000）
- 账务数据零差错，事后对账偏差率 < 0.001%
- 系统可用性 99.95%（年度停机 < 4.38 小时）
- 满足 PCI-DSS 安全合规要求

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
| 分期 | 消费分期、账单分期 |
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
| card_no | VARCHAR(20) | 卡号（加密存储） |
| credit_limit | DECIMAL(15,2) | 固定额度 |
| temp_limit | DECIMAL(15,2) | 临时额度 |
| used_amount | DECIMAL(15,2) | 已使用金额 |
| frozen_amount | DECIMAL(15,2) | 冻结金额（预授权） |
| available_amount | DECIMAL(15,2) | 可用金额（计算字段） |
| billing_day | INT | 账单日（1-31） |
| due_day | INT | 到期还款日 |
| status | ENUM | ACTIVE/FROZEN/CLOSED |
| open_date | DATE | 开卡日期 |
| close_date | DATE | 销户日期（nullable） |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |

#### Transaction（交易流水）

| 字段 | 类型 | 说明 |
|------|------|------|
| txn_id | UUID | 主键 |
| account_id | UUID | 账户ID |
| txn_type | ENUM | PURCHASE/WITHDRAWAL/REFUND/REPAYMENT/REVERSAL |
| txn_amount | DECIMAL(15,2) | 交易金额 |
| available_amount_before | DECIMAL(15,2) | 交易前可用额度 |
| available_amount_after | DECIMAL(15,2) | 交易后可用额度 |
| merchant_id | VARCHAR(50) | 商户ID |
| merchant_name | VARCHAR(200) | 商户名称 |
| terminal_id | VARCHAR(50) | 终端ID |
| auth_code | VARCHAR(20) | 授权码（用于联机交易） |
| reference_no | VARCHAR(50) | 参考号 |
| txn_time | TIMESTAMP | 交易时间 |
| settlement_date | DATE | 清算日期（可延后） |
| status | ENUM | PENDING/COMPLETED/REVERSED |
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
| total_purchase | DECIMAL(15,2) | 本期消费合计 |
| total_repayment | DECIMAL(15,2) | 本期还款合计 |
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
| account_id | UUID | 账户ID |
| auth_code | VARCHAR(20) | 授权码 |
| auth_amount | DECIMAL(15,2) | 授权金额 |
| auth_type | ENUM | PRE_AUTH/COMPLETION/CANCEL |
| merchant_id | VARCHAR(50) | 商户ID |
| status | ENUM | PENDING/COMPLETED/EXPIRED/CANCELLED |
| expire_time | TIMESTAMP | 过期时间（通常30天） |
| created_at | TIMESTAMP | 创建时间 |

### 4.2 索引设计

```sql
-- 账户查询加速
CREATE UNIQUE INDEX idx_account_card_no ON account(account_id, card_no(8));

-- 按客户查账户
CREATE INDEX idx_account_customer ON account(customer_id);

-- 交易流水查询（账户+时间范围）
CREATE INDEX idx_txn_account_time ON transaction(account_id, txn_time DESC);

-- 账单查询
CREATE UNIQUE INDEX idx_bill_account_month ON bill(account_id, bill_month);

-- 授权码唯一性（防重复）
CREATE UNIQUE INDEX idx_auth_code ON authorization(auth_code);
```

---

## 5. API 接口概要设计

### 5.1 账户管理

#### 创建账户
```
POST /api/v1/accounts
Request: { "customer_id": "uuid", "credit_limit": 50000, "billing_day": 15 }
Response: { "account_id": "uuid", "card_no": "**** **** **** 1234", "status": "ACTIVE" }
```

#### 查询账户
```
GET /api/v1/accounts/{account_id}
Response: { "account_id": "...", "credit_limit": 50000, "available_amount": 45000, ... }
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

#### 消费授权（联机）
```
POST /api/v1/transactions/authorize
Request: {
  "card_no": "6288888888881234",
  "amount": 500,
  "merchant_id": "M001",
  "terminal_id": "T001",
  "txn_type": "PURCHASE"
}
Response: {
  "auth_code": "A12345",
  "reference_no": "RN20250531001",
  "status": "APPROVED",
  "available_amount": 44500
}
Error: { "code": "INSUFFICIENT_CREDIT", "message": "额度不足" }
```

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

### 5.3 账单查询

```
GET /api/v1/accounts/{account_id}/bills?month=202505
Response: {
  "bill_id": "uuid",
  "bill_month": "202505",
  "statement_balance": 10000,
  "min_due": 1000,
  "due_date": "2025-06-25",
  "status": "UNPAID",
  "items": [...]
}
```

### 5.4 批量接口

| 接口 | 描述 | 说明 |
|------|------|------|
| POST /api/v1/batch/statements | 批量生成账单 | 定时任务触发 |
| POST /api/v1/batch/interest | 批量计息 | 账单日后一天 |
| GET /api/v1/reports/daily | 日终报表 | 下载 CSV/Excel |

---

## 6. 非功能性需求

### 6.1 安全性

| 需求 | 实现方案 |
|------|----------|
| 数据加密 | 卡号 AES-256 加密存储，传输全程 TLS 1.3 |
| 访问控制 | API Gateway 做 Token 鉴权，服务间 mTLS |
| 审计日志 | 所有写操作写入审计表，保留 7 年 |
| PCI-DSS 合规 | 禁止日志中出现完整卡号（掩码展示） |
| 密钥管理 | 使用 Vault 或云 KMS 管理密钥，密钥轮转 90 天 |

### 6.2 并发与性能

| 指标 | 目标值 |
|------|--------|
| 授权接口 P99 | < 100ms |
| 交易处理 TPS | ≥ 3000 |
| 批量账单生成 | 10 万账户 < 30 分钟 |
| 账单查询 P99 | < 500ms |
| 额度快照缓存更新延迟 | < 5 秒 |

**技术手段**：
- 授权链路：Redis 缓存当前可用额度，避免每次查 DB
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
- 账单生成
- 最低还款额计算
- 滞纳金与利息
- 历史账单查询

### Phase 4（高级功能，第 7-8 月）
- 分期业务
- 预授权（酒店/租车）
- 超额控制
- 欺诈监控

---

## 9. 风险与依赖

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 额度计算复杂度 | 预授权+分期+临时额度交叉影响 | 先抽象模型，Phase 1 后再迭代 |
| 对账压力 | 每日千万级流水对账 | T+1 对账任务分离，预留 4 小时窗口 |
| 合规监管 | 利息/滞纳金计算规则可能调整 | 配置化实现，规则可热更新 |
| 数据迁移 | 历史账户数据迁移 | 迁移工具双写验证，上线前全量校验 |

---

*文档版本：v1.0 | 最后更新：2025-05-31*