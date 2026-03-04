# Mini Ontology Engine

**版本：** 1.0-SNAPSHOT  
**语言：** Java 17  
**构建：** Maven 3.9+  
**核心框架：** Drools 7.74.1.Final · Spring Boot 3.x

---

## 一、项目概述

Mini Ontology Engine 是一个**轻量级本体规则引擎**，模拟 Palantir Ontology 的核心理念：

> 将业务实体（Object）、规则推理（Rule Engine）、后续动作（Action Engine）三层解耦，
> 通过声明式规则驱动业务决策，而非硬编码 if-else。

当前业务场景：**深圳市失业保险金申领资格自动审查系统**。输入申请人全量信息，规则引擎自动判断是否满足申领条件（四情形），并通过 REST API + 交互式 Web UI 对外暴露。

---

## 二、架构总览

```
┌─────────────────────────────────────────────────────────────────┐
│  Browser  index.html  (单页应用，Material Design 3)              │
│     └── POST /api/review  ─────────────────────────────────────►│
└────────────────────────────────────────────────────────────────►│
                                                                  │
┌─────────────────────────────────────────────────────────────────┤
│  Spring Boot 3.x                                                 │
│  ReviewController  (/api/review  GET + POST)                     │
│       └── ReviewService                                          │
│             ├── 构建 Facts（Policy / Person / InsuranceRecord…） │
│             └── OntologyRuleVersionManager                       │
│                   └── RuleEngine  (Drools 7 KieSession)          │
│                         └── unemployment.drl  v1.0               │
│                               ├── Scenario-1 GeneralEmployee     │
│                               ├── Scenario-2 ShortTenure         │
│                               ├── Scenario-3 NearRetirement      │
│                               ├── Scenario-4 IndividualBusiness  │
│                               └── Scenario-reject (兜底)         │
└─────────────────────────────────────────────────────────────────┘
```

---

## 三、核心实体（本体层）

所有实体继承 `OntologyObject`（id / type / state / attrs 动态键值对）。

| 实体类 | 职责 |
|--------|------|
| `UnemployedPerson` | 申领人：姓名、户籍地、出生日期、性别/岗位；v2 支持延迟退休动态计算 |
| `InsuranceRecord` | 单段参保记录：缴费月数、剩余月数、停保日期、停保原因（枚举） |
| `UnemploymentTimeline` | 多段参保聚合：自动取最晚停保记录为 `latestRecord`，累加 `totalMonths` |
| `UnemploymentPolicy` | 政策参数单例：最低缴费月数、临近退休年限、2025 延迟退休动态计算算法 |
| `EmployerUnit` | 用人单位：社会信用代码、单位类型（10种枚举，含个体工商户） |
| `UnemploymentRegistration` | 失业登记：是否有效、登记日期 |
| `TerminationMaterial` | 劳动关系终止材料：类型（6种枚举）、终止日期、出具单位 |
| `BankAccount` | 收款账户：银行、账号、户名 |
| `EligibilityResult` | 规则引擎输出结论：审查码、说明文字、拒绝原因 |

---

## 四、规则层（unemployment.drl v1.0）

四情形 + 一兜底，全部无硬编码数字，所有阈值来自 `$policy`：

| 规则名 | 核心条件 | 结论码 |
|--------|----------|--------|
| `Scenario-1 GeneralEmployee` | 户籍深圳、非临近退休、非个体工商户、累计缴费 ≥ `minContributionMonths`、非自愿停保 | `ELIGIBLE_SCENARIO_1` |
| `Scenario-2 ShortTenure` | 同上但累计缴费 < `minContributionMonths`，剩余月数 > 0 | `ELIGIBLE_SCENARIO_2` |
| `Scenario-3 NearRetirement` | 户籍深圳、`isNearRetirement($policy)==true`、非个体工商户、非自愿停保 | `ELIGIBLE_SCENARIO_3` |
| `Scenario-4 IndividualBusiness` | 户籍深圳、个体工商户、停产/注销/破产类停保 | `ELIGIBLE_SCENARIO_4` |
| `Scenario-reject` | 以上四情形均未命中（`not EligibilityResult`） | `NOT_ELIGIBLE` |

### 临近退休 — 2025 延迟退休动态计算

`UnemployedPerson.isNearRetirement(policy)` 委托给 `UnemploymentPolicy.isNearRetirement(birthday, gender, referenceDate)`，无需 DRL 内任何年龄硬编码：

| gender 值 | 原退休年龄 | 起算生日 | 延迟步长 | 上限 |
|-----------|-----------|---------|---------|------|
| `male` | 60 岁（720月） | 1965-01 | 每 4 月延 1 月 | 65 岁（780月） |
| `female55` | 55 岁（660月） | 1970-01 | 每 4 月延 1 月 | 58 岁（696月） |
| `female50` | 50 岁（600月） | 1975-01 | 每 2 月延 1 月 | 55 岁（660月） |

---

## 五、Web 层

### REST API

| Method | Path | 说明 |
|--------|------|------|
| `GET` | `/api/review` | 运行内置示例数据集，返回四情形演示结果 |
| `POST` | `/api/review` | 提交自定义申请人，返回三阶段审查结果 |

### POST 请求体（`ApplicantRequest`）

