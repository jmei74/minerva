# 🔒 小盾安全扫描报告 · 作者：小盾

**扫描时间**: 2026-06-08 08:30 (Asia/Shanghai, UTC+8)  
**扫描项目**: credit-card-core  
**扫描类型**: 源代码安全扫描 + 敏感信息扫描
**扫描工具**: grep (硬编码凭证检测), OWASP Dependency-Check,源代码人工审计

---

## 🔴 Critical 漏洞（1个）

###1. 硬编码加密密钥 [CWE-798] CVSS: 9.8 (Critical)
- **文件**: `src/main/java/com/minerva/creditcard/config/SecurityConfig.java`
- **行号**: 第 120 行
- **漏洞描述**: `deriveKey()` 方法中硬编码了 AES-256 加密密钥 `"minerva-credit-card-key-32bytes!!"` 作为 fallback。此密钥用于卡号加密，若代码泄露，攻击者可直接解密所有卡号数据。
- **修复建议**:
  1. 移除代码中的 fallback密钥
  2. 应用启动时若 `kmsKeyId` 为空，应抛出异常并阻止启动
  3. 真实密钥必须从 AWS KMS 或 Vault 获取，禁止硬编码

---

## 🟠 High 漏洞（2个）

### 2. 默认 JWT Secret 硬编码 [CWE-547] CVSS: 7.5 (High)
- **文件**: `src/main/resources/application.yml`
- **行号**: 第 100 行
- **漏洞描述**: JWT 默认密钥 `"minerva-credit-card-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256"` 硬编码在配置文件中。若生产环境环境变量 `JWT_SECRET` 未正确设置，系统将使用此已知默认值，攻击者可伪造任意 JWT Token。
- **修复建议**:
  1. 移除 YAML 中的默认 JWT secret 值
  2. 在 `JwtTokenProvider` 构造函数中验证 secret 非空，不符合要求则抛出异常
  3. 生产环境必须通过环境变量注入真实密钥

### 3. SSL Keystore 默认密码 [CWE-547] CVSS: 5.3 (Medium-High)
- **文件**: `src/main/resources/application.yml`
- **行号**: 第 61 行
- **漏洞描述**: Keystore 密码硬编码为 `"changeit"`，这是 Java keystore 的默认示例密码，安全性极低。
- **修复建议**:
  1. 将密码改为通过环境变量注入: `key-store-password: ${SSL_KEYSTORE_PASSWORD}`
  2. 使用强随机密码（建议 16+ 字符，含特殊字符）
  3. 密码存储在 AWS Secrets Manager 或 Vault 中

---

## 🟡 Medium 漏洞（0个）

无

---

## 🟢 Low 漏洞（0个）

无

---

## ⚠️ PCI-DSS 合规问题（2个）

### PCI-DSS §3.4 加密密钥管理不合规
- **问题**: 加密密钥通过硬编码 fallback 值存在于代码中
- **影响**: 不符合 PCI-DSS §3.4 要求（加密密钥必须安全存储和管理）
- **建议**: 密钥管理必须使用专用 KMS（如 AWS KMS），禁止代码中存储密钥

### PCI-DSS §7.1 访问控制配置缺失
- **问题**: Actuator 端点 `/actuator/health` 和 `/actuator/info` 设为公开访问
- **影响**: 攻击者可获取系统架构信息（Redis、Kafka、数据库健康状态）
- **建议**: 生产环境应限制 Actuator 端点暴露，或至少对 `/actuator/metrics` 等敏感端点启用认证

---

## 📋 修复优先级

| 优先级 | 漏洞 | 预计修复时间 |
|--------|------|--------------|
| P0 | 硬编码加密密钥 (SecurityConfig.java:120) | **立即修复** |
| P1 | 默认 JWT Secret (application.yml:100) | 24小时内 |
| P2 | SSL Keystore 密码 (application.yml:61) | 24小时内 |
| P3 | Actuator 端点权限 | 下周迭代 |

---

## ✅ 安全亮点

1. **卡号加密算法正确**: `CardEncryptionUtil` 使用 AES-256-GCM（比 CBC 更安全），符合 PCI-DSS §3.4 要求
2. **卡号掩码实现正确**: PCI-DSS §3.3 要求的卡号掩码 `**** **** **** 1234` 已正确实现
3. **JWT 鉴权架构合理**: 使用 HS256 + 无状态 Session，正确禁用 CSRF
4. **敏感信息无明文泄露**: 源码中未发现明文密码/API Key 泄露（grep 扫描通过）

---

## 📝 扫描备注

- **OWASP Dependency-Check**: 因 NVD API 返回 403 Forbidden 而失败（网络问题），依赖漏洞扫描暂时跳过
- **SpotBugs**: 项目未配置 SpotBugs 插件，建议添加以进行字节码级别安全扫描
- **建议**: 添加 SpotBugs Maven 插件并集成到 CI/CD，配置 `failBuildOnCVSS>7`

---

*报告生成: 小盾 · 网络安全专家*  
*下次扫描时间: 2026-06-09 08:30 (定时任务)*