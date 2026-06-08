-- Flyway migration: V1__init_schema.sql
-- 信用卡核心系统数据库初始化
-- 架构参考: architecture-design.md §5

-- 1. 账户表
CREATE TABLE IF NOT EXISTS account (
    account_id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id         UUID NOT NULL,
    card_no_encrypted   VARCHAR(256) NOT NULL,
    card_no_last4       VARCHAR(4) NOT NULL,
    credit_limit        DECIMAL(15,2) NOT NULL DEFAULT 0,
    temp_limit          DECIMAL(15,2) NOT NULL DEFAULT 0,
    used_amount         DECIMAL(15,2) NOT NULL DEFAULT 0,
    frozen_amount       DECIMAL(15,2) NOT NULL DEFAULT 0,
    billing_day         INTEGER NOT NULL CHECK (billing_day BETWEEN 1 AND 31),
    due_days            INTEGER NOT NULL DEFAULT 20,
    over_limit_ratio    DECIMAL(5,4) NOT NULL DEFAULT 0.1000,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING', 'ACTIVE', 'FROZEN', 'CLOSED')),
    version             BIGINT NOT NULL DEFAULT 0,
    open_date           DATE NOT NULL,
    close_date          DATE,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_account_card_last4 ON account(card_no_last4);
CREATE INDEX IF NOT EXISTS idx_account_customer ON account(customer_id);
CREATE INDEX IF NOT EXISTS idx_account_status ON account(status);

-- 2. 交易流水表
CREATE TABLE IF NOT EXISTS transaction (
    txn_id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id              UUID NOT NULL REFERENCES account(account_id),
    txn_type                VARCHAR(30) NOT NULL
                            CHECK (txn_type IN (
                                'PURCHASE', 'WITHDRAWAL', 'REFUND',
                                'REPAYMENT', 'REVERSAL', 'PRE_AUTH',
                                'AUTH_COMPLETION', 'AUTH_CANCEL', 'INSTALLMENT')),
    txn_amount              DECIMAL(15,2) NOT NULL,
    currency                VARCHAR(3) NOT NULL DEFAULT 'CNY',
    available_amount_before DECIMAL(15,2) NOT NULL,
    available_amount_after  DECIMAL(15,2) NOT NULL,
    merchant_id             VARCHAR(50),
    merchant_name           VARCHAR(200),
    terminal_id             VARCHAR(50),
    auth_code               VARCHAR(20),
    reference_no            VARCHAR(50),
    idempotency_key         VARCHAR(100) UNIQUE,
    txn_time                TIMESTAMP NOT NULL DEFAULT NOW(),
    settlement_date         DATE,
    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                            CHECK (status IN ('PENDING', 'COMPLETED', 'REVERSED', 'FAILED')),
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_txn_account_time ON transaction(account_id, txn_time DESC);
CREATE INDEX IF NOT EXISTS idx_txn_auth_code ON transaction(auth_code) WHERE auth_code IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_txn_idempotency ON transaction(idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_txn_reference ON transaction(reference_no) WHERE reference_no IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_txn_settlement ON transaction(settlement_date) WHERE settlement_date IS NOT NULL;

-- 3. 授权表（card_authorization 避免与 PostgreSQL 保留字冲突）
CREATE TABLE IF NOT EXISTS card_authorization (
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

CREATE INDEX IF NOT EXISTS idx_auth_account ON card_authorization(account_id);
CREATE INDEX IF NOT EXISTS idx_auth_expire ON card_authorization(expire_time) WHERE status = 'PENDING';

-- 4. 额度调整记录表
CREATE TABLE IF NOT EXISTS credit_limit_adjustment (
    adj_id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id      UUID NOT NULL REFERENCES account(account_id),
    adj_type        VARCHAR(20) NOT NULL CHECK (adj_type IN ('PERMANENT', 'TEMP')),
    old_limit       DECIMAL(15,2) NOT NULL,
    new_limit       DECIMAL(15,2) NOT NULL,
    reason          VARCHAR(500),
    approval_id     VARCHAR(50),
    status          VARCHAR(20) NOT NULL DEFAULT 'APPROVED'
                   CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    operator_id     VARCHAR(50),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_adj_account ON credit_limit_adjustment(account_id, created_at DESC);

-- 5. 贵宾室权益表
CREATE TABLE IF NOT EXISTS lounge_benefit (
    benefit_id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id       UUID NOT NULL REFERENCES account(account_id),
    lounge_network   VARCHAR(50) NOT NULL,
    card_last4       VARCHAR(4) NOT NULL,
    total_uses       INTEGER NOT NULL DEFAULT 0,
    used_count       INTEGER NOT NULL DEFAULT 0,
    expiry_date      DATE NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                     CHECK (status IN ('ACTIVE', 'EXPIRED', 'SUSPENDED', 'CANCELLED')),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (account_id, lounge_network)
);

CREATE INDEX IF NOT EXISTS idx_benefit_account ON lounge_benefit(account_id);

-- 6. 贵宾室入场记录表
CREATE TABLE IF NOT EXISTS lounge_access_record (
    access_id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    benefit_id       UUID NOT NULL REFERENCES lounge_benefit(benefit_id),
    lounge_id        VARCHAR(50) NOT NULL,
    lounge_name      VARCHAR(200),
    airport_code     VARCHAR(3) NOT NULL,
    access_time      TIMESTAMP NOT NULL DEFAULT NOW(),
    guest_count      INTEGER NOT NULL DEFAULT 1,
    qr_code          VARCHAR(500) NOT NULL,
    qr_valid_from TIMESTAMP NOT NULL,
    qr_valid_until   TIMESTAMP NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'USED'
                     CHECK (status IN ('USED', 'EXPIRED', 'CANCELLED')),
    created_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_access_benefit ON lounge_access_record(benefit_id);
CREATE INDEX IF NOT EXISTS idx_access_time ON lounge_access_record(access_time DESC);
