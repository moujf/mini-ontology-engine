package com.example.ontology;

import com.example.ontology.engine.OntologyRuleVersionManager;
import com.example.ontology.engine.RuleEngine;
import com.example.ontology.model.BankAccount;
import com.example.ontology.model.EligibilityResult;
import com.example.ontology.model.EmployerUnit;
import com.example.ontology.model.EmployerUnit.UnitType;
import com.example.ontology.model.InsuranceRecord;
import com.example.ontology.model.InsuranceRecord.StopReason;
import com.example.ontology.model.TerminationMaterial;
import com.example.ontology.model.TerminationMaterial.MaterialType;
import com.example.ontology.model.UnemployedPerson;
import com.example.ontology.model.UnemploymentPolicy;
import com.example.ontology.model.UnemploymentRegistration;
import com.example.ontology.model.UnemploymentTimeline;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 失业保险金申领资格规则引擎单元测试。
 * <p>
 * 每个测试方法独立创建 {@link RuleEngine} 实例，插入单一申请人的全部关联实体，
 * 触发规则后从工作内存中提取 {@link EligibilityResult} 并进行断言。
 * </p>
 *
 * <pre>
 * 测试矩阵（共 13 个场景）：
 *
 *   情形一：
 *     scenario1_approved               — 企业职工，合同期满，缴费36月                    → ELIG
 *     scenario1_rejected_voluntaryResign — 主动辞职（自愿停保）                           → NOT-ELIG
 *
 *   情形二：
 *     scenario2_approved               — 单位裁员（Article 41），缴费8月有剩余4月        → ELIG
 *     scenario2_rejected_noRemaining   — 缴费不满12月且无剩余领取期限                    → NOT-ELIG
 *
 *   情形三：
 *     scenario3_approved               — 临近退休，事业单位，合同期满，缴费240月         → ELIG
 *     scenario3_rejected_noRegistration — 临近退休，但未办失业登记                        → NOT-ELIG
 *
 *   情形四：
 *     scenario4_approved_unitClosed    — 个体工商户，单位注销（INDIVIDUAL_UNIT_CLOSED）  → ELIG
 *     scenario4_approved_stopProduction — 个体工商户，停产停业（INDIVIDUAL_STOP_PRODUCTION）→ ELIG
 *
 *   通用拒绝：
 *     rejected_lastInsuredNotShenzhen  — 最后参保地非深圳                                → NOT-ELIG
 *     rejected_domicileNotShenzhen     — 户籍地非深圳                                    → NOT-ELIG
 *
 *   多段参保时间线：
 *     timeline_multiRecord_approved         — 两段参保记录，最新段深圳且合同期满，累计≥12月    → ELIG
 *     timeline_multiRecord_latestVoluntaryReject — 最新段主动辞职，即使历史段合规也应拒绝     → NOT-ELIG
 *
 *   延迟退休动态计算：
 *     scenario3_approved_dynamicRetirement_male    — 男性1966-06生，动态法定退休2026-10，距今<5年 → ELIG（情形三）
 *     rejected_alreadyRetired_female55             — 女干部1970-06生，法定退休2025-07，申请时已退休 → NOT-ELIG
 * </pre>
 */
class UnemploymentEligibilityTest {

    // ──────────────────────────────────────────────────────────
    // 辅助方法
    // ──────────────────────────────────────────────────────────

    /** 从会话工作内存中收集所有 EligibilityResult 对象 */
    private static List<EligibilityResult> results(RuleEngine re) {
        return re.session().getObjects().stream()
                .filter(o -> o instanceof EligibilityResult)
                .map(o -> (EligibilityResult) o)
                .toList();
    }

    /** 断言：工作内存中有且仅有一个以 "-ELIG" 结尾的结果（APPROVED） */
    private static EligibilityResult assertApproved(RuleEngine re, String applicantId) {
        List<EligibilityResult> all = results(re);
        assertEquals(1, all.size(),
                "Expected exactly 1 EligibilityResult, but got: " + all.size());
        EligibilityResult r = all.get(0);
        assertEquals(applicantId + "-ELIG", r.getId(),
                "Expected APPROVED result id");
        assertTrue(r.getRejectReason() == null || r.getRejectReason().isEmpty(),
                "APPROVED result must not carry a reject reason");
        return r;
    }

