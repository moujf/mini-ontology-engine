package com.example.ontology.web.service;

import com.example.ontology.engine.OntologyRuleVersionManager;
import com.example.ontology.engine.RuleEngine;
import com.example.ontology.model.*;
import com.example.ontology.model.EmployerUnit.UnitType;
import com.example.ontology.model.InsuranceRecord.StopReason;
import com.example.ontology.model.TerminationMaterial.MaterialType;
import com.example.ontology.web.dto.ApplicantRequest;
import com.example.ontology.web.dto.ApplicantSnapshot;
import com.example.ontology.web.dto.EligibilityResultDto;
import com.example.ontology.web.dto.ReviewResponse;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 审查服务：封装录入 → 规则触发 → 结论输出的完整流程，
 * 并将 Drools 控制台输出捕获为结构化日志行，供前端展示。
 */
@Service
public class ReviewService {

    private static final String RULE_VERSION = "1.0";

    /**
     * 执行一次完整的示例审查，返回三阶段结构化数据。
     * <p>
     * 关键设计：每位申请人独立创建一个 KieSession，避免多人共享 session 时
     * 兜底拒绝规则（not EligibilityResult）被其他申请人的结果干扰。
     * </p>
     */
    public ReviewResponse runDemoReview() {
        return runDataset(buildDemoDataset());
    }

    /**
     * 对给定数据集执行完整三阶段审查。
     * 每位申请人独立 session，结果汇总后返回。
     */
    public ReviewResponse runDataset(List<ApplicantData> dataset) {

        // ── Phase 1: 录入快照 ─────────────────────────────────────
        List<ApplicantSnapshot> snapshots = dataset.stream()
                .map(this::toSnapshot)
                .toList();

        // ── Phase 2 + 3: 每人独立 session，捕获所有 stdout ──────────
        PrintStream originalOut = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream capture = new PrintStream(baos, true, StandardCharsets.UTF_8);

        List<EligibilityResultDto> results = new ArrayList<>();
        StringBuilder allLogs = new StringBuilder();

        System.setOut(capture);
        try {
            for (ApplicantData d : dataset) {
                // 每位申请人独立 session — 保证规则条件隔离
                baos.reset(); // 每人单独捕获，便于从日志解析命中规则名
                RuleEngine re = OntologyRuleVersionManager.getEngine("unemployment", RULE_VERSION);
                re.session().insert(d.person);
                re.session().insert(d.ins);
                re.session().insert(d.unit);
                re.session().insert(d.reg);
                re.session().insert(d.mat);
                re.session().insert(d.bank);
                re.fire();

                // 从本人的 stdout 解析命中的规则情形
                String personLog = baos.toString(StandardCharsets.UTF_8);
                allLogs.append(personLog);
                String detectedScenario = parseScenarioFromLog(personLog, d.scenario);

                // 提取本人结论
                for (Object o : re.session().getObjects()) {
                    if (o instanceof EligibilityResult er) {
                        boolean approved = er.getId().endsWith("-ELIG")
                                && !er.getId().endsWith("-NOT-ELIG");
                        String pid = er.getId()
                                .replace("-NOT-ELIG", "")
                                .replace("-ELIG", "");
                        EligibilityResultDto dto = new EligibilityResultDto();
                        dto.setApplicantId(pid);
                        dto.setApplicantName(d.person.getName());
                        dto.setApproved(approved);
                        dto.setRejectReason(er.getRejectReason());
                        dto.setScenario(detectedScenario);
                        if (approved) {
                            dto.setBankName(d.bank.getBankName());
                            dto.setAccountNo(d.bank.getAccountNo());
                            dto.setAccountName(d.bank.getAccountName());
                        }
                        results.add(dto);
                    }
                }
                re.session().dispose();
            }
        } finally {
            System.setOut(originalOut);
        }

        String rawLog = allLogs.toString();
        List<String> engineLog = Arrays.stream(rawLog.split("\\r?\\n"))
                .filter(l -> !l.isBlank())
                .toList();

        ReviewResponse resp = new ReviewResponse();
        resp.setApplicants(snapshots);
        resp.setEngineLog(engineLog);
        resp.setResults(results);
        resp.setRuleVersion(RULE_VERSION);
        return resp;
    }

