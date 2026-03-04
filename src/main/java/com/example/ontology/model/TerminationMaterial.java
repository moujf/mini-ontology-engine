package com.example.ontology.model;

/**
 * 劳动关系终止材料。
 * <p>
 * 证明劳动关系终止原因的文件实体。申请人须提交与停保原因对应的纸质/电子材料。
 * 操作规程要求：材料记载的终止原因须与 {@link InsuranceRecord} 中的停保原因一致，
 * 且须属于非本人意愿离职范畴。
 * </p>
 *
 * <p>材料类型与停保原因映射（参考）：</p>
 * <ul>
 *   <li>合同到期证明              → CONTRACT_EXPIRED</li>
 *   <li>破产/吊销/撤销文件        → UNIT_BANKRUPT / UNIT_ORDERED_CLOSE / UNIT_LICENSE_REVOKED / UNIT_DISSOLVED</li>
 *   <li>裁员通知（第41条）        → UNIT_LAYOFF_ARTICLE41</li>
 *   <li>用人单位解除通知          → UNIT_ADVANCE_NOTICE / UNIT_PAY_ONE_MONTH / UNIT_FIRE_FOR_FAULT</li>
 *   <li>协商解除协议              → MUTUAL_AGREEMENT_UNIT_PROPOSE</li>
 *   <li>劳动者被迫解除证明        → WORKER_QUIT_FOR_UNIT_FAULT</li>
 *   <li>事业单位解聘文件          → PUBLIC_UNIT_TERMINATE</li>
 *   <li>个体工商户注销/停业文件   → INDIVIDUAL_UNIT_CLOSED / INDIVIDUAL_STOP_PRODUCTION</li>
 *   <li>本人辞职申请              → VOLUNTARY_RESIGN（不符合申领条件）</li>
 * </ul>
 */
public class TerminationMaterial extends OntologyObject {

    /**
     * 材料类型枚举，对应操作规程附件2所列终止证明材料。
     */
    public enum MaterialType {
        /** 解除（终止）劳动合同通知书 */
        CONTRACT_TERMINATION_NOTICE,
        /** 用工单位注销、吊销、撤销通知书 */
        UNIT_CLOSURE_NOTICE,
        /** 停产停业通知 */
        STOP_PRODUCTION_NOTICE,
        /** 劳动合同（用于证明入职时间、合同性质） */
        LABOR_CONTRACT,
        /** 经济补偿支付凭证 */
        ECONOMIC_COMPENSATION_VOUCHER,
        /** 告知承诺书 */
        COMMITMENT_LETTER,
        /** 其他材料 */
        OTHER
    }

    /**
     * 构造劳动关系终止材料。
     *
     * @param applicantId     关联申请人证件号码
     * @param materialType    材料类型
     * @param terminationDate 劳动关系终止日期（格式 yyyy-MM-dd）
     * @param issuingUnit     出具单位名称
     */
    public TerminationMaterial(String applicantId,
                               MaterialType materialType,
                               String terminationDate,
                               String issuingUnit) {
        super(applicantId + "-MAT", "TerminationMaterial");
        setAttr("applicantId",     applicantId);
        setAttr("materialType",    materialType);
        setAttr("terminationDate", terminationDate != null ? terminationDate : "");
        setAttr("issuingUnit",     issuingUnit != null ? issuingUnit : "");
    }

    /** @return 关联申请人证件号码 */
    public String getApplicantId() { return (String) getAttr("applicantId"); }

    /** @return 材料类型 */
    public MaterialType getMaterialType() { return (MaterialType) getAttr("materialType"); }

    /** @return 劳动关系终止日期 */
    public String getTerminationDate() { return (String) getAttr("terminationDate"); }

    /** @return 出具单位名称 */
    public String getIssuingUnit() { return (String) getAttr("issuingUnit"); }
}

