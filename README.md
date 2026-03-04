# Mini Ontology Engine

**版本：** 1.0-SNAPSHOT  
**语言：** Java 17  
**构建：** Maven 3.9+  
**核心框架：** Drools 7.74.1.Final

---

## 一、项目概述

Mini Ontology Engine 是一个**轻量级本体规则引擎**，模拟 Palantir Ontology 的核心理念：

> 将业务实体（Object）、规则推理（Rule Engine）、后续动作（Action Engine）三层解耦，
> 通过声明式规则驱动业务决策，而非硬编码 if-else。

当前业务场景：**养老资格自动审查系统**。输入人员信息，规则引擎自动判断是否满足养老条件，并触发对应通知动作。

---

## 二、架构总览

```
┌─────────────────────────────────────────────────────┐
│                      MainApp                        │
│                   （程序入口）                        │
└───────────┬─────────────────────┬───────────────────┘
            │                     │
            ▼                     ▼
┌───────────────────┐   ┌─────────────────────┐
│    RuleEngine     │   │    ActionEngine      │
│  (Drools 引擎)    │   │   (动作分发器)        │
│                   │   │                     │
│  ┌─────────────┐  │   │  ┌───────────────┐  │
│  │ KieSession  │  │   │  │ NotifyAction  │  │
│  │  (工作内存) │  │   │  │  (通知动作)   │  │
│  └─────────────┘  │   │  └───────────────┘  │
└───────────────────┘   └─────────────────────┘
            │
            ▼
┌───────────────────────────────┐
│       eligibility.drl         │
│         (规则文件)             │
│  ● pension                    │
│  ● pension-not-eligible       │
└───────────────────────────────┘
            │
            ▼
┌───────────────────────────────┐
│       OntologyObject          │
│        (本体基类)              │
│  ├── Person                   │
│  └── EligibilityResult        │
└───────────────────────────────┘
```

---

## 三、模块说明

### 3.1 数据层 — 本体对象

#### `OntologyObject`（抽象基类）
所有业务实体的基类，提供统一的数据结构：

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | String | 对象唯一标识 |
| `type` | String | 对象类型名称 |
| `state` | String | 当前状态（如 `ACTIVE` / `NEW` / `DONE`） |
| `attrs` | `Map<String,Object>` | 动态属性键值对，支持任意扩展字段 |

```java
public abstract class OntologyObject {
    private String id;
    private String type;
    private String state;
    private Map<String,Object> attrs = new HashMap<>();
    // getId / getType / setState / getState / setAttr / getAttr
}
```

---

#### `Person`（业务实体）
继承 `OntologyObject`，表示待审查人员。

| 属性 | 说明 |
|------|------|
| `age` | 年龄 |
| `years` | 工龄（参保年数） |
| 初始状态 | `ACTIVE` |

```java
Person p = new Person("P1", 65, 20);
// id="P1", age=65, years=20, state=ACTIVE
```

---

#### `EligibilityResult`（审查结果）
继承 `OntologyObject`，由规则引擎触发后自动生成，代表一次资格审查结论。

| 属性 | 说明 |
|------|------|
| id 命名规则 | `{personId}-ELIG`（通过）/ `{personId}-NOT-ELIG`（拒绝） |
| 初始状态 | `NEW` |
| 动作执行后状态 | `DONE` |

---

### 3.2 规则层 — Drools 规则引擎

#### `RuleEngine`
封装 Drools `KieSession`，负责加载规则并执行推理。

```java
RuleEngine re = new RuleEngine();
re.session().insert(person);   // 将对象放入工作内存
re.fire();                     // 触发所有匹配规则
```

内部通过 `META-INF/kmodule.xml` 定位规则集：

```xml
<kmodule xmlns="http://www.drools.org/xsd/kmodule">
  <kbase name="kbase" packages="rules">
    <ksession name="ksession"/>
  </kbase>
</kmodule>
```

---

#### `eligibility.drl`（规则文件）

**规则一：`pension`（满足养老条件）**

```
条件：age >= 60 AND years >= 15
动作：插入 EligibilityResult("{id}-ELIG")
```

**规则二：`pension-not-eligible`（不满足养老条件）**

```
条件：age < 60 OR years < 15
动作：插入 EligibilityResult("{id}-NOT-ELIG")
```

---

### 3.3 动作层 — Action Engine

#### `OntologyAction`（接口）
所有动作的统一契约：