    /** 断言：工作内存中有且仅有一个以 "-NOT-ELIG" 结尾的结果（REJECTED） */
    private static EligibilityResult assertRejected(RuleEngine re, String applicantId) {
        List<EligibilityResult> all = results(re);
        assertEquals(1, all.size(),
                "Expected exactly 1 EligibilityResult, but got: " + all.size());
        EligibilityResult r = all.get(0);
        assertEquals(applicantId + "-NOT-ELIG", r.getId(),
                "Expected REJECTED result id");
        assertNotNull(r.getRejectReason(), "REJECTED result must carry a reject reason");
        assertFalse(r.getRejectReason().isBlank(), "REJECTED result must carry a non-blank reject reason");
        return r;
    }

    /**
     * 快捷方法：单条参保记录场景。
     * 自动构建 UnemploymentTimeline（单记录，stopDate=今天），插入所有事实并触发规则。
     */
    private static RuleEngine runApplicant(UnemployedPerson person,
                                           InsuranceRecord ins,
                                           EmployerUnit unit,
                                           UnemploymentRegistration reg,
                                           TerminationMaterial mat,
                                           BankAccount bank) {
        RuleEngine re = OntologyRuleVersionManager.getEngine("unemployment", "1.0");
        re.session().insert(new UnemploymentPolicy());
        re.session().insert(person);
        re.session().insert(ins);
        // 自动从单条记录构建时间线
        re.session().insert(new UnemploymentTimeline(person.getId(), List.of(ins)));
        re.session().insert(unit);
        re.session().insert(reg);
        re.session().insert(mat);
        re.session().insert(bank);
        re.fire();
        return re;
    }

    /**
     * 快捷方法：多条参保记录场景，显式传入时间线。
     * 调用方负责构造 UnemploymentTimeline 并将所有记录提前插入工作内存。
     */
    private static RuleEngine runApplicantWithTimeline(UnemployedPerson person,
                                                       List<InsuranceRecord> records,
                                                       UnemploymentTimeline timeline,
                                                       EmployerUnit unit,
                                                       UnemploymentRegistration reg,
                                                       TerminationMaterial mat,
                                                       BankAccount bank) {
        RuleEngine re = OntologyRuleVersionManager.getEngine("unemployment", "1.0");
        re.session().insert(new UnemploymentPolicy());
        re.session().insert(person);
        records.forEach(r -> re.session().insert(r));
        re.session().insert(timeline);
        re.session().insert(unit);
        re.session().insert(reg);
        re.session().insert(mat);
        re.session().insert(bank);
        re.fire();
        return re;
    }

    // ──────────────────────────────────────────────────────────
    // 情形一：企业职工，缴费 ≥ 12 月
    // ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("情形一 APPROVED — 企业职工，合同期满，缴费36月")
    void scenario1_approved() {
        String id = "440101199001011234";
        RuleEngine re = runApplicant(
            new UnemployedPerson(id, "ZhangSan", true, false),
            new InsuranceRecord(id, 36, 18, true, StopReason.CONTRACT_EXPIRED),
            new EmployerUnit(id, "91440300MA5XXXXX1A", "ABC Tech Co.", UnitType.ENTERPRISE),
            new UnemploymentRegistration(id, true, "2024-03-01"),
            new TerminationMaterial(id, MaterialType.CONTRACT_TERMINATION_NOTICE, "2024-02-28", "ABC Tech Co."),
            new BankAccount(id, "6222021234567890", "CCB", "ZhangSan")
        );
        assertApproved(re, id);
    }

    @Test
    @DisplayName("情形一 REJECTED — 企业职工，本人主动辞职（自愿停保）")
    void scenario1_rejected_voluntaryResign() {
        String id = "440101199001012345";
        RuleEngine re = runApplicant(
            new UnemployedPerson(id, "LiSi", true, false),
            new InsuranceRecord(id, 24, 12, true, StopReason.VOLUNTARY_RESIGN),
            new EmployerUnit(id, "91440300MA5XXXXX2B", "DEF Trading Co.", UnitType.ENTERPRISE),
            new UnemploymentRegistration(id, true, "2024-03-05"),
            new TerminationMaterial(id, MaterialType.COMMITMENT_LETTER, "2024-03-01", ""),
            new BankAccount(id, "6222029876543210", "ICBC", "LiSi")
        );
        EligibilityResult r = assertRejected(re, id);
        assertTrue(r.getRejectReason().contains("voluntary-stop"),
                "Reject reason should mention voluntary-stop, was: " + r.getRejectReason());
    }

