# Drools DRL 规则文件版本管理
**核心目标**：在本体论设计框架下，实现 DRL 规则的**版本可控、环境隔离、追溯可审计、迭代无风险**，同时保持与 Palantir 本体论「规则可版本化」的设计对齐。

---

## 一、DRL 版本管理的核心原则（贴合本体论设计）
DRL 作为本体论中「规则层」的核心载体，版本管理需遵循以下原则：
1. **语义化版本**：版本号需体现「业务规则变更」而非「技术迭代」（如 `v2.1.0` 对应「缴费月数阈值从12个月调整为10个月」）；
2. **实体-规则解耦**：版本变更仅影响规则层，不破坏实体层的稳定性（符合本体论「数据与逻辑分离」的核心）；
3. **环境隔离**：开发/测试/生产环境使用不同版本的 DRL，避免规则变更直接影响生产；
4. **可追溯**：每一次规则版本变更需关联「业务政策依据」（如对应《失业保险金申领操作规程》2026修订版）。

---

## 二、DRL 版本管理的三层实现方案
### 2.1 基础层：代码仓库级版本管理（核心）
这是最基础且必须落地的版本管理方式，适配 Maven/Gradle 工程体系，与本体论项目的工程化落地完全兼容。

#### 1. 目录结构设计（按版本/场景分层）
遵循「本体实体不变，规则按版本/场景拆分」的原则，目录结构示例：
```
src/main/resources/
├── ontology/                # 本体论核心目录
│   ├── rules/               # 规则根目录
│   │   ├── v1.0/            # 规则版本1.0（初始版本，缴费≥12个月）
│   │   │   ├── unemployment-general.drl  # 通用情形规则
│   │   │   ├── unemployment-individual.drl # 个体工商户规则
│   │   │   └── kmodule.xml               # 版本1.0的Kie模块配置
│   │   ├── v2.0/            # 规则版本2.0（调整缴费≥10个月）
│   │   │   ├── unemployment-general.drl
│   │   │   ├── unemployment-individual.drl
│   │   │   └── kmodule.xml
│   │   └── common/          # 通用规则片段（所有版本共享）
│   │       └── common-utils.drl  # 辅助函数、枚举定义
│   └── entities/            # 本体实体类（无版本，保持稳定）
```

#### 2. KieModule 版本配置（kmodule.xml）
每个版本的 DRL 对应独立的 `kmodule.xml`，通过 `kbase`/`ksession` 区分版本，示例（v1.0 的 kmodule.xml）：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<kmodule xmlns="http://www.drools.org/xsd/kmodule">
    <!-- 本体规则v1.0的kbase，对应业务版本1.0 -->
    <kbase name="unemploymentKBaseV1" packages="ontology.rules.v1.0,ontology.rules.common">
        <ksession name="unemploymentKSessionV1" type="stateful"/>
    </kbase>
</kmodule>
```
- `name`：明确包含版本号（如 `unemploymentKBaseV1`），便于代码中指定版本；
- `packages`：关联当前版本的 DRL 目录 + 通用规则目录，避免重复代码；
- 不同版本的 `kmodule.xml` 放在对应版本目录下，编译时通过 Maven 配置指定加载路径。

#### 3. 代码仓库版本控制（Git）
- **分支策略**：
  - `main` 分支：存放生产环境稳定的规则版本；
  - `develop` 分支：开发中的规则版本；
  - `feature/rule-v2.0`：新增规则版本的特性分支（如「调整缴费月数」）；
- **提交规范**：每次提交 DRL 需注明「业务变更原因 + 政策依据」，示例：
  ```
  feat(rules-v2.0): 调整失业保险金缴费月数阈值为10个月
  政策依据：《深圳市失业保险金申领操作规程》2026修订版第3条
  影响规则：unemployment-general.drl 中 getPaidMonths() >= 12 → >=10
  ```
- **标签（Tag）**：发布生产版本时打标签，如 `rule-v1.0-release`、`rule-v2.0-beta`，便于回滚。

### 2.2 应用层：运行时版本切换（适配本体论动态执行）
在代码层实现「按需加载指定版本的 DRL 规则」，满足本体论「动态计算实体状态」的需求，核心是通过 Kie 容器加载不同版本的规则。

#### 1. 版本化 Kie 容器管理
```java
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

