package com.example.ontology.model;

/**
 * 用人单位实体。
 * <p>
 * 记录申请人最后就职的用人单位基本信息。
 * 单位类型（{@link UnitType}）是判断申领情形的关键依据：
 * <ul>
 *   <li>代码 81（个体工商户有雇工）→ 情形四，适用个体工商户申领规则</li>
 *   <li>代码 50/55/56/57（事业单位）→ 停保原因须为 PUBLIC_UNIT_TERMINATE</li>
 *   <li>代码 10（企业）及其他       → 一般职工情形，适用情形一/二规则</li>
 * </ul>
 * </p>
 */
public class EmployerUnit extends OntologyObject {

    /**
     * 单位类型枚举，对应《社会保险登记证》单位类型国标编码。
     * 代码体系来源：深圳市社会保险信息系统单位类型字典。
     */
    public enum UnitType {
        /** 10 — 企业 */
        ENTERPRISE("10", "企业"),
        /** 30 — 机关 */
        GOVERNMENT("30", "机关"),
        /** 50 — 事业单位 */
        PUBLIC_INSTITUTION("50", "事业单位"),
        /** 55 — 全额拨款事业单位 */
        PUBLIC_FULL_FUND("55", "全额拨款事业单位"),
        /** 56 — 差额拨款事业单位 */
        PUBLIC_PARTIAL_FUND("56", "差额拨款事业单位"),
        /** 57 — 自收自支事业单位 */
        PUBLIC_SELF_FUND("57", "自收自支事业单位"),
        /** 70 — 社会团体 */
        SOCIAL_ORGANIZATION("70", "社会团体"),
        /** 80 — 自定义机构 */
        CUSTOM("80", "自定义机构"),
        /** 81 — 个体工商户（有雇工的） */
        INDIVIDUAL_BUSINESS("81", "个体工商户（有雇工的）"),
        /** 82 — 律师事务所 */
        LAW_FIRM("82", "律师事务所"),
        /** 83 — 会计师事务所 */
        ACCOUNTING_FIRM("83", "会计师事务所"),
        /** 84 — 驻华代表机构 */
        REP_OFFICE("84", "驻华代表机构"),
        /** 85 — 外国常驻新闻机构 */
        FOREIGN_PRESS("85", "外国常驻新闻机构"),
        /** 86 — 外国企业常驻代表机构 */
        FOREIGN_ENTERPRISE_REP("86", "外国企业常驻代表机构"),
        /** 87 — 境外非政府组织驻华代表机构 */
        FOREIGN_NGO_REP("87", "境外非政府组织驻华代表机构"),
        /** 90 — 其他组织机构 */
        OTHER_ORG("90", "其他组织机构"),
        /** 91 — 民办非企业单位（社会服务机构） */
        PRIVATE_NON_ENTERPRISE("91", "民办非企业单位（社会服务机构）"),
        /** 93 — 基金会 */
        FOUNDATION("93", "基金会"),
        /** 94 — 宗教活动场所 */
        RELIGIOUS("94", "宗教活动场所"),
        /** 95 — 农村村民委员会 */
        VILLAGE_COMMITTEE("95", "农村村民委员会"),
        /** 96 — 城市居民委员会 */
        RESIDENT_COMMITTEE("96", "城市居民委员会"),
        /** 98 — 军队 */
        MILITARY("98", "军队"),
        /** 99 — 其他 */
        OTHER("99", "其他");

        private final String code;
        private final String label;

        UnitType(String code, String label) {
            this.code  = code;
            this.label = label;
        }

        /** @return 单位类型代码（如 "10"、"81"） */
        public String getCode()  { return code; }

        /** @return 单位类型中文名称 */
        public String getLabel() { return label; }

        /**
         * 按代码字符串查找枚举值。
         *
         * @param code 代码字符串
         * @return 对应枚举值，未找到则返回 {@link #OTHER}
         */
        public static UnitType fromCode(String code) {
            for (UnitType t : values()) {
                if (t.code.equals(code)) return t;
            }
            return OTHER;
        }
    }

    /**
     * 构造用人单位。
     *
     * @param applicantId 关联申请人证件号码
     * @param unitCode    单位社会信用代码或登记证号
     * @param unitName    单位名称
     * @param unitType    单位类型
     */
    public EmployerUnit(String applicantId,
                        String unitCode,
                        String unitName,
                        UnitType unitType) {
        super(applicantId + "-UNIT", "EmployerUnit");
        setAttr("applicantId", applicantId);
        setAttr("unitCode",    unitCode != null ? unitCode : "");
        setAttr("unitName",    unitName != null ? unitName : "");
        setAttr("unitType",    unitType);
    }

    /** @return 关联申请人证件号码 */
    public String getApplicantId() { return (String) getAttr("applicantId"); }

    /** @return 单位社会信用代码或登记证号 */
    public String getUnitCode() { return (String) getAttr("unitCode"); }

    /** @return 单位名称 */
    public String getUnitName() { return (String) getAttr("unitName"); }

    /** @return 单位类型 */
    public UnitType getUnitType() { return (UnitType) getAttr("unitType"); }

    /**
     * 是否为个体工商户（有雇工的，代码 81）。
     * 用于规则引擎判断情形四（个体工商户申领情形）。
     *
     * @return true 表示单位类型为个体工商户
     */
    public boolean isIndividualBusiness() {
        return getUnitType() == UnitType.INDIVIDUAL_BUSINESS;
    }

    /**
     * 是否为事业单位（代码 50/55/56/57）。
     * 用于规则引擎判断适用停保原因是否为事业单位解聘。
     *
     * @return true 表示单位类型为事业单位（含全额/差额/自收自支）
     */
    public boolean isPublicInstitution() {
        UnitType t = getUnitType();
        return t == UnitType.PUBLIC_INSTITUTION
            || t == UnitType.PUBLIC_FULL_FUND
            || t == UnitType.PUBLIC_PARTIAL_FUND
            || t == UnitType.PUBLIC_SELF_FUND;
    }
}
