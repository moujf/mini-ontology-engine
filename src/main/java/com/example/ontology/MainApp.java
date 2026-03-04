package com.example.ontology;

import com.example.ontology.action.ActionEngine;
import com.example.ontology.action.NotifyAction;
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

/**
 * 程序主入口 — 失业保险金申领资格审查演示。
 * <p>
 * 每位申请人对应 5 个关联实体，全部插入 Drools Session：
 * <ul>
 *   <li>{@link UnemployedPerson}           — 申请人（身份：姓名、证件号、户籍地）</li>
 *   <li>{@link InsuranceRecord}            — 失业保险参保记录（含缴费月数、停保原因）</li>
 *   <li>{@link EmployerUnit}              — 用人单位（含单位类型，判断申领情形）</li>
 *   <li>{@link UnemploymentRegistration}   — 失业登记信息</li>
 *   <li>{@link TerminationMaterial}        — 劳动关系终止材料</li>
 *   <li>{@link BankAccount}               — 银行账户信息（不参与规则校验）</li>
 * </ul>
 * 申领情形由规则引擎根据单位类型 + 缴费月数推断，无需外部传入。
 * </p>
 */
public class MainApp {

    public static void main(String[] args) {

        System.out.println("══════════════════════════════════════");
        System.out.println("   失业保险金申领资格审查系统  v1.0");
        System.out.println("══════════════════════════════════════");
        System.out.println("[PHASE 1] 录入申请人信息");

        RuleEngine re = OntologyRuleVersionManager.getEngine("unemployment", "1.0");

        // ── 情形一 ✅：ZhangSan — 企业职工，合同期满，缴费36月 ──────────
        insertApplicant(re,
            new UnemployedPerson("440101199001011234", "ZhangSan", true, false),
            new InsuranceRecord("440101199001011234", 36, 18, true, StopReason.CONTRACT_EXPIRED),
            new EmployerUnit("440101199001011234", "91440300MA5XXXXX1A", "ABC科技有限公司", UnitType.ENTERPRISE),
            new UnemploymentRegistration("440101199001011234", true, "2024-03-01"),
            new TerminationMaterial("440101199001011234",
                MaterialType.CONTRACT_TERMINATION_NOTICE, "2024-02-28", "ABC科技有限公司"),
            new BankAccount("440101199001011234", "6222021234567890", "中国建设银行", "ZhangSan")
        );

        // ── 情形一 ❌：LiSi — 企业职工，本人主动辞职 ──────────────────
        insertApplicant(re,
            new UnemployedPerson("440101199001012345", "LiSi", true, false),
            new InsuranceRecord("440101199001012345", 24, 12, true, StopReason.VOLUNTARY_RESIGN),
            new EmployerUnit("440101199001012345", "91440300MA5XXXXX2B", "DEF贸易有限公司", UnitType.ENTERPRISE),
            new UnemploymentRegistration("440101199001012345", true, "2024-03-05"),
            new TerminationMaterial("440101199001012345",
                MaterialType.COMMITMENT_LETTER, "2024-03-01", ""),
            new BankAccount("440101199001012345", "6222029876543210", "中国工商银行", "LiSi")
        );

        // ── 情形二 ✅：WangWu — 企业职工，单位裁员，缴费8月有剩余 ───────
        insertApplicant(re,
            new UnemployedPerson("440101199001013456", "WangWu", true, false),
            new InsuranceRecord("440101199001013456", 8, 4, true, StopReason.UNIT_LAYOFF_ARTICLE41),
            new EmployerUnit("440101199001013456", "91440300MA5XXXXX3C", "XYZ制造有限公司", UnitType.ENTERPRISE),
            new UnemploymentRegistration("440101199001013456", true, "2024-04-10"),
            new TerminationMaterial("440101199001013456",
                MaterialType.CONTRACT_TERMINATION_NOTICE, "2024-04-01", "XYZ制造有限公司"),
            new BankAccount("440101199001013456", "6222025555666677", "中国农业银行", "WangWu")
        );

        // ── 情形三 ✅：ZhaoLiu — 企业职工，临近退休（距退休年龄不足5年），缴费240月 ──
        insertApplicant(re,
            new UnemployedPerson("440101196501014567", "ZhaoLiu", true, true),
            new InsuranceRecord("440101196501014567", 240, 24, true, StopReason.CONTRACT_EXPIRED),
            new EmployerUnit("440101196501014567", "12440300000XXXXX4D", "ShenZhen Public Institute", UnitType.ENTERPRISE),
            new UnemploymentRegistration("440101196501014567", true, "2025-01-10"),
            new TerminationMaterial("440101196501014567",
                MaterialType.CONTRACT_TERMINATION_NOTICE, "2025-01-01", "ShenZhen Public Institute"),
            new BankAccount("440101196501014567", "6222021111222233", "Bank of China", "ZhaoLiu")
        );

        // ── 情形四 ✅：ChenQi — 个体工商户，单位注销 ────────────────────
        insertApplicant(re,
            new UnemployedPerson("440101198001015678", "ChenQi", true, false),
            new InsuranceRecord("440101198001015678", 60, 12, true, StopReason.INDIVIDUAL_UNIT_CLOSED),
            new EmployerUnit("440101198001015678", "92440300MA5XXXXX5E", "陈记餐饮店", UnitType.INDIVIDUAL_BUSINESS),
            new UnemploymentRegistration("440101198001015678", true, "2024-06-15"),
            new TerminationMaterial("440101198001015678",
                MaterialType.UNIT_CLOSURE_NOTICE, "2024-06-01", "深圳市市场监督管理局"),
            new BankAccount("440101198001015678", "6222023344556677", "招商银行", "ChenQi")
        );

        // ── 情形一 ❌：LinBa — 企业职工，最后参保地非深圳 ───────────────
        insertApplicant(re,
            new UnemployedPerson("440101199001016789", "LinBa", true, false),
            new InsuranceRecord("440101199001016789", 18, 9, false, StopReason.CONTRACT_EXPIRED),
            new EmployerUnit("440101199001016789", "91440100MA5XXXXX6F", "广州某科技公司", UnitType.ENTERPRISE),
            new UnemploymentRegistration("440101199001016789", true, "2024-07-20"),
            new TerminationMaterial("440101199001016789",
                MaterialType.CONTRACT_TERMINATION_NOTICE, "2024-07-10", "广州某科技公司"),
            new BankAccount("440101199001016789", "6222027788990011", "平安银行", "LinBa")
        );

        // ── 规则引擎触发 ─────────────────────────────────────────────
        System.out.println("--------------------------------------");
        System.out.println("[PHASE 2] 规则引擎执行");
        re.fire();

        // ── 动作引擎：输出每条审查结论（含银行账户） ─────────────────
        System.out.println("\n══════════════════════════════════════");
        System.out.println("           审 查 结 论");
        System.out.println("══════════════════════════════════════");
        ActionEngine ae = new ActionEngine();
        ae.add(new NotifyAction());
        java.util.Map<String, BankAccount> bankMap = new java.util.HashMap<>();
        for (Object o : re.session().getObjects()) {
            if (o instanceof BankAccount b) bankMap.put(b.getApplicantId(), b);
        }
        for (Object o : re.session().getObjects()) {
            if (o instanceof EligibilityResult e) {
                ae.fire(e);
                String pid = e.getId().replace("-ELIG", "").replace("-NOT-ELIG", "");
                BankAccount bank = bankMap.get(pid);
                if (bank != null && e.getId().endsWith("-ELIG")) {
                    System.out.println("  发放账户 : " + bank.getBankName()
                        + " " + bank.getAccountNo() + " (" + bank.getAccountName() + ")");
                }
            }
        }
    }

