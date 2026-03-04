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
     * 获取拒绝原因。审查通过时返回空字符串。
     *
     * @return 拒绝原因
     */
    public String getRejectReason() {
        return (String) getAttr("rejectReason");
    }
}
