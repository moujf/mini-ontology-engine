package com.example.ontology.web.dto;

/**
 * 前端提交单条申请人数据的请求体。
 * 字段与 ApplicantSnapshot 基本对应，额外增加银行账户字段。
 */
public class ApplicantRequest {

    // ── 申请人 ──────────────────────────────────────────────────
    private String id;               // 证件号码（唯一标识）
    private String name;             // 姓名
    private boolean domicileShenzhen;
    private boolean nearRetirement;

    // ── 参保记录 ─────────────────────────────────────────────────
    private int    paidMonths;
    private int    remainingMonths;
    private boolean lastInsuredShenzhen;
    private String stopReason;       // InsuranceRecord.StopReason 枚举名

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

    public int getPaidMonths() { return paidMonths; }
    public void setPaidMonths(int v) { this.paidMonths = v; }

    public int getRemainingMonths() { return remainingMonths; }
    public void setRemainingMonths(int v) { this.remainingMonths = v; }

    public boolean isLastInsuredShenzhen() { return lastInsuredShenzhen; }
    public void setLastInsuredShenzhen(boolean v) { this.lastInsuredShenzhen = v; }

    public String getStopReason() { return stopReason; }
    public void setStopReason(String v) { this.stopReason = v; }

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
}
