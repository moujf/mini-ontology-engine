package com.example.ontology.web.dto;

import java.util.List;

/**
 * 前端提交单条申请人数据的请求体。
 * 字段与 ApplicantSnapshot 基本对应，额外增加银行账户字段。
 * <p>
 * v2：支持多条参保记录（insuranceRecords），同时保留单条字段做向后兼容。
 * v2：支持自定义政策参数（policy.*）。
 * </p>
 */
public class ApplicantRequest {

    // ── 申请人 ──────────────────────────────────────────────────
    private String id;               // 证件号码（唯一标识）
    private String name;             // 姓名
    private boolean domicileShenzhen;
    private boolean nearRetirement;
    private String birthday;         // 出生日期 yyyy-MM-dd（可选，用于延迟退休动态计算）
    private String gender;           // 性别/岗位："male"、"female55"、"female50"（可选）

    // ── 参保记录（v2：多条；v1 向后兼容单条字段） ─────────────────
    /** 多条参保记录列表（优先使用，若非空则忽略 v1 单条字段） */
    private List<InsuranceRecordItem> insuranceRecords;

    // ── 参保记录（v1 向后兼容） ──────────────────────────────────
    private int    paidMonths;
    private int    remainingMonths;
    private boolean lastInsuredShenzhen;
    private String stopReason;       // InsuranceRecord.StopReason 枚举名

    // ── 政策参数（可选，不传则使用默认值） ──────────────────────
    private PolicyParams policy;

    // ── 用人单位 ─────────────────────────────────────────────────
    private String unitCreditCode;   // 统一社会信用代码
    private String unitName;
    private String unitType;         // EmployerUnit.UnitType 枚举名

    // ── 失业登记 ─────────────────────────────────────────────────
    private boolean registered;
    private String  registrationDate;

    // ── 终止材料 ─────────────────────────────────────────────────
    private String materialType;     // TerminationMaterial.MaterialType 枚举名
    private String terminationDate;
    private String issuingUnit;

    // ── 银行账户 ─────────────────────────────────────────────────
    private String bankName;
    private String accountNo;
    private String accountName;

    // ── 自定义场景描述（前端可选传入） ─────────────────────────
    private String scenario;

    // Getters / Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isDomicileShenzhen() { return domicileShenzhen; }
    public void setDomicileShenzhen(boolean v) { this.domicileShenzhen = v; }

    public boolean isNearRetirement() { return nearRetirement; }
    public void setNearRetirement(boolean v) { this.nearRetirement = v; }

    public String getBirthday() { return birthday; }
    public void setBirthday(String v) { this.birthday = v; }

    public String getGender() { return gender; }
    public void setGender(String v) { this.gender = v; }

    public int getPaidMonths() { return paidMonths; }
    public void setPaidMonths(int v) { this.paidMonths = v; }

    public int getRemainingMonths() { return remainingMonths; }
    public void setRemainingMonths(int v) { this.remainingMonths = v; }

    public boolean isLastInsuredShenzhen() { return lastInsuredShenzhen; }
    public void setLastInsuredShenzhen(boolean v) { this.lastInsuredShenzhen = v; }

    public String getStopReason() { return stopReason; }
    public void setStopReason(String v) { this.stopReason = v; }

    public List<InsuranceRecordItem> getInsuranceRecords() { return insuranceRecords; }
    public void setInsuranceRecords(List<InsuranceRecordItem> v) { this.insuranceRecords = v; }

    public PolicyParams getPolicy() { return policy; }
    public void setPolicy(PolicyParams v) { this.policy = v; }

    public String getUnitCreditCode() { return unitCreditCode; }
    public void setUnitCreditCode(String v) { this.unitCreditCode = v; }

    public String getUnitName() { return unitName; }
    public void setUnitName(String v) { this.unitName = v; }

    public String getUnitType() { return unitType; }
    public void setUnitType(String v) { this.unitType = v; }

    public boolean isRegistered() { return registered; }
    public void setRegistered(boolean v) { this.registered = v; }

    public String getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(String v) { this.registrationDate = v; }

    public String getMaterialType() { return materialType; }
    public void setMaterialType(String v) { this.materialType = v; }

    public String getTerminationDate() { return terminationDate; }
    public void setTerminationDate(String v) { this.terminationDate = v; }

    public String getIssuingUnit() { return issuingUnit; }
    public void setIssuingUnit(String v) { this.issuingUnit = v; }

    public String getBankName() { return bankName; }
    public void setBankName(String v) { this.bankName = v; }

    public String getAccountNo() { return accountNo; }
    public void setAccountNo(String v) { this.accountNo = v; }

    public String getAccountName() { return accountName; }
    public void setAccountName(String v) { this.accountName = v; }

    public String getScenario() { return scenario; }
    public void setScenario(String v) { this.scenario = v; }

    // ── 多条参保记录内嵌 DTO ─────────────────────────────────────
    public static class InsuranceRecordItem {
        private String  recordId;           // 可选，前端不填则自动生成
        private int     paidMonths;
        private int     remainingMonths;
        private boolean lastInsuredShenzhen;
        private String  stopReason;
        private String  stopDate;           // yyyy-MM-dd，可选（不填默认今日）

        public String getRecordId() { return recordId; }
        public void setRecordId(String v) { this.recordId = v; }
        public int getPaidMonths() { return paidMonths; }
        public void setPaidMonths(int v) { this.paidMonths = v; }
        public int getRemainingMonths() { return remainingMonths; }
        public void setRemainingMonths(int v) { this.remainingMonths = v; }
        public boolean isLastInsuredShenzhen() { return lastInsuredShenzhen; }
        public void setLastInsuredShenzhen(boolean v) { this.lastInsuredShenzhen = v; }
        public String getStopReason() { return stopReason; }
        public void setStopReason(String v) { this.stopReason = v; }
        public String getStopDate() { return stopDate; }
        public void setStopDate(String v) { this.stopDate = v; }
    }

    // ── 政策参数内嵌 DTO ─────────────────────────────────────────
    public static class PolicyParams {
        private Integer minContributionMonths; // 默认 12
        private Integer maleRetirementAge;     // 默认 60
        private Integer femaleRetirementAge;   // 默认 55
        private Integer nearRetirementYears;   // 默认 5

        public Integer getMinContributionMonths() { return minContributionMonths; }
        public void setMinContributionMonths(Integer v) { this.minContributionMonths = v; }
        public Integer getMaleRetirementAge() { return maleRetirementAge; }
        public void setMaleRetirementAge(Integer v) { this.maleRetirementAge = v; }
        public Integer getFemaleRetirementAge() { return femaleRetirementAge; }
        public void setFemaleRetirementAge(Integer v) { this.femaleRetirementAge = v; }
        public Integer getNearRetirementYears() { return nearRetirementYears; }
        public void setNearRetirementYears(Integer v) { this.nearRetirementYears = v; }
    }
}
