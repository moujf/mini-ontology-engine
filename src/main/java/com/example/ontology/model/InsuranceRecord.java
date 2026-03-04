package com.example.ontology.model;

/**
 * 失业保险参保记录。
 * <p>
 * 记录申请人在深圳的失业保险缴费情况、剩余可领月数及系统记录的停保原因。
 * 停保原因（{@link StopReason}）由社保系统在停保时写入，是判断是否属于
 * 非本人意愿离职的第一手数据来源。
 * </p>
 * <p>
 * 对应操作规程校验规则：缴费月数 ≥ 12，或不满12个月但有剩余领取期限。
 * </p>
 * <p>
 * <b>申领情形（ApplyType）</b>不在本实体存储，而是由规则引擎根据以下实体组合推断：
 * <ul>
 *   <li>情形一：单位类型 ≠ 81，缴费月 ≥ 12</li>
 *   <li>情形二：单位类型 ≠ 81，缴费月 &lt; 12 但剩余月 &gt; 0</li>
 *   <li>情形三：临近退休（由 {@link UnemployedPerson} 年龄字段判断，暂外部传入标记）</li>
 *   <li>情形四：单位类型 = 81（{@link EmployerUnit#isIndividualBusiness()}）</li>
 * </ul>
 * </p>
 */
public class InsuranceRecord extends OntologyObject {

    /**
     * 停保原因枚举，对应操作规程中 14 种非本人意愿离职情形及 1 种本人意愿情形。
     * 由社保系统在单位办理停保时写入，是资格审查的核心判断依据。
     */
    public enum StopReason {
        /** 劳动合同期满 */
        CONTRACT_EXPIRED,
        /** 单位破产 */
        UNIT_BANKRUPT,
        /** 单位被责令关闭 */
        UNIT_ORDERED_CLOSE,
        /** 单位被吊销营业执照 */
        UNIT_LICENSE_REVOKED,
        /** 单位被撤销或提前解散 */
        UNIT_DISSOLVED,
        /** 用人单位因劳动者过错解除合同 */
        UNIT_FIRE_FOR_FAULT,
        /** 用人单位提前通知劳动者解除合同 */
        UNIT_ADVANCE_NOTICE,
        /** 用人单位额外支付一个月工资解除合同 */
        UNIT_PAY_ONE_MONTH,
        /** 用人单位依照劳动合同法第41条裁减人员 */
        UNIT_LAYOFF_ARTICLE41,
        /** 双方协商一致后单位提出解除合同 */
        MUTUAL_AGREEMENT_UNIT_PROPOSE,
        /** 用人单位过错导致劳动者解除合同 */
        WORKER_QUIT_FOR_UNIT_FAULT,
        /** 事业单位提出解除聘用合同 */
        PUBLIC_UNIT_TERMINATE,
        /** 个体工商户：单位注销、吊销、撤销 */
        INDIVIDUAL_UNIT_CLOSED,
        /** 个体工商户：停产停业 */
        INDIVIDUAL_STOP_PRODUCTION,
        /** 本人意愿离职（不符合申领条件） */
        VOLUNTARY_RESIGN
    }

    /**
     * 构造参保记录。
     *
     * @param applicantId         关联申请人证件号码
     * @param paidMonths          失业保险累计缴费月数
     * @param remainingMonths     可领取失业保险待遇剩余月数（0 表示无剩余）
     * @param lastInsuredShenzhen 最后参保地是否为深圳
     * @param stopReason          系统停保原因
     */
    public InsuranceRecord(String applicantId,
                           int paidMonths,
                           int remainingMonths,
                           boolean lastInsuredShenzhen,
                           StopReason stopReason) {
        super(applicantId + "-INS", "InsuranceRecord");
        setAttr("applicantId",         applicantId);
        setAttr("paidMonths",          paidMonths);
        setAttr("remainingMonths",     remainingMonths);
        setAttr("lastInsuredShenzhen", lastInsuredShenzhen);
        setAttr("stopReason",          stopReason);
    }

    /** @return 关联申请人证件号码 */
    public String getApplicantId() { return (String) getAttr("applicantId"); }

    /** @return 累计缴费月数 */
    public int getPaidMonths() { return (int) getAttr("paidMonths"); }

    /** @return 可领取失业保险待遇剩余月数 */
    public int getRemainingMonths() { return (int) getAttr("remainingMonths"); }

    /** @return 最后参保地是否为深圳 */
    public boolean isLastInsuredShenzhen() { return (boolean) getAttr("lastInsuredShenzhen"); }

    /**
     * 系统停保原因，由社保系统写入。
     *
     * @return 停保原因枚举值
     */
    public StopReason getStopReason() { return (StopReason) getAttr("stopReason"); }

    /**
     * 是否满足基本缴费条件。
     * 规则：缴费月数 ≥ 12，或不满12个月但有剩余领取期限且剩余月数 &gt; 0。
     *
     * @return true 表示满足缴费条件
     */
    public boolean meetsPaymentCondition() {
        return getPaidMonths() >= 12 || getRemainingMonths() > 0;
    }
}