    /**
     * 将前端提交的单条申请人数据转换为 ApplicantData 并执行审查。
     */
    public ReviewResponse submitApplicant(ApplicantRequest req) {
        InsuranceRecord.StopReason stopReason;
        try { stopReason = InsuranceRecord.StopReason.valueOf(req.getStopReason()); }
        catch (Exception e) { stopReason = InsuranceRecord.StopReason.VOLUNTARY_RESIGN; }

        EmployerUnit.UnitType unitType;
        try { unitType = EmployerUnit.UnitType.valueOf(req.getUnitType()); }
        catch (Exception e) { unitType = EmployerUnit.UnitType.ENTERPRISE; }

        TerminationMaterial.MaterialType materialType;
        try { materialType = TerminationMaterial.MaterialType.valueOf(req.getMaterialType()); }
        catch (Exception e) { materialType = TerminationMaterial.MaterialType.CONTRACT_TERMINATION_NOTICE; }

        String scenario = req.getScenario() != null && !req.getScenario().isBlank()
                ? req.getScenario() : "custom";
        String creditCode = req.getUnitCreditCode() != null ? req.getUnitCreditCode() : "";
        String regDate = req.getRegistrationDate() != null ? req.getRegistrationDate() : "";
        String termDate = req.getTerminationDate() != null ? req.getTerminationDate() : "";
        String issuingUnit = req.getIssuingUnit() != null ? req.getIssuingUnit() : "";

        ApplicantData data = new ApplicantData(
            scenario,
            new UnemployedPerson(req.getId(), req.getName(),
                    req.isDomicileShenzhen(), req.isNearRetirement()),
            new InsuranceRecord(req.getId(), req.getPaidMonths(),
                    req.getRemainingMonths(), req.isLastInsuredShenzhen(), stopReason),
            new EmployerUnit(req.getId(), creditCode, req.getUnitName(), unitType),
            new UnemploymentRegistration(req.getId(), req.isRegistered(), regDate),
            new TerminationMaterial(req.getId(), materialType, termDate, issuingUnit),
            new BankAccount(req.getId(), req.getAccountNo(), req.getBankName(), req.getAccountName())
        );
        return runDataset(List.of(data));
    }

    // ── helpers ──────────────────────────────────────────────────

    /**
     * 从单个申请人的规则引擎 stdout 中解析实际命中的情形名称。
     * <p>
     * DRL then 块输出格式：{@code [RULE][PASS] scenario-1 general-employee}
     * 若日志中未找到 PASS 行（说明走了拒绝兜底），则保留 fallback。
     * </p>
     */
    private String parseScenarioFromLog(String log, String fallback) {
        // 匹配 [RULE][PASS] 后面的内容，例如 "scenario-1 general-employee"
        for (String line : log.split("\\r?\\n")) {
            if (line.contains("[RULE][PASS]")) {
                String after = line.substring(line.indexOf("[RULE][PASS]") + "[RULE][PASS]".length()).trim();
                if (!after.isBlank()) return after;
            }
        }
        // 拒绝场景：fallback 若为 custom，改为 rejected
        return (fallback != null && !fallback.equals("custom")) ? fallback : "rejected";
    }

    private ApplicantSnapshot toSnapshot(ApplicantData d) {
        ApplicantSnapshot s = new ApplicantSnapshot();
        s.setId(d.person.getId());
        s.setName(d.person.getName());
        s.setDomicileShenzhen(d.person.isDomicileShenzhen());
        s.setNearRetirement(d.person.isNearRetirement());
        s.setPaidMonths(d.ins.getPaidMonths());
        s.setRemainingMonths(d.ins.getRemainingMonths());
        s.setLastInsuredShenzhen(d.ins.isLastInsuredShenzhen());
        s.setStopReason(d.ins.getStopReason().name());
        s.setUnitName(d.unit.getUnitName());
        s.setUnitType(d.unit.getUnitType().getCode());
        s.setUnitTypeLabel(d.unit.getUnitType().getLabel());
        s.setRegistered(d.reg.isRegistered());
        s.setRegistrationDate(d.reg.getRegistrationDate());
        s.setMaterialType(d.mat.getMaterialType().name());
        s.setTerminationDate(d.mat.getTerminationDate());
        s.setIssuingUnit(d.mat.getIssuingUnit());
        s.setBankName(d.bank.getBankName());
        s.setAccountNo(d.bank.getAccountNo());
        return s;
    }

    // ── demo dataset (mirrors MainApp) ────────────────────────────

