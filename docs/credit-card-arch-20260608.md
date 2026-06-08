# 🏗️ 小宗信用卡架构评估 · 作者：小宗

**评估日期：2026年6月8日**
**设计来源：credit-card-design-20260608.md（小晶 · 智能贵宾室权益管理系统）**

---

## 一、设计来源与功能概述

### 1.1 设计背景

小晶基于 Elaine 东南亚信用卡研究（新加坡高级权益：机场贵宾室/旅行保险/Concierge），
设计了一套**智能贵宾室权益管理系统（Lounge Benefits Management System）**，
目标是为高端信用卡用户提供完整的贵宾室权益管理体验。

### 1.2 核心功能矩阵

| 功能 | 描述 | 优先级 |
|------|------|--------|
| F1 权益总览看板 | 展示剩余免费次数、有效期、关联卡号 | P0 |
| F2 贵宾室查找与导航 | 基于位置/航班查找附近贵宾室 | P2 |
| F3 电子通行证入场 | 动态 QR 码（JWT 签名，5 分钟刷新） | P0 |
| F4 权益到期提醒 | 7 天前推送提醒 | P1 |
| F5 带人费用预估 | 同行费用计算与支付 | P3 |

---

## 二、功能技术映射

### 2.1 微服务映射

| 功能 | AWS 服务方案 | 微服务归属 | 技术实现 |
|------|-------------|-----------|---------|
| F1 权益总览看板 | RDS PostgreSQL + Redis | **Lounge Benefits Service** | 权益规则引擎，按卡种配置 |
| F2 贵宾室查找与导航 | DynamoDB（高频查询）+ S3（静态数据） | **Lounge Benefits Service** | 位置服务 + Google Maps API |
| F3 电子通行证入场 | Lambda + API Gateway | **Lounge Benefits Service** | JWT 签名 QR 码生成/验证 |
| F4 权益到期提醒 | EventBridge + SNS | **Lounge Benefits Service** | 复用现有 Notification Service |
| F5 带人费用预估 | RDS + Lambda | **Lounge Benefits Service** | 计费规则引擎 |

### 2.2 数据层映射

| 数据模型 | AWS 存储方案 | 说明 |
|---------|-------------|------|
| LoungeBenefit | RDS PostgreSQL | 权益记录，关联 account_id |
| LoungeAccessRecord | RDS PostgreSQL | 入场记录，写入 Transaction Service |
| LoungeLocation | DynamoDB（Phase1）/ S3 + Athena（数据湖） | 贵宾室位置信息，高频查询 |
| 权益规则配置 | Parameter Store / AppConfig | 按卡种配置，动态更新 |

### 2.3 消息层映射

| 事件 | Kafka Topic | 消费者 |
|------|------------|--------|
| LOUNGE_ACCESS | credit-card.transaction.events | Transaction Service（记录流水） |
| LOUNGE_BENEFIT_CHANGED | credit-card.lounge.events（新增） | Notification Service（推送提醒） |
| LOUNGE_EXPIRY_WARNING | credit-card.lounge.events | EventBridge Scheduler（定时任务） |

### 2.4 API 网关映射

| 接口 | Kong 路由配置 | 限流策略 |
|------|-------------|---------|
| `/v1/lounge/benefits/*` | Lounge Service | 200 req/min |
| `/v1/lounge/search` | Lounge Service | 500 req/min（搜索场景） |
| `/v1/lounge/*/access-code` | Lounge Service | 100 req/min（核心功能） |
| `/v1/lounge/access/validate` | Lounge Service | 无限制（扫码端） |

---

## 三、架构改动点

### 3.1 新增服务

```
┌────────────────────────────────────────────────────────────────────────────┐
│  新增服务：Lounge Benefits Service（贵宾室权益服务）                       │
│  - 部署：ECS Fargate（2-5 pods，HPA on CPU > 70%）                        │
│  - 语言：Java/Spring Boot 3.x（同现有技术栈）                              │
│  - 依赖：Account Service（REST）、Transaction Service（Kafka）              │
└────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 数据库改动

**新增表（PostgreSQL）**：

```sql
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

