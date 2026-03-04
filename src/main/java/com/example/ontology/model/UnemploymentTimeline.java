package com.example.ontology.model;

import java.util.Comparator;
import java.util.List;

/**
 * 申请人的失业保险时间线聚合实体。
 * <p>
 * 封装一位申请人所有参保记录的时序分析结果，是"时间维度控制"能力的核心载体。
 * 构造时自动完成两项预计算，将日期比对逻辑从 DRL 中彻底下沉到本体层：
 * </p>
 * <ol>
 *   <li><b>latestRecord</b> — 按 {@link InsuranceRecord#getStopDate()} 降序取第一条，
 *       即停保日期最近的一段参保记录（时间线终点）</li>
 *   <li><b>totalMonths</b> — 所有记录的 {@code paidMonths} 之和，用于判定累计缴费是否达标</li>
 * </ol>
 *
 * <h3>Palantir 本体论设计说明</h3>
 * <p>
 * DRL 规则只需匹配 {@code UnemploymentTimeline}，通过
 * {@code this == $timeline.getLatestRecord()} 锁定最后一条记录，
 * 无需在规则中写任何日期运算或排序逻辑。
 * </p>
 *
 * <h3>典型 DRL 用法</h3>
 * <pre>{@code
 * $timeline : UnemploymentTimeline(personId == $p.getId())
 * $latest   : InsuranceRecord(
 *     this == $timeline.getLatestRecord(),
 *     isInvoluntaryStop() == true,
 *     isLastInsuredShenzhen() == true
 * )
 * eval($timeline.getTotalMonths() >= $policy.getMinContributionMonths()
 *      || $latest.getRemainingMonths() > 0)
 * }</pre>
 */
public class UnemploymentTimeline extends OntologyObject {

    /**
     * 构造申请人时间线，自动从多条记录中计算 latestRecord 和 totalMonths。
     *
     * @param personId 关联申请人证件号码
     * @param records  该申请人的所有参保记录（顺序不限，至少一条）
     * @throws IllegalArgumentException 若 records 为空
     */
    public UnemploymentTimeline(String personId, List<InsuranceRecord> records) {
        super(personId + "-TIMELINE", "UnemploymentTimeline");

        if (records == null || records.isEmpty()) {
            throw new IllegalArgumentException(
                "UnemploymentTimeline requires at least one InsuranceRecord for personId=" + personId);
        }

        // 按停保日期降序排列，取第一条为"最后参保记录"
        InsuranceRecord latest = records.stream()
            .max(Comparator.comparing(InsuranceRecord::getStopDate))
            .orElseThrow();

        // 累计所有记录的缴费月数
        int total = records.stream()
            .mapToInt(InsuranceRecord::getPaidMonths)
            .sum();

        setAttr("personId",     personId);
        setAttr("latestRecord", latest);
        setAttr("totalMonths",  total);
    }

    /**
     * 关联申请人证件号码，用于 DRL 中关联匹配。
     *
     * @return 申请人证件号码
     */
    public String getPersonId() {
        return (String) getAttr("personId");
    }

    /**
     * 最后一段参保记录（stopDate 最大的那条）。
     * DRL 中通过 {@code this == $timeline.getLatestRecord()} 锁定此记录，
     * 避免在规则中手写日期比对逻辑。
     *
     * @return 最后参保记录
     */
    public InsuranceRecord getLatestRecord() {
        return (InsuranceRecord) getAttr("latestRecord");
    }

    /**
     * 所有有效参保记录的累计缴费月数（跨多段叠加）。
     * 用于 DRL 的 eval 条件中与 {@link UnemploymentPolicy#getMinContributionMonths()} 比对。
     *
     * @return 累计缴费月数
     */
    public int getTotalMonths() {
        return (int) getAttr("totalMonths");
    }
}
