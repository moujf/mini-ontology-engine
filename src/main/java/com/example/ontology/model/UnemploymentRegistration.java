package com.example.ontology.model;

/**
 * 失业登记信息。
 * <p>
 * 记录申请人在社保部门完成失业登记的状态及日期。
 * 对应操作规程校验规则：失业登记状态为有效，且失业登记日期在离职日期之后。
 * </p>
 */
public class UnemploymentRegistration extends OntologyObject {

    /**
     * 构造失业登记信息。
     *
     * @param applicantId    关联申请人证件号码
     * @param registered     是否已完成有效失业登记
     * @param registrationDate 失业登记日期（格式 yyyy-MM-dd，可为 null）
     */
    public UnemploymentRegistration(String applicantId,
                                    boolean registered,
                                    String registrationDate) {
        super(applicantId + "-REG", "UnemploymentRegistration");
        setAttr("applicantId",       applicantId);
        setAttr("registered",        registered);
        setAttr("registrationDate",  registrationDate != null ? registrationDate : "");
    }

    /** @return 关联申请人证件号码 */
    public String getApplicantId() { return (String) getAttr("applicantId"); }

    /** @return 失业登记状态是否有效 */
    public boolean isRegistered() { return (boolean) getAttr("registered"); }

    /** @return 失业登记日期字符串（yyyy-MM-dd） */
    public String getRegistrationDate() { return (String) getAttr("registrationDate"); }
}
