package com.example.ontology.web.dto;

/**
 * 单条申请人录入信息快照（用于前端"录入阶段"卡片展示）。
 */
public class ApplicantSnapshot {
    private String id;
    private String name;
    private boolean domicileShenzhen;
    private boolean nearRetirement;

    // insurance
    private int paidMonths;
    private int remainingMonths;
    private boolean lastInsuredShenzhen;
    private String stopReason;

    // employer
    private String unitName;
    private String unitType;
    private String unitTypeLabel;

    // registration
    private boolean registered;
    private String registrationDate;

    // material
    private String materialType;
    private String terminationDate;
    private String issuingUnit;

    // bank
    private String bankName;
    private String accountNo;

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

    public String getUnitName() { return unitName; }
    public void setUnitName(String v) { this.unitName = v; }

    public String getUnitType() { return unitType; }
    public void setUnitType(String v) { this.unitType = v; }

    public String getUnitTypeLabel() { return unitTypeLabel; }
    public void setUnitTypeLabel(String v) { this.unitTypeLabel = v; }

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
}
