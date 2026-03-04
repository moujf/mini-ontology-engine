package com.example.ontology.model;

import java.time.LocalDate;

/**
 * 失业保险参保记录（单段）。
 * <p>
 * 记录申请人在深圳的一段连续参保缴费情况、停保日期及停保原因。
 * 一位申请人可能有多段参保记录（多次就业/离职），由
 * {@link UnemploymentTimeline} 负责聚合，并通过 {@code stopDate} 排序
 * 确定 {@code latestRecord}（最后一段）。
 * </p>
 * <p>
 * 对应操作规程校验规则：缴费月数 ≥ 12，或不满12个月但有剩余领取期限。
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
     * 构造参保记录（含停保日期，用于时间线排序）。
     *
     * @param recordId            记录唯一标识（如 "{applicantId}-INS-1"）
     * @param applicantId         关联申请人证件号码
     * @param paidMonths          本段失业保险缴费月数
     * @param remainingMonths     可领取失业保险待遇剩余月数（0 表示无剩余）
     * @param lastInsuredShenzhen 本段参保地是否为深圳
     * @param stopReason          系统停保原因
     * @param stopDate            停保日期，用于 {@link UnemploymentTimeline} 排序确定最后参保段
     */
    public InsuranceRecord(String recordId,
                           String applicantId,
                           int paidMonths,
                           int remainingMonths,
                           boolean lastInsuredShenzhen,
                           StopReason stopReason,
                           LocalDate stopDate) {
        super(recordId, "InsuranceRecord");
        setAttr("applicantId",         applicantId);
        setAttr("paidMonths",          paidMonths);
        setAttr("remainingMonths",     remainingMonths);
        setAttr("lastInsuredShenzhen", lastInsuredShenzhen);
        setAttr("stopReason",          stopReason);
        setAttr("stopDate",            stopDate);
    }

    /**
     * 向后兼容构造器（单记录场景，stopDate 默认今天）。
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
        this(applicantId + "-INS",
             applicantId,
             paidMonths,
             remainingMonths,
             lastInsuredShenzhen,
             stopReason,
             LocalDate.now());
    }

    /** @return 关联申请人证件号码 */
    public String getApplicantId() { return (String) getAttr("applicantId"); }

    /** @return 本段缴费月数 */
    public int getPaidMonths() { return (int) getAttr("paidMonths"); }

    /** @return 可领取失业保险待遇剩余月数 */
    public int getRemainingMonths() { return (int) getAttr("remainingMonths"); }

    /** @return 本段参保地是否为深圳 */
    public boolean isLastInsuredShenzhen() { return (boolean) getAttr("lastInsuredShenzhen"); }

    /** @return 系统停保原因 */
    public StopReason getStopReason() { return (StopReason) getAttr("stopReason"); }

    /**
     * 停保日期，由 {@link UnemploymentTimeline} 用于时序排序。
     *
     * @return 停保日期
     */
    public LocalDate getStopDate() { return (LocalDate) getAttr("stopDate"); }

    /**
     * 停保原因是否属于非本人意愿（语义化布尔属性）。
     * 覆盖操作规程中 14 种合法非本人意愿停保原因。
     *
     * @return true 表示停保原因属于非本人意愿
     */
    public boolean isInvoluntaryStop() {
        StopReason reason = getStopReason();
        if (reason == null) return false;
        switch (reason) {
            case CONTRACT_EXPIRED:
            case UNIT_BANKRUPT:
            case UNIT_ORDERED_CLOSE:
            case UNIT_LICENSE_REVOKED:
            case UNIT_DISSOLVED:
            case UNIT_FIRE_FOR_FAULT:
            case UNIT_ADVANCE_NOTICE:
            case UNIT_PAY_ONE_MONTH:
            case UNIT_LAYOFF_ARTICLE41:
            case MUTUAL_AGREEMENT_UNIT_PROPOSE:
            case WORKER_QUIT_FOR_UNIT_FAULT:
            case PUBLIC_UNIT_TERMINATE:
            case INDIVIDUAL_UNIT_CLOSED:
            case INDIVIDUAL_STOP_PRODUCTION:
                return true;
            default:
                return false;
        }
    }

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