    // ──────────────────────────────────────────────────────────
    // 情形二：缴费不满 12 月但有剩余领取期限
    // ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("情形二 APPROVED — 企业职工，单位裁员（Article 41），缴费8月有剩余4月")
    void scenario2_approved() {
        String id = "440101199001013456";
        RuleEngine re = runApplicant(
            new UnemployedPerson(id, "WangWu", true, false),
            new InsuranceRecord(id, 8, 4, true, StopReason.UNIT_LAYOFF_ARTICLE41),
            new EmployerUnit(id, "91440300MA5XXXXX3C", "XYZ Manufacturing", UnitType.ENTERPRISE),
            new UnemploymentRegistration(id, true, "2024-04-10"),
            new TerminationMaterial(id, MaterialType.CONTRACT_TERMINATION_NOTICE, "2024-04-01", "XYZ Manufacturing"),
            new BankAccount(id, "6222025555666677", "ABC", "WangWu")
        );
        assertApproved(re, id);
    }

    @Test
    @DisplayName("情形二 REJECTED — 缴费不满12月且无剩余领取期限")
    void scenario2_rejected_noRemainingMonths() {
        String id = "440101199001013456";
        RuleEngine re = runApplicant(
            new UnemployedPerson(id, "WangWu", true, false),
            new InsuranceRecord(id, 8, 0, true, StopReason.UNIT_LAYOFF_ARTICLE41),  // remainingMonths=0
            new EmployerUnit(id, "91440300MA5XXXXX3C", "XYZ Manufacturing", UnitType.ENTERPRISE),
            new UnemploymentRegistration(id, true, "2024-04-10"),
            new TerminationMaterial(id, MaterialType.CONTRACT_TERMINATION_NOTICE, "2024-04-01", "XYZ Manufacturing"),
            new BankAccount(id, "6222025555666677", "ABC", "WangWu")
        );
        EligibilityResult r = assertRejected(re, id);
        assertTrue(r.getRejectReason().contains("no-remaining-months"),
                "Reject reason should mention no-remaining-months, was: " + r.getRejectReason());
    }

    // ──────────────────────────────────────────────────────────
    // 情形三：临近退休人员（距法定退休年龄不足5年）
    // ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("情形三 APPROVED — 临近退休，事业单位，合同期满，缴费240月")
    void scenario3_approved() {
        String id = "440101196501014567";
        RuleEngine re = runApplicant(
            new UnemployedPerson(id, "ZhaoLiu", true, true),   // nearRetirement=true
            new InsuranceRecord(id, 240, 24, true, StopReason.CONTRACT_EXPIRED),
            new EmployerUnit(id, "12440300000XXXXX4D", "SZ Public Institute", UnitType.ENTERPRISE),
            new UnemploymentRegistration(id, true, "2025-01-10"),
            new TerminationMaterial(id, MaterialType.CONTRACT_TERMINATION_NOTICE, "2025-01-01", "SZ Public Institute"),
            new BankAccount(id, "6222021111222233", "Bank of China", "ZhaoLiu")
        );
        assertApproved(re, id);
    }

    @Test
    @DisplayName("情形三 REJECTED — 临近退休，但未办失业登记")
    void scenario3_rejected_noRegistration() {
        String id = "440101196501014567";
        RuleEngine re = runApplicant(
            new UnemployedPerson(id, "ZhaoLiu", true, true),   // nearRetirement=true
            new InsuranceRecord(id, 240, 24, true, StopReason.CONTRACT_EXPIRED),
            new EmployerUnit(id, "12440300000XXXXX4D", "SZ Public Institute", UnitType.ENTERPRISE),
            new UnemploymentRegistration(id, false, null),      // registered=false
            new TerminationMaterial(id, MaterialType.CONTRACT_TERMINATION_NOTICE, "2025-01-01", "SZ Public Institute"),
            new BankAccount(id, "6222021111222233", "Bank of China", "ZhaoLiu")
        );
        EligibilityResult r = assertRejected(re, id);
        assertTrue(r.getRejectReason().contains("registration-invalid"),
                "Reject reason should mention registration-invalid, was: " + r.getRejectReason());
    }

