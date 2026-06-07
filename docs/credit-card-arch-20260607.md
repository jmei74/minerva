# 【🏗️ 小宗信用卡架构评估 · 作者：小宗】

**评估日期**：2026年6月7日
**设计来源**：credit-card-design-20260607.md（小晶）
**基础架构**：architecture-design.md（小宗，v1.0）

---

## 一、设计来源摘要

小晶基于 Elaine 的新加坡信用卡研究，设计了**智能分段返现引擎**，包含 5 个核心功能：
- **F1 实时返现预估**：消费完成 3 秒内展示预估返现
- **F2 月度进度追踪**：实时展示距低保门槛差额、各类别已用/剩余返现额度
- **F3 除外类目提醒**：除外类目交易主动推送不返现提示
- **F4 多卡切换对比**：多卡绑定后展示各卡返现状态
- **F5 返现历史报表**：按月/按类别汇总，支持导出

**Pre-transaction 查询**（创新点）：用户付款前查询某商户/某类消费能返多少，竞品均无此功能。

---

## 二、功能技术映射（AWS 架构落地）

### 2.1 新增服务：Cashback Service

现有架构（Account Svc / Transaction Svc / Billing Svc）**缺少返现域**，需新增 `Cashback Svc`。

```
现有架构                              新增 Cashback Svc
┌─────────────────────────────────┐   ┌──────────────────────────────────┐
│  Kong Gateway                   │   │  Kong Gateway                    │
│  Account Svc / Transaction Svc  │   │  Cashback Svc  ← 新增            │
│  Billing Svc / Credit Engine    │   │  ├─ Rule Engine（MCC映射/分段计算）│
│  Pricing Engine                 │   │  ├─ Progress Aggregator            │
│  PostgreSQL / Redis / Kafka     │   │  ├─ Exclusion Filter              │
└─────────────────────────────────┘   │  ├─ Card Rule Config (配置后台)   │
                                      │  └─ Notification Publisher        │
                                      │  PostgreSQL（RDS）/ Redis / SQS   │
                                      └──────────────────────────────────┘
```

### 2.2 功能 → 技术组件映射

| 功能 | 技术组件 | AWS 服务 | 复用现有？ |
|------|---------|---------|-----------|
| **F1 实时返现预估** | Cashback Svc + MCC 映射引擎 | ECS Fargate（服务）+ RDS PostgreSQL（规则库）+ Lambda（MCC 查表） | 部分复用 transaction 表 |
| **F2 月度进度追踪** | Progress Aggregator | RDS PostgreSQL（进度快照）+ Redis（实时缓存）+ EventBridge（每日聚合） | 新建 |
| **F3 除外类目提醒** | Exclusion Filter + Push Service | SNS → Mobile Push / SQS（异步解耦） | 复用 notification 基础设施 |
| **F4 多卡切换对比** | Card Binding 查询 | Account Svc（已建）+ Cashback Svc（查询聚合） | 复用 Account Svc |
| **F5 返现历史报表** | 报表生成服务 | S3（CSV 存储）+ Athena（查询）+ Lambda（生成） | 新建 |
| **Pre-transaction 查询** | 预估查询 API | API Gateway → Lambda → RDS（只读卡规则，无交易依赖） | 新建接口 |

### 2.3 数据模型扩展

**新增表（Cashback 域）**：

