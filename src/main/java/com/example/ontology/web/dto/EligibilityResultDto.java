package com.example.ontology.web.dto;

/**
 * 单条审查结论（用于前端"结论阶段"卡片展示）。
 */
public class EligibilityResultDto {
    private String applicantId;
    private String applicantName;
    private boolean approved;
    private String rejectReason;
    // 适用情形描述，由 service 层推断
    private String scenario;
    // 延迟退休信息（有出生日期时由 service 层计算后写入）
    private String retirementDate;   // 法定退休日期，格式 "yyyy-MM"，无出生日期时为 null
    private Boolean nearRetirement;  // 是否临近退休，无出生日期时为 null
    // 发放账户信息（仅 approved 时有值）
    private String bankName;
    private String accountNo;
    private String accountName;

    public String getApplicantId() { return applicantId; }
    public void setApplicantId(String v) { this.applicantId = v; }

    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String v) { this.applicantName = v; }

    public boolean isApproved() { return approved; }
    public void setApproved(boolean v) { this.approved = v; }

    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String v) { this.rejectReason = v; }

    public String getScenario() { return scenario; }
    public void setScenario(String v) { this.scenario = v; }

    public String getRetirementDate() { return retirementDate; }
    public void setRetirementDate(String v) { this.retirementDate = v; }

    public Boolean getNearRetirement() { return nearRetirement; }
    public void setNearRetirement(Boolean v) { this.nearRetirement = v; }

    public String getBankName() { return bankName; }
    public void setBankName(String v) { this.bankName = v; }

    public String getAccountNo() { return accountNo; }
    public void setAccountNo(String v) { this.accountNo = v; }

    public String getAccountName() { return accountName; }
    public void setAccountName(String v) { this.accountName = v; }
}
