-- Flyway migration: Create lounge tables
-- 对应架构文档: §3.2 数据库改动
-- Phase 1 MVP贵宾室权益表

-- 贵宾室权益表
CREATE TABLE lounge_benefit (
    benefit_id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id       UUID NOT NULL REFERENCES account(account_id),
    lounge_network   VARCHAR(50) NOT NULL,          -- Priority_Pass/LoungeKey/MC_Travel_Pass
    card_last4       CHAR(4) NOT NULL,
    total_uses       INT NOT NULL DEFAULT 0,
    used_count       INT NOT NULL DEFAULT 0,
    expiry_date      DATE NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_benefit_account ON lounge_benefit(account_id);
CREATE UNIQUE INDEX idx_benefit_account_network ON lounge_benefit(account_id, lounge_network);

COMMENT ON TABLE lounge_benefit IS '贵宾室权益表 - 存储账户的贵宾室权益信息';
COMMENT ON COLUMN lounge_benefit.lounge_network IS '贵宾室网络: Priority_Pass/LoungeKey/MC_Travel_Pass';
COMMENT ON COLUMN lounge_benefit.card_last4 IS '关联卡号后4位（PCI-DSS §3.3 卡号最小化）';

-- 贵宾室入场记录表
CREATE TABLE lounge_access_record (
    access_id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    benefit_id       UUID NOT NULL REFERENCES lounge_benefit(benefit_id),
    lounge_id        VARCHAR(50) NOT NULL,
    lounge_name      VARCHAR(200),
    airport_code     CHAR(3) NOT NULL,
    access_time      TIMESTAMP NOT NULL DEFAULT NOW(),
    guest_count      INT NOT NULL DEFAULT 1,
    qr_code          VARCHAR(500) NOT NULL,         -- JWT token for QR code
    qr_valid_from    TIMESTAMP NOT NULL,
    qr_valid_until   TIMESTAMP NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'USED'
                     CHECK (status IN ('USED', 'EXPIRED', 'CANCELLED')),
    created_at       TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_access_benefit ON lounge_access_record(benefit_id);
CREATE INDEX idx_access_time ON lounge_access_record(access_time DESC);

COMMENT ON TABLE lounge_access_record IS '贵宾室入场记录表 - 记录每次入场详情';
COMMENT ON COLUMN lounge_access_record.qr_code IS 'JWT签名QR码（HMAC-SHA256）';