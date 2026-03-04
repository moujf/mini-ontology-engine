package com.example.ontology.model;

/**
 * 资格审查结果实体。
 * <p>
 * 由 Drools 规则引擎在规则命中时自动创建并插入工作内存。
 * id 命名约定：
 * <ul>
 *   <li>{personId}-ELIG     — 满足资格条件</li>
 *   <li>{personId}-NOT-ELIG — 不满足资格条件（携带拒绝原因）</li>
 * </ul>
 * 初始状态为 {@code NEW}，经 ActionEngine 处理后变为 {@code DONE}。
 * </p>
 */
public class EligibilityResult extends OntologyObject {

    /**
     * 构造审查通过结果。
     *
     * @param id 结果唯一标识，通常为 "{personId}-ELIG"
     */
    public EligibilityResult(String id) {
        super(id, "EligibilityResult");
        setState("NEW");
        setAttr("rejectReason", "");
    }

    /**
     * 构造审查拒绝结果，携带拒绝原因。
     *
     * @param id           结果唯一标识，通常为 "{personId}-NOT-ELIG"
     * @param rejectReason 拒绝原因描述（英文，分号分隔多条）
     */
    public EligibilityResult(String id, String rejectReason) {
        super(id, "EligibilityResult");
        setState("NEW");
        setAttr("rejectReason", rejectReason != null ? rejectReason : "");
    }

    /**
     * 构造审查通过结果，携带情形代码和中文说明（本体论语义化版本）。
     * <p>
     * 由重构后的 DRL 规则使用，id 命名约定保持 "{personId}-ELIG" 以兼容测试。
     * </p>
     *
     * @param personId    申请人证件号码
     * @param scenarioCode 情形代码，如 "ELIGIBLE_SCENARIO_1"
     * @param description  中文描述，如 "符合一般职工申领条件"
     */
    public EligibilityResult(String personId, String scenarioCode, String description) {
        super(personId + "-ELIG", "EligibilityResult");
        setState("NEW");
        setAttr("rejectReason",  "");
        setAttr("scenarioCode",  scenarioCode);
        setAttr("description",   description);
    }

    /**
     * 获取情形代码（重构后 DRL 写入）。
     *
     * @return 情形代码，如 "ELIGIBLE_SCENARIO_1"
     */
    public String getScenarioCode() {
        return (String) getAttr("scenarioCode");
    }

    /**
     * 获取中文描述。
     *
     * @return 描述文本
     */
    public String getDescription() {
        return (String) getAttr("description");
    }

    /**
     * 获取拒绝原因。审查通过时返回空字符串。
     *
     * @return 拒绝原因
     */
    public String getRejectReason() {
        return (String) getAttr("rejectReason");
    }
}