    // ──────────────────────────────────────────────────────────
    // 情形四：个体工商户业主
    // ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("情形四 APPROVED — 个体工商户，单位注销（INDIVIDUAL_UNIT_CLOSED）")
    void scenario4_approved_unitClosed() {
        String id = "440101198001015678";
        RuleEngine re = runApplicant(
            new UnemployedPerson(id, "ChenQi", true, false),
            new InsuranceRecord(id, 60, 12, true, StopReason.INDIVIDUAL_UNIT_CLOSED),
            new EmployerUnit(id, "92440300MA5XXXXX5E", "Chen Catering", UnitType.INDIVIDUAL_BUSINESS),
            new UnemploymentRegistration(id, true, "2024-06-15"),
            new TerminationMaterial(id, MaterialType.UNIT_CLOSURE_NOTICE, "2024-06-01", "SAMR Shenzhen"),
            new BankAccount(id, "6222023344556677", "CMB", "ChenQi")
        );
        assertApproved(re, id);
    }

    @Test
    @DisplayName("情形四 APPROVED — 个体工商户，停产停业（INDIVIDUAL_STOP_PRODUCTION）")
    void scenario4_approved_stopProduction() {
        String id = "440101198001015678";
        RuleEngine re = runApplicant(
            new UnemployedPerson(id, "ChenQi", true, false),
            new InsuranceRecord(id, 60, 12, true, StopReason.INDIVIDUAL_STOP_PRODUCTION),
            new EmployerUnit(id, "92440300MA5XXXXX5E", "Chen Catering", UnitType.INDIVIDUAL_BUSINESS),
            new UnemploymentRegistration(id, true, "2024-06-15"),
            new TerminationMaterial(id, MaterialType.STOP_PRODUCTION_NOTICE, "2024-06-01", "SAMR Shenzhen"),
            new BankAccount(id, "6222023344556677", "CMB", "ChenQi")
        );
        assertApproved(re, id);
    }

    // ──────────────────────────────────────────────────────────
    // 通用拒绝场景
    // ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("REJECTED — 最后参保地非深圳")
    void rejected_lastInsuredNotShenzhen() {
        String id = "440101199001016789";
        RuleEngine re = runApplicant(
            new UnemployedPerson(id, "LinBa", true, false),
            new InsuranceRecord(id, 18, 9, false, StopReason.CONTRACT_EXPIRED),  // lastInsuredShenzhen=false
            new EmployerUnit(id, "91440100MA5XXXXX6F", "GZ Tech Co.", UnitType.ENTERPRISE),
            new UnemploymentRegistration(id, true, "2024-07-20"),
            new TerminationMaterial(id, MaterialType.CONTRACT_TERMINATION_NOTICE, "2024-07-10", "GZ Tech Co."),
            new BankAccount(id, "6222027788990011", "PAB", "LinBa")
        );
        EligibilityResult r = assertRejected(re, id);
        assertTrue(r.getRejectReason().contains("last-insured-not-shenzhen"),
                "Reject reason should mention last-insured-not-shenzhen, was: " + r.getRejectReason());
    }

    @Test
    @DisplayName("REJECTED — 户籍地非深圳")
    void rejected_domicileNotShenzhen() {
        String id = "440101199001019999";
        RuleEngine re = runApplicant(
            new UnemployedPerson(id, "WaiDi", false, false),   // domicileShenzhen=false
            new InsuranceRecord(id, 24, 12, true, StopReason.CONTRACT_EXPIRED),
            new EmployerUnit(id, "91440300MA5XXXXX9Z", "Some Co.", UnitType.ENTERPRISE),
            new UnemploymentRegistration(id, true, "2024-08-01"),
            new TerminationMaterial(id, MaterialType.CONTRACT_TERMINATION_NOTICE, "2024-07-31", "Some Co."),
            new BankAccount(id, "6222020000111122", "BOC", "WaiDi")
        );
        EligibilityResult r = assertRejected(re, id);
        assertTrue(r.getRejectReason().contains("domicile-not-shenzhen"),
                "Reject reason should mention domicile-not-shenzhen, was: " + r.getRejectReason());
    }

