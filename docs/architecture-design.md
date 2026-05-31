# 信用卡核心系统技术架构设计

**版本**: v1.0
**作者**: 小宗
**日期**: 2025-05-31
**状态**: 初稿
**基于 PRD**: credit-card-prd.md v1.0

---

## 1. 架构设计原则

| 原则 | 说明 |
|------|------|
| 强一致性优先 | 账务数据零差错，TPS ≥ 3000 可用异步写流水+幂等补偿实现 |
| 安全红线不妥协 | PCI-DSS 合规贯穿所有层级 |
| 最小可行架构 | 不引入过度设计，Phase 1 验证后再迭代 |
| 性能有数据再动 | 瓶颈定位后才优化，避免 premature optimization |

---

## 2. 分层架构总览

```
┌────────────────────────────────────────────────────────────────────────────┐
│                              接入层（Gateway Layer）                         │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────┐  ┌─────────────────┐   │
│  │  API Gateway │  │  Kafka Consumer│  │  XXL-Job    │  │  OpenAPI / Webhook│ │
│  │  (Kong/ELB)  │  │  (事件消费)   │  │  (批量调度)  │  │  (外部系统集成)   │ │
│  └──────┬──────┘  └──────┬───────┘  └──────┬──────┘  └────────┬────────┘   │
│         │                 │                │                   │            │
│  ┌──────▼──────────────────▼────────────────▼───────────────────▼────────┐  │
│  │                        服务层（Service Layer）                         │  │
│  │                                                                       │  │
│  │  ┌────────────────┐  ┌────────────────┐  ┌────────────────────────┐    │  │
│  │  │  Account Svc   │  │ Transaction Svc│  │  Billing Svc           │    │  │
│  │  │  账户管理服务   │  │ 交易处理服务    │  │  账单服务              │    │  │
│  │  │  - 开卡/销户   │  │  - 消费/授权   │  │  - 账单生成(批量)      │    │  │
│  │  │  - 额度管理    │  │  - 还款/退货   │  │  - 利息/滞纳金计算     │    │  │
│  │  │  - 账户状态    │  │  - 冲正/撤销   │  │  - 最低还款额计算      │    │  │
│  │  └───────┬────────┘  └───────┬────────┘  └───────────┬────────────┘    │  │
│  │          │                  │                       │                 │  │
│  │  ┌───────▼───────────────────▼───────────────────────▼────────────┐   │  │
│  │  │                    公共能力层（Common Layer）                    │   │  │
│  │  │  ┌─────────────────┐  ┌──────────────┐  ┌─────────────────┐    │   │  │
│  │  │  │  Credit Engine  │  │ Pricing Eng. │  │  Event Bus      │    │   │  │
│  │  │  │  额度引擎       │  │ 定价引擎      │  │  事件总线        │    │   │  │
│  │  │  │  - 可用额度计算 │  │ - 手续费计算  │  │  - Kafka Producer│   │   │  │
│  │  │  │  - 额度扣减/释放│  │ - 利息计算    │  │  - 消息路由      │    │   │  │
│  │  │  │  - 预授权管理  │  │ - 滞纳金计算  │  │                  │    │   │  │
│  │  │  └─────────────────┘  └──────────────┘  └─────────────────┘    │   │  │
│  └───────────────────────────────────────────────────────────────────┘   │
│                                    │                                       │
│  ┌─────────────────────────────────▼───────────────────────────────────┐ │
│  │                         数据层（Data Layer）                           │ │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌────────────────────────┐  │ │
│  │  │  PostgreSQL 16  │  │  Redis Cluster  │  │  Kafka 3.x             │  │ │
│  │  │  (账务核心数据)  │  │  (额度快照/缓存) │  │  (事件驱动/异步写流水) │  │ │
│  │  │  主从 + 读写分离 │  │  3 主 3 从      │  │  多副本 + ISR         │  │ │
│  │  └─────────────────┘  └─────────────────┘  └────────────────────────┘  │ │
│  └───────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
│  ┌──────────────────────────────┐  ┌──────────────────────────────────┐   │
│  │  MongoDB (可选)               │  │  Elasticsearch (可选)               │   │
│  │  操作审计日志/交易流水归档   │  │  日终报表全文检索                   │   │
│  └──────────────────────────────┘  └──────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. 微服务职责边界

### 3.1 Account Service（账户管理服务）

**职责**：账户全生命周期管理

| 模块 | 说明 |
|------|------|
| AccountController | 开卡 / 销户 / 账户查询 / 状态变更 |
| LimitManager | 永久额度 / 临时额度调整（含审批流触发） |
| AccountStatusMachine | 状态机：ACTIVE → FROZEN → CLOSED |

**对外 API**：
- `POST /api/v1/accounts` — 创建账户
- `GET /api/v1/accounts/{account_id}` — 查询账户
- `PUT /api/v1/accounts/{account_id}/limit` — 额度调整
- `PUT /api/v1/accounts/{account_id}/status` — 账户状态变更

**技术约束**：
- 账户创建：先落库再发事件，失败回滚
- 状态变更：需校验无未清账款方可销户
- 敏感数据：card_no 加密存储（AES-256-CBC），仅存储后 4 位明文

### 3.2 Transaction Service（交易处理服务）

**职责**：所有交易流水的发生与记录

| 模块 | 说明 |
|------|------|
| AuthorizationHandler | 联机授权 / 预授权 / 授权完成 / 授权撤销 |
| TransactionProcessor | 消费确认 / 还款 / 退货 / 冲正 |
| IdempotencyManager | 幂等键管理（防重复扣款） |

**对外 API**：
- `POST /api/v1/transactions/authorize` — 消费授权（核心链路，P99 < 100ms）
- `POST /api/v1/transactions/settle` — 消费确认（清算）
- `POST /api/v1/transactions/{txn_id}/refund` — 退货
- `POST /api/v1/accounts/{account_id}/repayment` — 还款

**技术约束**：
- 授权链路：先查 Redis 额度快照判断是否通过，通过后乐观锁更新 DB，异步写流水
- 幂等：基于 auth_code / reference_no 做幂等校验，存储在 Redis（TTL 24h）
- 并发控制：乐观锁（version 字段）+ 重试机制（最多 3 次）

### 3.3 Billing Service（账单服务）

**职责**：账单生成与历史查询

| 模块 | 说明 |
|------|------|
| StatementGenerator | 批量账单生成（XXL-Job 触发） |
| InterestCalculator | 全额计息 / 分期计息 |
| LateFeeProcessor | 滞纳金计算 |
| BillQueryService | 历史账单查询 |

**对外 API**：
- `GET /api/v1/accounts/{account_id}/bills?month=202505` — 账单查询
- `POST /api/v1/batch/statements` — 批量生成账单（内部调用）
- `POST /api/v1/batch/interest` — 批量计息（内部调用）

**技术约束**：
- 账单生成：批量任务，10 万账户 < 30 分钟
- 计息规则：配置化，支持热更新（利率 / 滞纳金比例）

### 3.4 公共能力层

#### 3.4.1 Credit Engine（额度引擎）

**核心公式**：
```
可用额度 = 固定额度 + 临时额度 - 已使用金额 - 冻结金额
```

**功能**：
- 实时额度计算（用于授权决策）
- 额度扣减（消费、预授权）
- 额度释放（退货、预授权撤销、还款）
- 分期占用计入（分期本金在结清前持续占用）

#### 3.4.2 Pricing Engine（定价引擎）

**功能**：
- 取现手续费计算
- 利息计算（全额从消费日起计，未全额还款时全额计息）
- 滞纳金计算（未还最低额时收取）
- 分期手续费计算

---

## 4. 服务间通信方案

### 4.1 通信模式选择原则

| 场景 | 通信方式 | 理由 |
|------|----------|------|
| 实时授权决策 | 同步（REST / gRPC） | 100ms 内需响应，额度查询走同步 |
| 交易流水落库 | 异步（Kafka） | 高并发写入，削峰填谷 |
| 批量任务触发 | 异步（XXL-Job → Kafka） | 账单生成 / 计息按批次触发 |
| 跨服务状态同步 | 异步（Kafka Event） | 账户状态变更通知下游 |
| 超时敏感场景 | Hystrix / Sentinel 熔断 | 下游服务不可用时降级 |

### 4.2 同步通信（REST/gRPC）

```
Transaction Svc ──gRPC──> Credit Engine
     │                         │
     │<─────额度查询─────────────┘
     │
     └─REST──> Account Svc（查询账户信息，不频繁）