```sql
-- card_rule：卡种返现规则配置
CREATE TABLE card_rule (
    card_rule_id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    card_product   VARCHAR(50) NOT NULL,          -- "DBS_LIVEFRESH" / "UOB_EVOL"
    effective_date DATE NOT NULL,
    expiry_date    DATE,
    tiers          JSONB NOT NULL,                 -- [{category, rate, min_spend, cap, inclusions, exclusions}]
    status         VARCHAR(20) DEFAULT 'ACTIVE',
    created_at     TIMESTAMP DEFAULT NOW(),
    updated_at     TIMESTAMP DEFAULT NOW()
);

-- transaction_cashback：交易返现记录
CREATE TABLE transaction_cashback (
    cashback_id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    txn_id             UUID REFERENCES transaction(txn_id),
    account_id         UUID NOT NULL REFERENCES account(account_id),
    card_rule_id       UUID REFERENCES card_rule(card_rule_id),
    estimated_cashback DECIMAL(10,2) NOT NULL DEFAULT 0,
    category           VARCHAR(30),                -- "dining" / "online" / "transport"
    status             VARCHAR(20) DEFAULT 'pending'  -- "pending" / "confirmed" / "excluded"
                      CHECK (status IN ('pending','confirmed','excluded')),
    exclusion_reason   VARCHAR(200),
    created_at         TIMESTAMP DEFAULT NOW()
);

-- monthly_progress：月度进度快照
CREATE TABLE monthly_progress (
    progress_id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id           UUID NOT NULL REFERENCES account(account_id),
    card_rule_id         UUID REFERENCES card_rule(card_rule_id),
    bill_month           VARCHAR(6) NOT NULL,      -- YYYYMM
    total_spend          DECIMAL(15,2) DEFAULT 0,
    min_spend_threshold  DECIMAL(15,2) NOT NULL,
    distance_to_threshold DECIMAL(15,2) DEFAULT 0,
    category_progress    JSONB,                     -- [{category, spend, earned_cashback, remaining_cap}]
    updated_at           TIMESTAMP DEFAULT NOW(),
    CONSTRAINT uk_progress_account_month UNIQUE (account_id, bill_month)
);

CREATE INDEX idx_cashback_account ON transaction_cashback(account_id, created_at DESC);
CREATE INDEX idx_cashback_txn     ON transaction_cashback(txn_id) WHERE txn_id IS NOT NULL;
CREATE INDEX idx_progress_account ON monthly_progress(account_id, bill_month DESC);
```

### 2.4 消息流集成（Kafka）

```
Transaction Svc ──[publish]──> topic: credit-card.transaction.events
                                     │
                                     ├──[consumer]──> Cashback Svc
                                     │                ├─ F1 实时计算返现
                                     │                ├─ F3 除外类目判断
                                     │                └─ F2 更新月度进度
                                     │
                                     └──[consumer]──> Billing Svc（已有）
                                     └──[consumer]──> Account Svc（已有）
```

---

## 三、架构改动点

### 3.1 改动范围

| 改动类型 | 描述 | 影响范围 |
|---------|------|---------|
| **新增服务** | Cashback Svc（Rule Engine / Progress Aggregator / Exclusion Filter） | 新增 5-7 个 Pod（ECS Fargate） |
| **新增数据库表** | card_rule / transaction_cashback / monthly_progress（3 张表） | RDS PostgreSQL 新 schema |
| **新增 Kafka Consumer** | 消费 transaction.events，计算返现并写 DB | Cashback Svc |
| **新增 API 路由** | `/v1/cashback/*`（5 个接口） | Kong Gateway 路由配置 |
| **新增配置后台** | 运营人员配置卡种返现规则（规则热更新） | 内部管理台 |
| **S3 + Athena** | F5 报表导出 CSV | 新增 S3 bucket + Athena 角色 |

### 3.2 不改动的部分

- Account Svc / Transaction Svc / Billing Svc 核心逻辑不变
- 现有 PostgreSQL 表结构不变（transaction / account / bill 等）
- API Gateway 鉴权 / 限流 / 脱敏机制复用现有 Kong 配置
- Kafka topic `credit-card.transaction.events` 结构不变，Cashback Svc 仅作为 Consumer 接入

---

## 四、工时成本估算

### 4.1 开发工时（按 Phase）

| Phase | 功能 | 工时（人月） | 说明 |
|-------|------|------------|------|
| **Phase 1 MVP** | F1 实时返现预估 + F3 除外提醒 + 规则引擎核心 | **6 周（2 人）** | MCC 映射 + 除外规则 + 实时推送 |
| **Phase 2** | F2 月度进度追踪 + F5 历史报表 | **6 周（2 人）** | 聚合计算 + S3/Athena 报表 |
| **Phase 3** | F4 多卡切换 + 卡组织 API 对接 | **4 周（1-2 人）** | 多卡绑定查询 + Visa IntelliServer / MC SpendingPulse |
| **合计** | 全功能 | **16 周** | 约 4 个月 |

### 4.2 AWS 成本估算（年度，prod + non-prod）