    private List<ApplicantData> buildDemoDataset() {
        List<ApplicantData> list = new ArrayList<>();

        list.add(new ApplicantData("scenario-1 (approved)",
            new UnemployedPerson("440101199001011234", "ZhangSan", true, false),
            new InsuranceRecord("440101199001011234", 36, 18, true, StopReason.CONTRACT_EXPIRED),
            new EmployerUnit("440101199001011234", "91440300MA5XXXXX1A", "ABC Tech Co.", UnitType.ENTERPRISE),
            new UnemploymentRegistration("440101199001011234", true, "2024-03-01"),
            new TerminationMaterial("440101199001011234", MaterialType.CONTRACT_TERMINATION_NOTICE, "2024-02-28", "ABC Tech Co."),
            new BankAccount("440101199001011234", "6222021234567890", "CCB", "ZhangSan")
        ));

        list.add(new ApplicantData("scenario-1 (rejected - voluntary resign)",
            new UnemployedPerson("440101199001012345", "LiSi", true, false),
            new InsuranceRecord("440101199001012345", 24, 12, true, StopReason.VOLUNTARY_RESIGN),
            new EmployerUnit("440101199001012345", "91440300MA5XXXXX2B", "DEF Trading Co.", UnitType.ENTERPRISE),
            new UnemploymentRegistration("440101199001012345", true, "2024-03-05"),
            new TerminationMaterial("440101199001012345", MaterialType.COMMITMENT_LETTER, "2024-03-01", ""),
            new BankAccount("440101199001012345", "6222029876543210", "ICBC", "LiSi")
        ));

        list.add(new ApplicantData("scenario-2 (approved)",
            new UnemployedPerson("440101199001013456", "WangWu", true, false),
            new InsuranceRecord("440101199001013456", 8, 4, true, StopReason.UNIT_LAYOFF_ARTICLE41),
            new EmployerUnit("440101199001013456", "91440300MA5XXXXX3C", "XYZ Manufacturing", UnitType.ENTERPRISE),
            new UnemploymentRegistration("440101199001013456", true, "2024-04-10"),
            new TerminationMaterial("440101199001013456", MaterialType.CONTRACT_TERMINATION_NOTICE, "2024-04-01", "XYZ Manufacturing"),
            new BankAccount("440101199001013456", "6222025555666677", "ABC Bank", "WangWu")
        ));

        list.add(new ApplicantData("scenario-3 (approved)",
            new UnemployedPerson("440101196501014567", "ZhaoLiu", true, true),
            new InsuranceRecord("440101196501014567", 240, 24, true, StopReason.CONTRACT_EXPIRED),
            new EmployerUnit("440101196501014567", "12440300000XXXXX4D", "SZ Public Institute", UnitType.ENTERPRISE),
            new UnemploymentRegistration("440101196501014567", true, "2025-01-10"),
            new TerminationMaterial("440101196501014567", MaterialType.CONTRACT_TERMINATION_NOTICE, "2025-01-01", "SZ Public Institute"),
            new BankAccount("440101196501014567", "6222021111222233", "Bank of China", "ZhaoLiu")
        ));

        list.add(new ApplicantData("scenario-4 (approved)",
            new UnemployedPerson("440101198001015678", "ChenQi", true, false),
            new InsuranceRecord("440101198001015678", 60, 12, true, StopReason.INDIVIDUAL_UNIT_CLOSED),
            new EmployerUnit("440101198001015678", "92440300MA5XXXXX5E", "Chen Catering", UnitType.INDIVIDUAL_BUSINESS),
            new UnemploymentRegistration("440101198001015678", true, "2024-06-15"),
            new TerminationMaterial("440101198001015678", MaterialType.UNIT_CLOSURE_NOTICE, "2024-06-01", "SAMR Shenzhen"),
            new BankAccount("440101198001015678", "6222023344556677", "CMB", "ChenQi")
        ));

        list.add(new ApplicantData("rejected (last insured not Shenzhen)",
            new UnemployedPerson("440101199001016789", "LinBa", true, false),
            new InsuranceRecord("440101199001016789", 18, 9, false, StopReason.CONTRACT_EXPIRED),
            new EmployerUnit("440101199001016789", "91440100MA5XXXXX6F", "GZ Tech Co.", UnitType.ENTERPRISE),
            new UnemploymentRegistration("440101199001016789", true, "2024-07-20"),
            new TerminationMaterial("440101199001016789", MaterialType.CONTRACT_TERMINATION_NOTICE, "2024-07-10", "GZ Tech Co."),
            new BankAccount("440101199001016789", "6222027788990011", "PAB", "LinBa")
        ));

        return list;
    }

    /** 内部数据载体，将一位申请人的全部关联实体聚合在一起 */
    private record ApplicantData(
            String scenario,
            UnemployedPerson person,
            InsuranceRecord ins,
            EmployerUnit unit,
            UnemploymentRegistration reg,
            TerminationMaterial mat,
            BankAccount bank) {}
}