```jsonc
{
  // 申请人
  "id": "440101199001011234",
  "name": "张三",
  "domicileShenzhen": true,
  "birthday": "1966-06-15",          // 可选；填写后由系统动态计算是否临近退休
  "gender": "male",                   // "male" | "female55" | "female50"
  "nearRetirement": false,            // 兜底开关，birthday 存在时忽略

  // 多段参保记录（优先使用）
  "insuranceRecords": [
    {
      "paidMonths": 36,
      "remainingMonths": 18,
      "lastInsuredShenzhen": true,
      "stopReason": "CONTRACT_EXPIRED",
      "stopDate": "2024-01-31"
    }
  ],

  // 政策参数（可选，不传用默认值）
  "policy": {
    "minContributionMonths": 12,
    "nearRetirementYears": 5
  },

  // 用人单位
  "unitName": "ABC科技有限公司",
  "unitType": "ENTERPRISE",

  // 失业登记
  "registered": true,
  "registrationDate": "2024-03-01",

  // 劳动关系终止材料
  "materialType": "CONTRACT_TERMINATION_NOTICE",
  "terminationDate": "2024-02-28",

  // 银行账户
  "bankName": "中国建设银行",
  "accountNo": "6222021234567890",
  "accountName": "张三"
}
```

### 响应体（`ReviewResponse`）

```jsonc
{
  "ruleVersion": "1.0",
  "applicants": [ /* Phase-1 录入快照 */ ],
  "engineLog":  [ /* Phase-2 Drools stdout 逐行 */ ],
  "results": [
    {
      "applicantId": "440101199001011234",
      "applicantName": "张三",
      "approved": true,
      "scenario": "Scenario-1 GeneralEmployee",
      "rejectReason": null,
      "bankName": "中国建设银行",
      "accountNo": "6222021234567890"
    }
  ]
}
```

---

## 六、单元测试

`UnemploymentEligibilityTest`（JUnit 5）覆盖 13 个场景：

| 测试方法 | 预期结论 |
|----------|---------|
| `scenario1_approved` | 情形一通过 |
| `scenario1_rejected_voluntaryResign` | 主动辞职 → 拒绝 |
| `scenario2_approved` | 情形二（缴费不足有剩余期限）通过 |
| `scenario3_approved` | 情形三（临近退休）通过 |
| `scenario3_rejected_noRegistration` | 未办失业登记 → 拒绝 |
| `scenario4_approved` | 情形四（个体工商户）通过 |
| `rejected_lastInsuredNotShenzhen` | 最后参保地非深圳 → 拒绝 |
| … 其余 6 个 | 各类边界/拒绝场景 |

运行：

```bash
mvn test
```

---

## 七、目录结构

```
mini-ontology-engine/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/com/example/ontology/
    │   │   ├── OntologyApplication.java          # Spring Boot 启动类
    │   │   ├── MainApp.java                       # 独立命令行入口（可选）
    │   │   ├── model/
    │   │   │   ├── OntologyObject.java            # 本体基类
    │   │   │   ├── UnemployedPerson.java          # 申领人（v2 含 birthday/gender）
    │   │   │   ├── InsuranceRecord.java           # 单段参保记录
    │   │   │   ├── UnemploymentTimeline.java      # 多段参保聚合
    │   │   │   ├── UnemploymentPolicy.java        # 政策参数 + 延迟退休算法
    │   │   │   ├── EmployerUnit.java              # 用人单位（10种类型枚举）
    │   │   │   ├── UnemploymentRegistration.java  # 失业登记
    │   │   │   ├── TerminationMaterial.java       # 劳动关系终止材料
    │   │   │   ├── BankAccount.java               # 收款账户
    │   │   │   └── EligibilityResult.java         # 审查结论
    │   │   ├── engine/
    │   │   │   ├── RuleEngine.java                # Drools KieSession 封装
    │   │   │   └── OntologyRuleVersionManager.java # 规则版本管理（缓存）
    │   │   ├── action/
    │   │   │   ├── OntologyAction.java            # 动作接口
    │   │   │   ├── ActionEngine.java              # 动作分发器
    │   │   │   └── NotifyAction.java              # 通知动作实现
    │   │   └── web/
    │   │       ├── controller/ReviewController.java
    │   │       ├── service/ReviewService.java
    │   │       └── dto/
    │   │           ├── ApplicantRequest.java      # POST 请求体（含 InsuranceRecordItem/PolicyParams）
    │   │           ├── ApplicantSnapshot.java     # Phase-1 录入快照
    │   │           ├── EligibilityResultDto.java  # Phase-3 结论 DTO
    │   │           └── ReviewResponse.java        # 三阶段响应体
    │   └── resources/
    │       ├── application.yml
    │       ├── META-INF/kmodule.xml               # Drools 模块配置（unemploymentSessionV1）
    │       ├── ontology/rules/v1.0/
    │       │   └── unemployment.drl               # 四情形规则 + 兜底拒绝
    │       └── static/index.html                  # 交互式 Web UI（单页应用）
    └── test/
        └── java/com/example/ontology/
            └── UnemploymentEligibilityTest.java   # 13 个场景单元测试
```

---

## 八、构建与运行

```bash
# 构建（跳过测试）
mvn clean package -DskipTests

# 运行所有单元测试
mvn test

# 启动 Spring Boot（端口 8080）
mvn spring-boot:run
# 访问 Web UI：http://localhost:8080
# API：        http://localhost:8080/api/review
```

---

## 九、依赖清单

| 依赖 | 版本 | 用途 |
|------|------|------|
| `spring-boot-starter-web` | 3.x | REST API + 静态资源服务 |
| `drools-core` | 7.74.1.Final | Drools 核心运行时 |
| `drools-compiler` | 7.74.1.Final | DRL 规则编译器 |
| `drools-mvel` | 7.74.1.Final | MVEL 表达式引擎 |
| `kie-api` | 7.74.1.Final | KIE 统一 API |
| `junit-jupiter` | 5.x | 单元测试框架 |
| `slf4j-simple` | 1.7.36 | 日志实现 |
| `jgrapht-core` | 1.5.2 | 图结构支持（Drools 依赖） |
| `caffeine` | 3.1.8 | 规则引擎本地缓存 |
| `maven-shade-plugin` | 3.5.1 | Fat Jar 打包 |
