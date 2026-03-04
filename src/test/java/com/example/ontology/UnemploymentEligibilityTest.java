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
import com.example.ontology.model.UnemploymentRegistration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
 * 测试矩阵：
 *   scenario1_approved  — 情形一：企业职工，缴费≥12月，合同期满  → ELIG
 *   scenario1_rejected_voluntaryResign — 情形一失败：主动辞职       → NOT-ELIG, reject-reason 含 voluntary-stop
 *   scenario2_approved  — 情形二：缴费&lt;12月但有剩余期限，裁员   → ELIG
 *   scenario3_approved  — 情形三：临近退休，非个体工商户          → ELIG
 *   scenario3_rejected_noRegistration — 情形三失败：未办失业登记   → NOT-ELIG
 *   scenario4_approved  — 情形四：个体工商户，单位注销            → ELIG
 *   rejected_lastInsuredNotShenzhen — 最后参保地非深圳             → NOT-ELIG, reject-reason 含 last-insured-not-shenzhen
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

    /** 快捷方法：插入一位申请人的全部关联实体并触发规则 */
    private static RuleEngine runApplicant(UnemployedPerson person,
                                           InsuranceRecord ins,
                                           EmployerUnit unit,
                                           UnemploymentRegistration reg,
                                           TerminationMaterial mat,
                                           BankAccount bank) {
        RuleEngine re = OntologyRuleVersionManager.getEngine("unemployment", "1.0");
        re.session().insert(person);
        re.session().insert(ins);
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
}
