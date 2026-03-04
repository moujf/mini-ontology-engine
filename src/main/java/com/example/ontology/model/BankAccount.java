package com.example.ontology.model;

/**
 * 银行账户信息。
 * <p>
 * 用于失业保险金发放的收款账户，对应操作规程信息采集项：
 * 银行账号、银行名称、银行户名。
 * 受理要求：选择账号用途类别为"待遇拨付/发放账号"。
 * </p>
 */
public class BankAccount extends OntologyObject {

    /**
     * 构造银行账户信息。
     *
     * @param applicantId 关联申请人证件号码
     * @param accountNo   银行账号
     * @param bankName    开户行名称
     * @param accountName 银行户名（须与申请人姓名一致）
     */
    public BankAccount(String applicantId,
                       String accountNo,
                       String bankName,
                       String accountName) {
        super(applicantId + "-BANK", "BankAccount");
        setAttr("applicantId",  applicantId);
        setAttr("accountNo",    accountNo);
        setAttr("bankName",     bankName);
        setAttr("accountName",  accountName);
    }

    /** @return 关联申请人证件号码 */
    public String getApplicantId() { return (String) getAttr("applicantId"); }

    /** @return 银行账号 */
    public String getAccountNo() { return (String) getAttr("accountNo"); }

    /** @return 开户行名称 */
    public String getBankName() { return (String) getAttr("bankName"); }

    /** @return 银行户名 */
    public String getAccountName() { return (String) getAttr("accountName"); }
}