```

**技术选型**：
- 内部服务通信优先 gRPC（性能更好，Protobuf 序列化）
- 跨语言或公网边界用 REST
- 超时配置：主链路 100ms，非主链路 500ms

### 4.3 异步通信（Kafka Event）

```
Transaction Svc
    │
    ├──[publish]──> topic: transaction.events
    │                     │
    │                     ├──[consumer]──> Account Svc（更新已用金额）
    │                     ├──[consumer]──> Billing Svc（记录交易到账单）
    │                     └──[consumer]──> Reconciliation Svc（日终对账）

Account Svc
    │
    ├──[publish]──> topic: account.events
    │                     │
    │                     ├──[consumer]──> Notification Svc（短信/推送）
    │                     └──[consumer]──> Audit Svc（审计日志）
```

**消息可靠性**：
- Producer：acks=all，retries=3
- Consumer：手动提交 offset，失败重试，死信队列（DLQ）处理

---

## 5. 数据库表结构设计

### 5.1 PostgreSQL 表设计

#### 5.1.1 account（账户表）

```sql
CREATE TABLE account (
    account_id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id         UUID NOT NULL,
    card_no_encrypted   VARCHAR(256) NOT NULL,          -- AES-256 加密
    card_no_last4       CHAR(4) NOT NULL,                -- 仅后 4 位明文
    credit_limit        DECIMAL(15,2) NOT NULL DEFAULT 0,
    temp_limit          DECIMAL(15,2) NOT NULL DEFAULT 0,
    used_amount         DECIMAL(15,2) NOT NULL DEFAULT 0,
    frozen_amount       DECIMAL(15,2) NOT NULL DEFAULT 0,
    available_amount    DECIMAL(15,2) GENERATED ALWAYS AS (
                            credit_limit + temp_limit - used_amount - frozen_amount
                        ) STORED,
    billing_day         SMALLINT NOT NULL CHECK (billing_day BETWEEN 1 AND 31),
    due_days            SMALLINT NOT NULL DEFAULT 20,   -- 到期日距账单日天数
    over_limit_ratio    DECIMAL(5,4) NOT NULL DEFAULT 0.1000,  -- 超额比例 10%
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                        CHECK (status IN ('PENDING', 'ACTIVE', 'FROZEN', 'CLOSED')),
    version             BIGINT NOT NULL DEFAULT 0,      -- 乐观锁
    open_date           DATE NOT NULL,
    close_date          DATE,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE account IS '信用卡账户表';
CREATE UNIQUE INDEX idx_account_card_last4 ON account(card_no_last4);
CREATE INDEX idx_account_customer ON account(customer_id);
CREATE INDEX idx_account_status ON account(status);
```

#### 5.1.2 transaction（交易流水表）

```sql
CREATE TABLE transaction (
    txn_id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id              UUID NOT NULL REFERENCES account(account_id),
    txn_type                VARCHAR(30) NOT NULL
                            CHECK (txn_type IN (
                                'PURCHASE', 'WITHDRAWAL', 'REFUND',
                                'REPAYMENT', 'REVERSAL', 'PRE_AUTH',
                                'AUTH_COMPLETION', 'AUTH_CANCEL', 'INSTALLMENT'
                            )),
    txn_amount              DECIMAL(15,2) NOT NULL,
    currency                VARCHAR(3) NOT NULL DEFAULT 'CNY',
    available_amount_before DECIMAL(15,2) NOT NULL,
    available_amount_after  DECIMAL(15,2) NOT NULL,
    merchant_id             VARCHAR(50),
    merchant_name           VARCHAR(200),
    terminal_id             VARCHAR(50),
    auth_code               VARCHAR(20),
    reference_no            VARCHAR(50),
    idempotency_key         VARCHAR(100) UNIQUE,        -- 幂等键
    txn_time                TIMESTAMP NOT NULL DEFAULT NOW(),
    settlement_date         DATE,
    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                            CHECK (status IN ('PENDING', 'COMPLETED', 'REVERSED', 'FAILED')),
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE transaction IS '交易流水表';
CREATE INDEX idx_txn_account_time ON transaction(account_id, txn_time DESC);
CREATE INDEX idx_txn_auth_code ON transaction(auth_code) WHERE auth_code IS NOT NULL;
CREATE INDEX idx_txn_idempotency ON transaction(idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE INDEX idx_txn_reference ON transaction(reference_no) WHERE reference_no IS NOT NULL;
CREATE INDEX idx_txn_settlement ON transaction(settlement_date) WHERE settlement_date IS NOT NULL;
```

#### 5.1.3 authorization（授权表）

```sql
CREATE TABLE authorization (
    auth_id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id       UUID NOT NULL REFERENCES account(account_id),
    auth_code        VARCHAR(20) NOT NULL UNIQUE,
    auth_type        VARCHAR(20) NOT NULL CHECK (auth_type IN ('PRE_AUTH', 'COMPLETION', 'CANCEL')),
    auth_amount      DECIMAL(15,2) NOT NULL,
    consumed_amount  DECIMAL(15,2) NOT NULL DEFAULT 0,
    merchant_id      VARCHAR(50),
    merchant_name    VARCHAR(200),
    terminal_id      VARCHAR(50),
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                     CHECK (status IN ('PENDING', 'COMPLETED', 'EXPIRED', 'CANCELLED')),
    expire_time      TIMESTAMP NOT NULL,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE authorization IS '授权表（预授权/授权完成）';
CREATE INDEX idx_auth_account ON authorization(account_id);
CREATE INDEX idx_auth_expire ON authorization(expire_time) WHERE status = 'PENDING';
```

#### 5.1.4 installment（分期表）

```sql
CREATE TABLE installment (
    installment_id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id               UUID NOT NULL REFERENCES account(account_id),
    original_txn_id         UUID REFERENCES transaction(txn_id),
    bill_id                  UUID REFERENCES bill(bill_id),
    installment_type        VARCHAR(20) NOT NULL CHECK (installment_type IN ('PURCHASE', 'BILL')),
    principal               DECIMAL(15,2) NOT NULL,          -- 分期本金
    total_installments      SMALLINT NOT NULL,               -- 总期数
    completed_installments  SMALLINT NOT NULL DEFAULT 0,     -- 已还期数
    installment_amount      DECIMAL(15,2) NOT NULL,         -- 每期还款金额
    fee_rate                DECIMAL(5,4) NOT NULL,          -- 手续费率（月）
    total_fee               DECIMAL(15,2) NOT NULL,         -- 总手续费
    monthly_payment         DECIMAL(15,2) NOT NULL,         -- 每月应还（本金+手续费）
    first_due_date          DATE NOT NULL,                   -- 首次到期日
    status                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                            CHECK (status IN ('ACTIVE', 'COMPLETED', 'CANCELLED', 'OVERDUE')),
    start_date              DATE NOT NULL,
    end_date                DATE,                            -- 结清日期
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE installment IS '分期记录表';
CREATE INDEX idx_inst_account ON installment(account_id);
CREATE INDEX idx_inst_bill ON installment(bill_id) WHERE bill_id IS NOT NULL;
CREATE INDEX idx_inst_status ON installment(status) WHERE status IN ('ACTIVE', 'OVERDUE');
```

#### 5.1.5 installment_schedule（分期还款计划表）

```sql
CREATE TABLE installment_schedule (
    schedule_id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    installment_id     UUID NOT NULL REFERENCES installment(installment_id),
    account_id         UUID NOT NULL REFERENCES account(account_id),
    period             SMALLINT NOT NULL,                   -- 第 N 期
    principal_amount   DECIMAL(15,2) NOT NULL,            -- 本金
    fee_amount         DECIMAL(15,2) NOT NULL,            -- 手续费
    total_amount       DECIMAL(15,2) NOT NULL,            -- 应还合计
    due_date           DATE NOT NULL,
    paid_amount        DECIMAL(15,2) NOT NULL DEFAULT 0,
    status             VARCHAR(20) NOT NULL DEFAULT 'UNPAID'
                       CHECK (status IN ('UNPAID', 'PAID', 'OVERDUE')),
    paid_txn_id        UUID REFERENCES transaction(txn_id),
    created_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE installment_schedule IS '分期还款计划表';
CREATE UNIQUE INDEX idx_sched_inst_period ON installment_schedule(installment_id, period);
CREATE INDEX idx_sched_due ON installment_schedule(due_date) WHERE status = 'UNPAID';
CREATE INDEX idx_sched_account_due ON installment_schedule(account_id, due_date);
```

#### 5.1.6 bill（账单表）

```sql
CREATE TABLE bill (
    bill_id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id           UUID NOT NULL REFERENCES account(account_id),
    bill_month           VARCHAR(6) NOT NULL,              -- YYYYMM
    statement_date       DATE NOT NULL,
    due_date             DATE NOT NULL,
    opening_balance     DECIMAL(15,2) NOT NULL DEFAULT 0,
    total_purchase      DECIMAL(15,2) NOT NULL DEFAULT 0,
    total_repayment     DECIMAL(15,2) NOT NULL DEFAULT 0,
    total_installment   DECIMAL(15,2) NOT NULL DEFAULT 0,  -- 本期分期摊销
    min_due              DECIMAL(15,2) NOT NULL DEFAULT 0,
    statement_balance   DECIMAL(15,2) NOT NULL DEFAULT 0,
    interest             DECIMAL(15,2) NOT NULL DEFAULT 0,
    late_fee             DECIMAL(15,2) NOT NULL DEFAULT 0,
    previous_min_due_unpaid DECIMAL(15,2) NOT NULL DEFAULT 0,  -- 上期最低额未还
    status               VARCHAR(20) NOT NULL DEFAULT 'UNPAID'
                        CHECK (status IN ('UNPAID', 'PARTIAL_PAID', 'PAID', 'OVERDUE')),
    created_at           TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_bill_account_month UNIQUE (account_id, bill_month)
);

COMMENT ON TABLE bill IS '账单表';
CREATE INDEX idx_bill_account_month ON bill(account_id, bill_month DESC);
```

#### 5.1.7 bill_item（账单明细表）

```sql
CREATE TABLE bill_item (
    item_id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bill_id         UUID NOT NULL REFERENCES bill(bill_id),
    account_id      UUID NOT NULL REFERENCES account(account_id),
    txn_id          UUID REFERENCES transaction(txn_id),
    txn_type        VARCHAR(30) NOT NULL,
    description     VARCHAR(200),
    txn_date        DATE NOT NULL,
    txn_amount      DECIMAL(15,2) NOT NULL,
    currency        VARCHAR(3) NOT NULL DEFAULT 'CNY',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE bill_item IS '账单明细表';
CREATE INDEX idx_item_bill ON bill_item(bill_id);
```

#### 5.1.8 credit_limit_adjustment（额度调整记录表）

```sql
CREATE TABLE credit_limit_adjustment (
    adj_id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id      UUID NOT NULL REFERENCES account(account_id),
    adj_type        VARCHAR(20) NOT NULL CHECK (adj_type IN ('PERMANENT', 'TEMP')),
    old_limit       DECIMAL(15,2) NOT NULL,
    new_limit       DECIMAL(15,2) NOT NULL,
    reason          VARCHAR(500),
    approval_id     VARCHAR(50),                          -- 审批流 ID
    status          VARCHAR(20) NOT NULL DEFAULT 'APPROVED'
                   CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    operator_id     VARCHAR(50),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_adj_account ON credit_limit_adjustment(account_id, created_at DESC);
```

#### 5.1.9 操作审计表

```sql
CREATE TABLE audit_log (
    audit_id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_name    VARCHAR(50) NOT NULL,
    operation       VARCHAR(50) NOT NULL,
    account_id      UUID,
    operator_id     VARCHAR(50),
    request_id      VARCHAR(100) NOT NULL,                 -- 全链路 trace_id
    ip_address      VARCHAR(45),
    request_payload TEXT,                                  -- 脱敏后
    response_status SMALLINT,
    duration_ms     BIGINT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
) PARTITION BY RANGE (created_at);

-- 按月分区，保留 7 年（84 个分区）
CREATE TABLE audit_log_2025_05 PARTITION OF audit_log
    FOR VALUES FROM ('2025-05-01') TO ('2025-06-01');

CREATE INDEX idx_audit_request ON audit_log(request_id);
CREATE INDEX idx_audit_account ON audit_log(account_id) WHERE account_id IS NOT NULL;
```

### 5.2 索引设计总结

| 表 | 索引 | 类型 | 用途 |
|------|------|------|------|
| account | `idx_account_card_last4` | UNIQUE | 卡号后 4 位查询 |
| account | `idx_account_customer` | NORMAL | 按客户查账户 |
| transaction | `idx_txn_account_time` | NORMAL | 账户交易历史 |
| transaction | `idx_txn_idempotency` | UNIQUE | 幂等校验 |
| transaction | `idx_txn_auth_code` | NORMAL | 授权码查交易 |
| bill | `uk_bill_account_month` | UNIQUE | 账单唯一性 |
| authorization | `idx_auth_expire` | NORMAL | 预授权过期清理 |
| installment_schedule | `idx_sched_due` | NORMAL | 批量扣款任务 |

---

## 6. Redis 缓存策略

### 6.1 Key 设计

| Key Pattern | 类型 | TTL | 说明 |
|-------------|------|-----|------|
| `credit:snapshot:{account_id}` | Hash | 5s | 额度快照（实时性要求高） |
| `credit:available:{account_id}` | String | 5s | 可用额度计算值 |
| `auth:idempotency:{idempotency_key}` | String | 24h | 幂等键（防重复扣款） |
| `auth:pending:{auth_code}` | Hash | 30d | 预授权信息 |
| `bill:summary:{account_id}:{bill_month}` | Hash | 24h | 账单摘要缓存 |
| `lock:account:{account_id}` | String | 10s | 分布式锁（额度更新） |
| `rate:limit:{client_id}` | String | 1min | 限流计数器 |

### 6.2 额度快照缓存

**问题**：高并发授权场景下，每次都查 DB 会成为瓶颈。

**解决方案**：

```
授权请求 → 查 Redis 额度快照 → 通过 → 乐观锁更新 DB → 更新 Redis 快照
                                    ↓ 失败
                              Redis 分布式锁 + 重试
```

**数据一致性保证**：
- Redis 快照 TTL = 5 秒（强制回源）
- DB 更新成功后异步更新 Redis
- Redis 与 DB 不一致时，以 DB 为准（Cache-Aside 模式）

**Redis 数据结构**：
```json
HSET credit:snapshot:{account_id}
  credit_limit 50000
  temp_limit 10000
  used_amount 15000
  frozen_amount 2000
  available_amount 43000
  version 12345
```

### 6.3 热点数据缓存

| 数据 | 缓存策略 | 更新机制 |
|------|----------|----------|
| 账户信息 | Read-Through | 更新时删除缓存 |
| 账单摘要 | TTL 24h | 账单生成后写入 |
| 分期计划 | TTL 1h | 还款后删除 |
| 授权码信息 | TTL 30d | 完成/撤销时删除 |

---

## 7. Kafka 事件清单

### 7.1 Topic 设计

| Topic | 分区数 | 副本数 | 保留时间 | 说明 |
|-------|--------|--------|----------|------|
| `credit-card.transaction.events` | 32 | 3 | 7 天 | 交易事件（消费/退货/授权） |
| `credit-card.account.events` | 16 | 3 | 7 天 | 账户事件（开卡/销户/状态变更） |
| `credit-card.billing.events` | 16 | 3 | 30 天 | 账单事件（生成/计息） |
| `credit-card.credit.events` | 16 | 3 | 7 天 | 额度事件（调整/占用/释放） |
| `credit-card.dlq` | 4 | 3 | 30 天 | 死信队列 |

### 7.2 事件详细设计

#### 7.2.1 transaction.events

```json
// 消费授权通过
{
  "event_id": "uuid",
  "event_type": "AUTHORIZATION_APPROVED",
  "occurred_at": "2025-05-31T10:00:00Z",
  "version": 1,
  "payload": {
    "account_id": "uuid",
    "auth_code": "A12345",
    "txn_type": "PURCHASE",
    "auth_amount": 500.00,
    "merchant_id": "M001",
    "merchant_name": "星巴克咖啡",
    "terminal_id": "T001",
    "available_amount_before": 45000.00,
    "available_amount_after": 44500.00,
    "reference_no": "RN20250531001"
  }
}

// 消费确认（清算）
{
  "event_id": "uuid",
  "event_type": "TRANSACTION_COMPLETED",
  "occurred_at": "2025-05-31T10:05:00Z",
  "version": 1,
  "payload": {
    "txn_id": "uuid",
    "account_id": "uuid",
    "auth_code": "A12345",
    "txn_type": "PURCHASE",
    "settle_amount": 500.00,
    "txn_time": "2025-05-31T10:00:00Z"
  }
}

// 还款成功
{
  "event_id": "uuid",
  "event_type": "REPAYMENT_SUCCESS",
  "occurred_at": "2025-05-31T14:00:00Z",
  "version": 1,
  "payload": {
    "txn_id": "uuid",
    "account_id": "uuid",
    "repayment_amount": 5000.00,
    "method": "FULL",
    "available_amount_after": 49500.00
  }
}

// 退货成功
{
  "event_id": "uuid",
  "event_type": "REFUND_SUCCESS",
  "occurred_at": "2025-05-31T15:00:00Z",
  "version": 1,
  "payload": {
    "refund_txn_id": "uuid",
    "original_txn_id": "uuid",
    "account_id": "uuid",
    "refund_amount": 500.00,
    "available_amount_after": 50000.00
  }
}
```

#### 7.2.2 account.events

```json
// 账户创建
{
  "event_id": "uuid",
  "event_type": "ACCOUNT_OPENED",
  "occurred_at": "2025-05-31T09:00:00Z",
  "version": 1,
  "payload": {
    "account_id": "uuid",
    "customer_id": "uuid",
    "card_no_last4": "1234",
    "credit_limit": 50000.00,
    "billing_day": 15,
    "due_days": 20,
    "status": "ACTIVE"
  }
}

// 账户状态变更
{
  "event_id": "uuid",
  "event_type": "ACCOUNT_STATUS_CHANGED",
  "occurred_at": "2025-05-31T10:00:00Z",
  "version": 1,
  "payload": {
    "account_id": "uuid",
    "old_status": "ACTIVE",
    "new_status": "FROZEN",
    "reason": "疑似欺诈",
    "operator_id": "SYS"
  }
}

// 额度调整
{
  "event_id": "uuid",
  "event_type": "CREDIT_LIMIT_ADJUSTED",
  "occurred_at": "2025-05-31T11:00:00Z",
  "version": 1,
  "payload": {
    "account_id": "uuid",
    "adj_type": "PERMANENT",
    "old_limit": 50000.00,
    "new_limit": 60000.00,
    "approval_id": "APPR001"
  }
}
```

#### 7.2.3 billing.events

```json
// 账单生成
{
  "event_id": "uuid",
  "event_type": "BILL_GENERATED",
  "occurred_at": "2025-05-31T08:00:00Z",
  "version": 1,
  "payload": {
    "bill_id": "uuid",
    "account_id": "uuid",
    "bill_month": "202505",
    "statement_balance": 10000.00,
    "min_due": 1000.00,
    "due_date": "2025-06-25",
    "item_count": 15
  }
}

// 利息计算
{
  "event_id": "uuid",
  "event_type": "INTEREST_CALCULATED",
  "occurred_at": "2025-06-01T02:00:00Z",
  "version": 1,
  "payload": {
    "bill_id": "uuid",
    "account_id": "uuid",
    "interest_amount": 150.00,
    "bill_month": "202505"
  }
}

// 滞纳金计算
{
  "event_id": "uuid",
  "event_type": "LATE_FEE_CALCULATED",
  "occurred_at": "2025-06-01T02:00:00Z",
  "version": 1,
  "payload": {
    "bill_id": "uuid",
    "account_id": "uuid",
    "late_fee_amount": 50.00,
    "unpaid_min_due": 1000.00,
    "bill_month": "202504"
  }
}
```

#### 7.2.4 credit.events

```json
// 额度占用（消费授权）
{
  "event_id": "uuid",
  "event_type": "CREDIT_FROZEN",
  "occurred_at": "2025-05-31T10:00:00Z",
  "version": 1,
  "payload": {
    "account_id": "uuid",
    "frozen_amount_delta": 500.00,
    "available_amount_after": 44500.00,
    "reference": "auth:A12345"
  }
}

// 额度释放（退货/授权撤销）
{
  "event_id": "uuid",
  "event_type": "CREDIT_UNFROZEN",
  "occurred_at": "2025-05-31T15:00:00Z",
  "version": 1,
  "payload": {
    "account_id": "uuid",
    "frozen_amount_delta": -500.00,
    "available_amount_after": 45000.00,
    "reference": "txn:uuid"
  }
}

// 分期占用额度
{
  "event_id": "uuid",
  "event_type": "INSTALLMENT_CREDIT_FROZEN",
  "occurred_at": "2025-05-31T10:00:00Z",
  "version": 1,
  "payload": {
    "account_id": "uuid",
    "installment_id": "uuid",
    "frozen_amount_delta": 3000.00,
    "available_amount_after": 42000.00
  }
}
```

---

## 8. API 网关设计方案

### 8.1 技术选型

**选 Kong**，不选 Spring Cloud Gateway，原因：
- Kong 基于 Nginx + Lua，性能优于 Java 写的 Gateway
- 插件生态丰富（JWT、rate-limit、request-transformer）
- 声明式配置，运维友好
- 支持 Cassandra 分布式配置存储

### 8.2 核心功能

#### 8.2.1 鉴权

```
客户端请求
    │
    ▼
┌─────────────────────────────────┐
│  1. TLS termination             │
│  2. API Key / JWT 验证          │  ← Kong JWT plugin
│  3. Client ID 白名单检查        │
│  4. 敏感字段脱敏（卡号掩码）     │
└────────────┬──────────────────────┘
             │ 通过
             ▼
┌─────────────────────────────────┐
│  Upstream（微服务）              │
└─────────────────────────────────┘
```

**JWT Token 结构**：
```json
{
  "sub": "user_id",
  "client_id": "app_client_id",
  "permissions": ["account:read", "transaction:write"],
  "iat": 1717123200,
  "exp": 1717126800
}
```

**PCI-DSS 要求**：
- 请求 / 响应中的完整卡号必须掩码（Kong request-transformer + response-transformer）
- 日志中禁止记录完整卡号（Kong log 插件配置脱敏）

#### 8.2.2 限流

| 维度 | 配置 | 说明 |
|------|------|------|
| 全局 | 10000 req/s | 网关级别 |
| per client_id | 1000 req/min | 应用级别 |
| 授权接口 | 500 req/min | 核心链路单独限流 |
| 幂等键校验 | 100 req/min | 防滥用 |

**算法**：令牌桶（Token Bucket），Redis 存储计数器。

**限流响应**：
```json
HTTP 429 Too Many Requests
Retry-After: 60
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 0
```

#### 8.2.3 日志与链路追踪

```
┌─────────────────────────────────────────────────────────┐
│  Kong Access Log（JSON）                               │
│  {                                                     │
│    "request_id": "trace-id",                           │
│    "client_id": "app001",                             │
│    "method": "POST",                                  │
│    "path": "/api/v1/transactions/authorize",          │
│    "upstream_latency_ms": 45,                         │
│    "status": 200,                                     │
│    "card_no_masked": "**** **** **** 1234"           │  ← PCI-DSS
│  }                                                     │
└─────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────┐
│  ELK Stack                      │
│  - Kafka → Logstash → ES        │
│  - Kibana 可视化                │
└─────────────────────────────────┘
```

**链路追踪**：SkyWalking Agent（Java 服务）+ Kong 插件（inject trace_id），
全链路 trace_id 透传。

#### 8.2.4 服务发现与路由

```
Kong → Consul（服务注册发现）
         │
         ├── account-svc:8080
         ├── transaction-svc:8080
         └── billing-svc:8080
```

### 8.3 API Gateway 路由配置

```yaml
# Kong declarative config (deck yaml)
services:
  - name: account-service
    url: http://account-svc:8080
    routes:
      - name: account-route
        paths: ["/api/v1/accounts"]
        methods: ["GET", "POST", "PUT"]
        plugins:
          - name: jwt
          - name: rate-limiting
            config: { minute: 1000 }

  - name: transaction-service
    url: http://transaction-svc:8080
    routes:
      - name: authorize-route
        paths: ["/api/v1/transactions/authorize"]
        methods: ["POST"]
        plugins:
          - name: jwt
          - name: rate-limiting
            config: { minute: 500 }    # 核心链路更严格
          - name: request-transformer  # 卡号脱敏
          - name: correlation-id        # trace_id

  - name: billing-service
    url: http://billing-svc:8080
    routes:
      - name: billing-route
        paths: ["/api/v1/bills", "/api/v1/batch"]
        methods: ["GET", "POST"]
        plugins:
          - name: jwt
          - name: rate-limiting
            config: { minute: 200 }
```

---

## 9. 技术决策记录（ADR）

| ID | 决策 | 理由 | 风险 |
|----|------|------|------|
| ADR-001 | 授权链路用 Redis 快照判断额度 | 高并发场景避免 DB 瓶颈，P99 < 100ms | Redis 与 DB 不一致风险，用 5s TTL + Cache-Aside 缓解 |
| ADR-002 | 交易流水异步写 Kafka | 交易成功即返回，流水异步落库，削峰 | 消息丢失风险，acks=all + 消费者手动提交缓解 |
| ADR-003 | 幂等键存 Redis TTL 24h | 防重复扣款，轻量实现 | Redis 不可用时降级为 DB 查询幂等键 |
| ADR-004 | 账单批量生成用 XXL-Job | 任务可视化，支持 HA | 10 万账户 < 30 分钟，需优化 SQL 批量 |
| ADR-005 | 分期占用计入 frozen_amount | 额度模型统一，计算简单 | 分期提前结清时需主动释放额度 |
| ADR-006 | 使用 Kong API Gateway | 性能优于 Java Gateway，插件生态好 | 学习曲线，Lua 插件开发需额外投入 |
| ADR-007 | PostgreSQL 乐观锁更新额度 | 避免分布式锁性能损耗 | 热点账户冲突重试，最多 3 次 |

---

## 10. 部署架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        AWS / 阿里云（多 AZ）                     │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  K8s Cluster（EKS/ACK）                                 │   │
│  │                                                        │   │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐            │   │
│  │  │ Kong GW  │  │ Kong GW  │  │ Kong GW  │  ← 3 副本   │   │
│  │  └────┬─────┘  └────┬─────┘  └────┬─────┘            │   │
│  │       └───────────────┼──────────────┘                 │   │
│  │                       │ LB                               │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │   │
│  │  │ Account Svc │  │Transaction  │  │ Billing Svc │    │   │
│  │  │  5+ pods    │  │Svc  10+ pods │  │  3+ pods    │    │   │
│  │  └─────────────┘  └─────────────┘  └─────────────┘    │   │
│  │                                                        │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌────────────┐  │   │
│  │  │ Credit Engine│  │Pricing Engine│  │ XXL-Job   │  │   │
│  │  │  5+ pods     │  │  3+ pods     │  │  2+ pods  │  │   │
│  │  └──────────────┘  └──────────────┘  └────────────┘  │   │
│  └─────────────────────────────────────────────────────────┘   │
│                            │                                    │
│  ┌─────────────────────────▼────────────────────────────────┐ │
│  │  数据层                                                    │ │
│  │  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌─────────┐ │ │
│  │  │ PostgreSQL │ │ Redis      │ │  Kafka     │ │ Consul  │ │ │
│  │  │ Primary+   │ │ Cluster    │ │  Cluster   │ │ Registry│ │ │
│  │  │ Standby    │ │ 3主3从     │ │  多副本    │ │         │ │ │
│  │  └────────────┘ └────────────┘ └────────────┘ └─────────┘ │ │
│  └────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────┘
```

**扩缩容策略**：
- Transaction Svc：HPA based on CPU > 70% + 授权 QPS
- Account Svc / Billing Svc：HPA based on CPU > 70%
- 最小副本数：3（可用性保证）

---

## 11. 安全设计（PCI-DSS 合规）

| 控制项 | 实现措施 |
|--------|----------|
| 数据加密 | card_no：AES-256-CBC（KMS 管理密钥，90 天轮转） |
| 传输加密 | 全链路 TLS 1.3（Gateway → Service → DB/Redis/Kafka） |
| 访问控制 | mTLS（服务间）+ JWT（外部 API） |
| 密钥管理 | AWS KMS / HashiCorp Vault |
| 审计日志 | 所有写操作写审计表（分区表），保留 7 年 |
| 卡号掩码 | 日志 / 响应中禁止明文卡号，掩码展示 `**** **** **** 1234` |
| 漏洞扫描 | 每季度 OWASP Top 10 扫描 |
| 渗透测试 | 每年一次 |

---

## 12. 性能预算

| 链路 | 目标 | 当前估算 | 瓶颈 | 优化手段 |
|------|------|----------|------|----------|
| 授权 P99 | < 100ms | 60-80ms | Redis 查询 + DB 更新 | 快照缓存 + 异步写流水 |
| 账单生成 | 10 万 < 30min | 预计 20-25min | DB 批量写入 | 批量 INSERT + 多 worker |
| 账单查询 P99 | < 500ms | 200-400ms | DB 复杂查询 | bill_item 缓存 + 分页 |

---

## 13. 与 PRD 的对应关系

| PRD 章节 | 对应架构设计 |
|----------|--------------|
| 2.1 账户管理 | Account Service（3.1）+ account 表（5.1.1） |
| 2.2 交易处理 | Transaction Service（3.2）+ transaction 表（5.1.2） |
| 2.3 额度控制 | Credit Engine（3.4.1）+ Redis 缓存（6）+ credit.events（7.2.4） |
| 2.4 账单生成 | Billing Service（3.3）+ bill/installment 表（5.1.4/5.1.5/5.1.6） |
| 6.1 安全性 | API Gateway 鉴权（8）+ PCI-DSS 合规（11） |
| 6.2 并发与性能 | Redis 缓存（6）+ 异步写流水（4.3） |
| 6.3 可用性 | K8s HA + PostgreSQL 主从 + Redis Cluster（10） |
| 6.4 可扩展性 | 微服务拆分（3）+ Kafka 解耦（4.3）+ 配置中心（10） |
| 7.1 API 接口 | 各服务 API（3.1-3.3）+ Kong 路由（8.3） |
| 7.2 技术选型 | 选型一致：Spring Boot 3.x / PostgreSQL 16 / Redis 7.x / Kafka 3.x |

---

*文档版本：v1.0 | 最后更新：2025-05-31*