    // ──────────────────────────────────────────────────────────
    // 时间线核心场景：多段参保记录，规则应以最新一段为判定依据
    // ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("时间线 APPROVED — 两段参保记录，最新段在深圳且合同期满，累计≥12月")
    void timeline_multiRecord_approved() {
        String id = "440101199001020001";
        // 第一段：2018年在外省参保8个月，本人主动辞职
        InsuranceRecord rec1 = new InsuranceRecord(
            id + "-INS-1", id, 8, 0, false,
            StopReason.VOLUNTARY_RESIGN,
            LocalDate.of(2019, 3, 31)
        );
        // 第二段（最新）：2022年在深圳参保36个月，合同期满
        InsuranceRecord rec2 = new InsuranceRecord(
            id + "-INS-2", id, 36, 18, true,
            StopReason.CONTRACT_EXPIRED,
            LocalDate.of(2025, 6, 30)
        );
        // 时间线自动以 rec2（stopDate 最大）为 latestRecord，totalMonths=44
        UnemploymentTimeline timeline = new UnemploymentTimeline(id, List.of(rec1, rec2));

        // 验证时间线预计算结果
        assertEquals(rec2, timeline.getLatestRecord(), "latestRecord 应为 stopDate 最大的 rec2");
        assertEquals(44, timeline.getTotalMonths(),    "totalMonths 应为 8+36=44");

        RuleEngine re = runApplicantWithTimeline(
            new UnemployedPerson(id, "MultiRecordUser", true, false),
            List.of(rec1, rec2),
            timeline,
            new EmployerUnit(id, "91440300MA5XXXXXMR", "SZ Future Tech", UnitType.ENTERPRISE),
            new UnemploymentRegistration(id, true, "2025-07-10"),
            new TerminationMaterial(id, MaterialType.CONTRACT_TERMINATION_NOTICE, "2025-06-30", "SZ Future Tech"),
            new BankAccount(id, "6222020001000101", "CMB", "MultiRecordUser")
        );
        EligibilityResult r = assertApproved(re, id);
        assertEquals("ELIGIBLE_SCENARIO_1", r.getScenarioCode(),
                "应命中情形一（累计缴费44月≥12，最新段深圳合同期满）");
    }

    @Test
    @DisplayName("时间线 REJECTED — 最新段为主动辞职，即使历史段在深圳也应拒绝")
    void timeline_multiRecord_latestVoluntaryReject() {
        String id = "440101199001020002";
        // 第一段（较早）：深圳，合同期满，缴费24月
        InsuranceRecord rec1 = new InsuranceRecord(
            id + "-INS-1", id, 24, 12, true,
            StopReason.CONTRACT_EXPIRED,
            LocalDate.of(2022, 12, 31)
        );
        // 第二段（最新）：深圳，但本人主动辞职
        InsuranceRecord rec2 = new InsuranceRecord(
            id + "-INS-2", id, 6, 0, true,
            StopReason.VOLUNTARY_RESIGN,
            LocalDate.of(2025, 8, 31)
        );
        UnemploymentTimeline timeline = new UnemploymentTimeline(id, List.of(rec1, rec2));

        assertEquals(rec2, timeline.getLatestRecord(), "latestRecord 应为 stopDate 最大的 rec2");

        RuleEngine re = runApplicantWithTimeline(
            new UnemployedPerson(id, "VolunteerQuitter", true, false),
            List.of(rec1, rec2),
            timeline,
            new EmployerUnit(id, "91440300MA5XXXXXVQ", "SZ Old Corp", UnitType.ENTERPRISE),
            new UnemploymentRegistration(id, true, "2025-09-01"),
            new TerminationMaterial(id, MaterialType.COMMITMENT_LETTER, "2025-08-31", ""),
            new BankAccount(id, "6222020002000202", "ICBC", "VolunteerQuitter")
        );
        EligibilityResult r = assertRejected(re, id);
        assertTrue(r.getRejectReason().contains("voluntary-stop"),
                "最新段主动辞职，应拒绝并说明 voluntary-stop，was: " + r.getRejectReason());
    }