    /**
     * 将一位申请人的全部关联实体批量插入规则引擎 Session，并打印录入摘要。
     */
    private static void insertApplicant(RuleEngine re,
                                        UnemployedPerson person,
                                        InsuranceRecord ins,
                                        EmployerUnit unit,
                                        UnemploymentRegistration reg,
                                        TerminationMaterial mat,
                                        BankAccount bank) {
        System.out.println("--------------------------------------");
        System.out.println("[INSERT] 申请人  : " + person.getName() + " (" + person.getId() + ")");
        System.out.println("         户籍深圳: " + person.isDomicileShenzhen() + "  临近退休: " + person.isNearRetirement());
        System.out.println("         单位    : " + unit.getUnitName() + " [" + unit.getUnitType().getCode() + " " + unit.getUnitType().getLabel() + "]");
        System.out.println("         缴费月  : " + ins.getPaidMonths() + "  剩余月: " + ins.getRemainingMonths() + "  参保地深圳: " + ins.isLastInsuredShenzhen());
        System.out.println("         停保原因: " + ins.getStopReason());
        System.out.println("         失业登记: " + (reg.isRegistered() ? "有效 (" + reg.getRegistrationDate() + ")" : "无效"));
        System.out.println("         终止材料: " + mat.getMaterialType() + " 出具: " + mat.getIssuingUnit());
        System.out.println("         收款账户: " + bank.getBankName() + " " + bank.getAccountNo());
        re.session().insert(person);
        re.session().insert(ins);
        re.session().insert(unit);
        re.session().insert(reg);
        re.session().insert(mat);
        re.session().insert(bank);
    }
}