/**
 * 本体规则版本管理器（核心：加载指定版本的DRL规则）
 */
public class OntologyRuleVersionManager {
    // 缓存不同版本的Kie容器，避免重复加载
    private static final Map<String, KieContainer> KIE_CONTAINER_CACHE = new ConcurrentHashMap<>();
    
    /**
     * 获取指定版本的KieSession
     * @param ruleVersion 规则版本（如 "v1.0", "v2.0"）
     * @return 对应版本的KieSession
     */
    public static KieSession getKieSessionByVersion(String ruleVersion) {
        // 1. 从缓存加载或初始化Kie容器
        KieContainer kieContainer = KIE_CONTAINER_CACHE.computeIfAbsent(ruleVersion, version -> {
            KieServices kieServices = KieServices.Factory.get();
            // 根据版本拼接kmodule路径（对应目录结构）
            String kmodulePath = "ontology/rules/" + version + "/kmodule.xml";
            // 加载指定版本的kmodule配置
            return kieServices.newKieContainer(
                kieServices.newKieFileSystem()
                    .write(kmodulePath, kieServices.getResources().newClassPathResource(kmodulePath))
                    .generateAndGetKieModule()
                    .getReleaseId()
            );
        });
        
        // 2. 获取对应版本的KieSession（如v1.0对应unemploymentKSessionV1）
        String ksessionName = "unemploymentKSession" + version.replace(".", "");
        return kieContainer.newKieSession(ksessionName);
    }
    
    // 清理指定版本的容器缓存（版本下线时调用）
    public static void clearContainerCache(String ruleVersion) {
        KIE_CONTAINER_CACHE.remove(ruleVersion);
    }
}
```

#### 2. 业务层调用（按版本执行规则）
```java
// 本体审查服务：支持指定规则版本执行
public class UnemploymentReviewService {
    /**
     * 执行失业保险金申领审查（指定规则版本）
     * @param applicant 申领人实体（本体根实体）
     * @param ruleVersion 规则版本（如 "v1.0"）
     * @return 审查结论实体（本体派生实体）
     */
    public EligibilityResult review(UnemployedPerson applicant, String ruleVersion) {
        // 1. 获取指定版本的KieSession
        KieSession kieSession = OntologyRuleVersionManager.getKieSessionByVersion(ruleVersion);
        
        try {
            // 2. 插入本体实体（构建审查上下文）
            kieSession.insert(applicant);
            kieSession.insert(applicant.getInsuranceRecord());
            kieSession.insert(applicant.getEmployerUnit());
            
            // 3. 执行对应版本的规则（本体论动态计算）
            kieSession.fireAllRules();
            
            // 4. 获取审查结论（本体派生实体）
            return (EligibilityResult) kieSession.getObjects(obj -> obj instanceof EligibilityResult)
                .stream().findFirst().orElseThrow(() -> new RuntimeException("无审查结论"));
        } finally {
            kieSession.dispose(); // 释放资源
        }
    }
}
```

### 2.3 进阶层：规则版本中心（适配政务系统可审计需求）
对于政务级本体论系统，建议搭建「规则版本中心」，实现 DRL 版本的可视化管理、发布审批、灰度发布，核心功能包括：

| 核心功能 | 实现方式 | 贴合本体论价值 |
|----------|----------|----------------|
| 版本可视化管理 | 前端页面展示所有DRL版本，关联「政策依据、变更人、变更时间」 | 本体论「可审计」要求，规则变更可追溯 |
| 发布审批流程 | 新版本DRL需经过「业务审核 + 技术审核」才能发布到生产 | 政务系统「合规性」要求，避免规则随意变更 |
| 灰度发布 | 按「申领人地区/类型」分批切换规则版本（如先对南山区试点v2.0） | 本体论「平稳迭代」，降低规则变更风险 |
| 版本回滚 | 一键回滚到历史稳定版本（基于Git Tag） | 本体论「高可用」，规则变更出错可快速恢复 |
| 版本对比 | 可视化对比不同版本DRL的差异（如v1.0 vs v2.0的缴费月数变更） | 本体论「规则可理解」，业务人员可直观看到变更点 |

#### 简化版版本中心落地（基于数据库）
```sql
-- 规则版本表（关联本体论规则版本与业务信息）
CREATE TABLE ontology_rule_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_version VARCHAR(20) NOT NULL COMMENT '规则版本（如v1.0）',
    kmodule_path VARCHAR(100) NOT NULL COMMENT 'kmodule.xml路径',
    policy_basis VARCHAR(500) COMMENT '政策依据（如文件编号+条款）',
    change_content TEXT COMMENT '变更内容描述',
    status TINYINT NOT NULL COMMENT '状态：0-草稿 1-测试 2-生产 3-下线',
    creator VARCHAR(50) NOT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    publish_time DATETIME COMMENT '发布时间',
    UNIQUE KEY uk_rule_version (rule_version)
);