-- 贵宾室入场记录表
CREATE TABLE lounge_access_record (
    access_id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    benefit_id       UUID NOT NULL REFERENCES lounge_benefit(benefit_id),
    lounge_id        VARCHAR(50) NOT NULL,
    lounge_name      VARCHAR(200),
    airport_code     CHAR(3) NOT NULL,
    access_time      TIMESTAMP NOT NULL DEFAULT NOW(),
    guest_count      INT NOT NULL DEFAULT 1,
    qr_code          VARCHAR(500) NOT NULL,         -- JWT token
    qr_valid_from    TIMESTAMP NOT NULL,
    qr_valid_until   TIMESTAMP NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'USED'
                     CHECK (status IN ('USED', 'EXPIRED', 'CANCELLED')),
    created_at       TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_access_benefit ON lounge_access_record(benefit_id);
CREATE INDEX idx_access_time ON lounge_access_record(access_time DESC);

-- 贵宾室位置表（DynamoDB 方案）
-- Table: LoungeLocation
-- PK: lounge_id (String)
-- GSI: airport_code-index (airport_code -> lounge_id)
```

### 3.3 消息队列改动

**新增 Topic**：

| Topic | 分区数 | 副本数 | 保留时间 | 说明 |
|-------|--------|--------|----------|------|
| credit-card.lounge.events | 8 | 3 | 30 天 | 贵宾室权益事件 |

### 3.4 安全改动

| 控制项 | 实现措施 |
|--------|---------|
| JWT QR 码签名 | HMAC-SHA256，KMS 管理密钥 |
| 卡号最小化 | 仅存储后 4 位，符合 PCI-DSS |
| 入口扫码校验 | 验签 + 时间窗口 + 权益状态 |

---

## 四、工时成本估算

### 4.1 开发工时估算（Phase 1 MVP）

| 模块 | 功能范围 | 工时（人天） | 说明 |
|------|---------|-------------|------|
| **Lounge Service 基础** | 微服务框架 + Account Service 集成 | 8 | Spring Boot + REST Client |
| **权益管理模块** | F1 权益看板 + 规则引擎 | 6 | 按卡种权益规则配置 |
| **QR 码生成模块** | F3 电子通行证（JWT 签名） | 5 | HMAC-SHA256 + 5min 刷新 |
| **QR 码验证模块** | 扫码端验证接口 | 4 | 验签 + 时间窗口 + 权益状态 |
| **提醒模块** | F4 权益到期提醒 | 3 | 复用 Notification Service |
| **数据库** | 新增 2 表 + DynamoDB 设计 | 3 | 权益表 + 入场记录表 |
| **Kong 路由配置** | `/v1/lounge/*` 路由 | 2 | JWT 插件 + 限流 |
| **单元测试 + 集成测试** | — | 6 | 覆盖率 80% |
| **合计** | **Phase 1 MVP** | **37 人天** | ~2 个月（2 人团队） |

### 4.2 后续阶段工时

| 阶段 | 功能范围 | 工时（人天） |
|------|---------|-------------|
| Phase 2 | F2 贵宾室查找与导航 + 航班联动 | 25 |
| Phase 3 | Priority Pass API 对接 + F5 带人费用 | 30 |

### 4.3 AWS 成本估算（年度）

| AWS 资源 | 规格 | 月费用（USD） | 年费用（USD） |
|---------|------|-------------|-------------|
| ECS Fargate（Lounge Service） | 2 vCPU, 4GB × 4 tasks | $180 | $2,160 |
| RDS PostgreSQL（新增表） | db.t3.medium Multi-AZ | $150 | $1,800 |
| DynamoDB（贵宾室位置） | 按需模式，10K RCU/WCU | $200 | $2,400 |
| API Gateway | 10M 请求/月 | $50 | $600 |
| Kafka（新增 Topic） | MSK 共享集群 | $50 | $600 |
| Lambda（QR 验证） | 按调用计费 | $20 | $240 |
| Data Transfer | — | $30 | $360 |
| **合计** | | **$680/月** | **$8,160/年** |

### 4.4 总成本估算

| 成本类型 | Phase 1 MVP | Phase 2 | Phase 3 | 合计 |
|---------|------------|--------|--------|------|
| 开发人力（$1,200/人天） | $44,400 | $30,000 | $36,000 | **$110,400** |
| AWS 年度成本 | $8,160 | +$2,000 | +$5,000 | **$15,160/年** |
| 第三方 API（可选） | $0 | $20,000 | $40,000 | $60,000/年 |
| **3 年 TCO（估算）** | — | — | — | **~$200K** |

**结论**：总成本约 **$200K（3 年）**，远低于 $500K 预算上限。

---

## 五、架构可行性评估

### 5.1 技术可行性

| 功能 | 可行性 | 说明 |
|------|--------|------|
| F1 权益总览看板 | ✅ 可行 | 权益规则引擎，配置化实现 |
| F2 贵宾室查找与导航 | ✅ 可行 | DynamoDB GSI + Google Maps API |
| F3 电子通行证入场 | ✅ 可行 | JWT 签名 + KMS，参考 SingPass 方案 |
| F4 权益到期提醒 | ✅ 可行 | EventBridge Scheduler + SNS 复用 |
| F5 带人费用预估 | ✅ 可行 | 计费规则引擎 + 支付集成 |

### 5.2 性能评估

| 链路 | 目标 | 估算 | 方案 |
|------|------|------|------|
| 权益查询 P99 | < 200ms | 150ms | Redis 缓存权益快照（TTL 5min） |
| QR 码生成 P99 | < 100ms | 80ms | Lambda + KMS 签名 |
| QR 码验证 P99 | < 100ms | 60ms | Lambda + 内存验签 |
| 贵宾室搜索 P99 | < 300ms | 250ms | DynamoDB GSI 查询 |

### 5.3 高可用评估

| 组件 | 可用性目标 | 方案 |
|------|-----------|------|
| Lounge Service | 99.9% | ECS Fargate Multi-AZ，HPA |
| RDS | 99.95% | Multi-AZ 主从 |
| DynamoDB | 99.999% | 按需容量模式 |
| API Gateway | 99.9% | AWS 原生 |

---

## 六、风险评估

### 6.1 技术风险

| 风险 | 级别 | 缓解措施 |
|------|------|---------|
| JWT QR 码被截图滥用 | 🔴 高 | 5 分钟刷新 + 时间窗口校验 + SingPass 同款方案 |
| Priority Pass API 对接复杂度 | 🟡 中 | Phase 1 使用本地数据，Phase 3 再对接 |
| 贵宾室信息不准确 | 🟡 中 | 第三方 API 实时同步 + 用户反馈机制 |
| 多卡权益冲突 | 🟡 中 | App 自动推荐最优卡，用户可手动切换 |

### 6.2 合规风险

| 风险 | 级别 | 缓解措施 |
|------|------|---------|
| MAS 合规（权益披露条款） | 🟡 中 | 权益说明页面设计，符合透明披露要求 |
| PDPA（位置数据保护） | 🟡 中 | 用户同意机制 + 数据最小化 |
| PCI-DSS | ✅ 低 | 仅存储后 4 位，无完整卡号 |

### 6.3 架构风险

| 风险 | 级别 | 缓解措施 |
|------|------|---------|
| 新服务与现有服务耦合 | 🟡 中 | Lounge Service 独立部署，事件驱动解耦 |
| Kafka Topic 增加复杂度 | 🟢 低 | 新增 lounge.events Topic，不影响现有 Topic |
| DynamoDB 成本不可预测 | 🟡 中 | 按需模式 + CloudWatch 监控 |

---

## 七、架构决策建议

### 7.1 建议采纳的设计

1. **JWT 签名 QR 码方案**：参考 SingPass 动态码，安全可靠
2. **Phase 1 MVP 不接入第三方 API**：降低初期复杂度，聚焦核心功能
3. **LoungeLocation 使用 DynamoDB**：高频查询场景，DynamoDB GSI 最优
4. **复用 Notification Service**：减少重复建设，加快交付

### 7.2 建议调整的设计

1. **DynamoDB 表设计**：建议增加 GSI（airport_code-index）用于机场搜索
2. **权益规则存储**：建议使用 AppConfig 而非数据库，提升热更新能力
3. **QR 码 TTL**：建议可配置化（默认 5 分钟，支持按卡种调整）

### 7.3 待确认事项

| 问题 | 负责人 | 优先级 |
|------|--------|--------|
| 贵宾室数据来源（自建 vs 采购） | 小晶 | 高 |
| Priority Pass API 对接时间线 | 小晶 | 中 |
| 带人费用支付集成方案 | 小晶 | 中 |

---

## 八、总结

| 项目 | 评估结果 |
|------|---------|
| **技术可行性** | ✅ 通过（所有功能可在 AWS 上实现） |
| **开发工时** | ~37 人天（Phase 1 MVP） |
| **3 年 TCO** | ~$200K（远低于 $500K 预算） |
| **性能目标** | ✅ 可达（授权 P99 < 100ms，权益查询 P99 < 200ms） |
| **高可用** | ✅ 满足（99.9%+ 可用性目标） |
| **主要风险** | QR 码截图滥用（已设计缓解措施） |

---

*本评估由小宗（系统架构师）于 2026年6月8日完成*
*设计参考：credit-card-design-20260608.md（小晶）*
*架构参考：architecture-design.md（小宗，v1.0）*