    // ──────────────────────────────────────────────────────────
    // 延迟退休动态计算场景
    // ──────────────────────────────────────────────────────────

    /**
     * 延迟退休动态计算：男性，1966-06-01 生。
     *
     * <p>计算过程：
     * <ul>
     *   <li>起算门槛：1965-01-01（男性）</li>
     *   <li>距门槛自然月数：17 月（1965-01 → 1966-06）</li>
     *   <li>延迟月数：floor(17 / 4) = 4 月</li>
     *   <li>法定退休月数：720 + 4 = 724 月（60 岁 4 个月）</li>
     *   <li>法定退休日期：1966-06-01 + 724 月 = 2026-10-01</li>
     *   <li>参考日期（测试当天）：2026-03-05 → 距退休 ≈ 7 月 ≤ 60 月（5 年）→ nearRetirement = true</li>
     * </ul>
     * </p>
     */
    @Test
    @DisplayName("情形三 APPROVED — 延迟退休动态计算：男性1966-06生，法定退休60y4m，距今<5年")
    void scenario3_approved_dynamicRetirement_male() {
        String id = "440101196606015678";
        // birthday 传入，gender="male" → policy.isNearRetirement 动态判定临近退休
        RuleEngine re = runApplicant(
            new UnemployedPerson(id, "DelayRetireMale", true,
                    LocalDate.of(1966, 6, 1), "male"),
            new InsuranceRecord(id, 240, 24, true, StopReason.CONTRACT_EXPIRED),
            new EmployerUnit(id, "91440300MA5XXXXXDR", "SZ Tech Corp", UnitType.ENTERPRISE),
            new UnemploymentRegistration(id, true, "2026-01-10"),
            new TerminationMaterial(id, MaterialType.CONTRACT_TERMINATION_NOTICE, "2025-12-31", "SZ Tech Corp"),
            new BankAccount(id, "6222021966060112", "CCB", "DelayRetireMale")
        );
        EligibilityResult r = assertApproved(re, id);
        assertEquals("ELIGIBLE_SCENARIO_3", r.getScenarioCode(),
                "应命中情形三（动态延迟退休：法定退休 2026-10，距今<5年）");
    }

    /**
     * 延迟退休动态计算：女性干部岗，1970-06-01 生。
     *
     * <p>计算过程：
     * <ul>
     *   <li>起算门槛：1970-01-01（female55）</li>
     *   <li>距门槛自然月数：5 月（1970-01 → 1970-06）</li>
     *   <li>延迟月数：floor(5 / 4) = 1 月</li>
     *   <li>法定退休月数：660 + 1 = 661 月（55 岁 1 个月）</li>
     *   <li>法定退休日期：1970-06-01 + 661 月 = 2025-07-01</li>
     *   <li>参考日期：2026-03-05 → 已超过退休日期 → isRetired=true → 不得申领失业保险金</li>
     * </ul>
     * </p>
     */
    @Test
    @DisplayName("REJECTED — 延迟退休动态计算：女干部1970-06生，法定退休55y1m，已过退休日不得申领")
    void rejected_alreadyRetired_female55() {
        String id = "440101197006015679";
        // 已超过法定退休日 → isRetired=true → 四种情形均无法命中 → 兜底拒绝 already-retired
        RuleEngine re = runApplicant(
            new UnemployedPerson(id, "AlreadyRetiredFemale55", true,
                    LocalDate.of(1970, 6, 1), "female55"),
            new InsuranceRecord(id, 180, 18, true, StopReason.CONTRACT_EXPIRED),
            new EmployerUnit(id, "91440300MA5XXXXXF5", "SZ Finance Ltd", UnitType.ENTERPRISE),
            new UnemploymentRegistration(id, true, "2026-01-15"),
            new TerminationMaterial(id, MaterialType.CONTRACT_TERMINATION_NOTICE, "2025-12-31", "SZ Finance Ltd"),
            new BankAccount(id, "6222021970060111", "BOC", "AlreadyRetiredFemale55")
        );
        EligibilityResult r = assertRejected(re, id);
        assertTrue(r.getRejectReason().contains("already-retired"),
                "已过法定退休日，应拒绝并说明 already-retired，was: " + r.getRejectReason());
    }
}