-- 规则版本与场景关联表（支持多场景复用版本）
CREATE TABLE ontology_rule_version_scene (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_version VARCHAR(20) NOT NULL,
    scene_code VARCHAR(50) NOT NULL COMMENT '场景编码（如unemployment-general）',
    scene_name VARCHAR(100) NOT NULL COMMENT '场景名称（通用申领情形）',
    FOREIGN KEY (rule_version) REFERENCES ontology_rule_version(rule_version)
);
```

---

## 三、DRL 版本管理的最佳实践（适配政务本体论系统）
### 3.1 版本命名规范
采用「语义化版本 + 业务标识」，示例：
- `v1.0.0`：初始版本（基础规则）；
- `v1.1.0`：新增「灵活就业人员」规则（功能新增）；
- `v1.1.1`：修复「个体工商户户主」规则的停保原因判断（bug修复）；
- `v2.0.0`：调整缴费月数阈值（重大业务变更）。

### 3.2 版本发布流程
1. **开发**：在 `feature/rule-vX.X` 分支编写DRL，关联业务需求文档；
2. **测试**：测试环境加载新版本DRL，执行本体论测试用例（如「缴费10个月是否通过」）；
3. **审批**：提交版本发布申请，附「政策依据 + 测试报告」；
4. **发布**：合并到 `main` 分支，打Tag，更新数据库版本表状态为「生产」；
5. **监控**：发布后监控规则执行日志，确认无异常；
6. **归档**：将DRL版本、测试报告、审批记录归档，满足政务审计要求。

### 3.3 避免版本冲突的关键
1. **通用规则抽离**：将枚举、辅助函数等通用逻辑放在 `common` 目录，所有版本共享，避免重复定义；
2. **实体兼容性**：规则版本升级时，本体实体仅新增属性，不删除/修改原有属性，确保旧版本规则仍可运行；
3. **KieSession隔离**：不同版本的KieSession完全隔离，避免规则交叉影响。

---

## 四、总结：DRL版本管理的核心要点
1. **分层管理**：代码仓库层做基础版本控制，应用层实现运行时版本切换，进阶层搭建版本中心满足政务审计；
2. **贴合本体论**：版本管理围绕「业务语义」展开，版本号关联政策变更，而非技术迭代；
3. **隔离与兼容**：不同版本的DRL通过KieModule/KieSession隔离，实体层保持稳定，确保规则迭代不破坏本体核心；
4. **可追溯可回滚**：每一次版本变更都关联政策依据，支持一键回滚，满足政务系统「可审计、高可用」的核心需求。

这套方案既对齐了 Palantir 本体论「规则可版本化」的设计范式，又落地了政务系统的本地化要求，是 DRL 规则版本管理在本体论驱动项目中的完整解决方案。