| 组件 | 规格 | 月成本（USD） | 年成本（USD） |
|------|------|------------|-------------|
| **Cashback Svc**（ECS Fargate） | 5 tasks × 1 vCPU / 2GB | ~$400 | ~$4,800 |
| **RDS PostgreSQL**（db.t3.medium Multi-AZ） | 2 实例 | ~$250 | ~$3,000 |
| **ElastiCache Redis**（cache.r6g.large） | 1 主 2 从 | ~$300 | ~$3,600 |
| **SQS / SNS**（消息量估算 10M/月） | 按量付费 | ~$100 | ~$1,200 |
| **S3 + Athena**（报表存储与查询） | 存储 10GB + 查询 | ~$50 | ~$600 |
| **API Gateway**（新增 5 个接口） | 按调用计费 | ~$200 | ~$2,400 |
| **Lambda**（MCC 查表 / 报表生成） | 按调用计费 | ~$50 | ~$600 |
| **CloudWatch + X-Ray** | 日志 / 追踪 | ~$100 | ~$1,200 |
| **合计基础设施** | | **~$1,450/月** | **~$17,400/年** |

### 4.3 总拥有成本（TCO，12 个月）

| 成本项 | 金额（USD） |
|-------|-----------|
| AWS 基础设施（12 个月） | ~$17,400 |
| 人员开发成本（4 人月 × $15K/月） | ~$60,000 |
| 第三方 MCC 数据（如 Swiviel/客乐芙） | ~$5,000/年 |
| **TCO 合计** | **~$82,400** |

> ✅ **远低于 $500K 预算上限**，节省约 83%。

---

## 五、风险评估

| 风险 | 级别 | 缓解措施 |
|------|------|---------|
| **MCC 码映射不准确** | 🟡 中 | Phase 1 MVP 用银行内部交易数据的 MCC 字段，不依赖第三方；后续引入 Swiviel/客乐芙做商户名补充映射 |
| **规则配置错误** | 🔴 高 | 上线前双人审核规则配置；灰度发布（先对 1% 用户生效）；Kong 层增加配置变更审批流 |
| **Kafka 消费延迟导致返现预估滞后** | 🟡 中 | transaction.events 消费延迟 < 5s（Kafka 本身延迟低）；F1 评估可降级为"预估中"状态 |
| **Pre-transaction 查询性能** | 🟡 中 | API Gateway + Lambda + RDS 只读场景，P99 目标 < 200ms；加 Redis 缓存 MCC 映射表（TTL 1h） |
| **卡组织 API 对接（Phase 3）** | 🟡 中 | Phase 1/2 MVP 不依赖卡组织 API；Phase 3 对接 Visa IntelliServer / MC SpendingPulse 作为可选项 |
| **与账务实际返现结果不一致引发客诉** | 🟡 中 | 前端明确展示"预估"标识；注明"最终以银行账单为准"；建立用户反馈机制 |
| **除类目清单不完整** | 🟡 中 | 建立用户反馈入口；每月更新除外类目 MCC 清单；参考 MAS 合规要求 |

---

## 六、结论与建议

**选 A（实施）不选 B（不实施）**，原因：

1. **预算充足**：~$82K TCO，远低于 $500K 上限，财务可行性高。
2. **架构可落地**：基于现有 PostgreSQL / Redis / Kafka 基础设施，新增 Cashback Svc 侵入性小，不破坏现有核心链路。
3. **差异化强**：Pre-transaction 查询是新加坡市场空白，是核心竞争壁垒，值得优先实现。
4. **风险可控**：Phase 1 MVP 仅依赖内部交易数据，无需卡组织 API，降低初期集成风险。

### 实施建议优先级

| 优先级 | 功能 | 理由 |
|--------|------|------|
| **P0** | F1 实时返现预估 + F3 除外提醒 | MVP 核心，6 周交付，验证商业模式 |
| **P1** | F2 月度进度追踪 | 提升用户粘性，复用 transaction 表数据 |
| **P2** | F5 历史报表 | 数据透明度，S3/Athena 方案成本低 |
| **P3** | F4 多卡切换 | 需 Account Svc 支持多卡绑定基础能力 |
| **P4** | 卡组织 API 对接 | Phase 3 可选项，非 MVP 必需 |

---

*本评估由小宗（系统架构师）于 2026 年 6 月 7 日完成*
*参考：小晶 credit-card-design-20260607.md，小宗 architecture-design.md v1.0*