```java
public interface OntologyAction {
    boolean supports(OntologyObject o);  // 是否处理该对象
    void run(OntologyObject o);          // 执行动作
}
```

#### `ActionEngine`（动作分发器）
维护一组 `OntologyAction`，遍历执行所有支持该对象的动作。支持注册多个动作，天然具备扩展性。

```java
ActionEngine ae = new ActionEngine();
ae.add(new NotifyAction());   // 可继续 add 更多动作
ae.fire(eligibilityResult);
```

#### `NotifyAction`（通知动作）
当前唯一的动作实现，处理 `EligibilityResult` 对象：

| 结果类型 | 输出 | 状态变更 |
|----------|------|----------|
| `*-ELIG` | `ACTION notify -> APPROVED: ...` | `NEW → DONE` |
| `*-NOT-ELIG` | `ACTION notify -> REJECTED: ...` | `NEW → DONE` |

---

## 四、执行流程

```
1. 创建 Person 对象（携带 age / years 属性）
        ↓
2. 插入到 RuleEngine 的 KieSession 工作内存
        ↓
3. re.fire() — Drools 匹配所有规则
        ↓
   ┌────────────────────┬──────────────────────────┐
   │ age≥60 & years≥15  │  age<60 OR years<15      │
   │ → 插入 *-ELIG      │  → 插入 *-NOT-ELIG        │
   └────────────────────┴──────────────────────────┘
        ↓
4. 遍历 session 中所有 EligibilityResult
        ↓
5. ActionEngine.fire() — 触发 NotifyAction
        ↓
   ┌────────────────────┬──────────────────────────┐
   │ *-ELIG             │  *-NOT-ELIG              │
   │ → 打印 APPROVED    │  → 打印 REJECTED          │
   │ → state = DONE     │  → state = DONE           │
   └────────────────────┴──────────────────────────┘
```

---

## 五、目录结构

```
mini-ontology-engine/
├── pom.xml                          # Maven 构建配置
├── README.md                        # 本文档
├── TROUBLESHOOTING_REPORT.md        # 排障报告
└── src/main/
    ├── java/com/example/ontology/
    │   ├── OntologyObject.java      # 本体基类（抽象）
    │   ├── Person.java              # 人员实体
    │   ├── EligibilityResult.java   # 审查结果实体
    │   ├── RuleEngine.java          # Drools 规则引擎封装
    │   ├── OntologyAction.java      # 动作接口
    │   ├── ActionEngine.java        # 动作分发引擎
    │   ├── NotifyAction.java        # 通知动作实现
    │   └── MainApp.java             # 程序入口
    └── resources/
        ├── META-INF/
        │   └── kmodule.xml          # Drools 模块配置
        └── rules/
            └── eligibility.drl      # 养老资格规则
```

---

## 六、构建与运行

### 构建 Fat Jar
```bash
mvn clean package
```

### 直接运行（IDE）
在 VS Code 中打开 `MainApp.java`，点击 **Run** 按钮。

### 命令行运行
```bash
java -jar target/mini-ontology-engine-1.0-SNAPSHOT.jar
```

---

## 七、示例输出

```
RULE hit for P1
RULE not eligible for P2 (age=55, years=20)
ACTION notify -> APPROVED: EligibilityResult(P1-ELIG) state=NEW {}
ACTION notify -> REJECTED: EligibilityResult(P2-NOT-ELIG) state=NEW {}
```

---

## 八、扩展指引

| 扩展点 | 方式 |
|--------|------|
| 新增业务实体 | 继承 `OntologyObject`，添加属性 |
| 新增审查规则 | 在 `eligibility.drl` 中添加新 `rule` 块 |
| 新增后处理动作 | 实现 `OntologyAction` 接口，调用 `ae.add()` 注册 |
| 多规则文件 | 在 `kmodule.xml` 的 `kbase` 中扩展 `packages` |

---

## 九、依赖清单

| 依赖 | 版本 | 用途 |
|------|------|------|
| `drools-core` | 7.74.1.Final | Drools 核心运行时 |
| `drools-compiler` | 7.74.1.Final | DRL 规则编译器 |
| `drools-mvel` | 7.74.1.Final | MVEL 表达式引擎 |
| `kie-api` | 7.74.1.Final | KIE 统一 API |
| `slf4j-simple` | 1.7.36 | 日志实现 |
| `jgrapht-core` | 1.5.2 | 图结构支持 |
| `caffeine` | 3.1.8 | 本地缓存 |
| `maven-shade-plugin` | 3.5.1 | Fat Jar 打包 |
