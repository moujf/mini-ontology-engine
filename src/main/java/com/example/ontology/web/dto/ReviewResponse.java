package com.example.ontology.web.dto;

import java.util.List;

/**
 * /api/review 接口的完整响应体，包含三个阶段的数据：
 * <ol>
 *   <li>applicants  — 录入阶段：所有申请人快照</li>
 *   <li>engineLog   — 规则触发阶段：每条规则命中的日志行</li>
 *   <li>results     — 审查结论阶段：每位申请人的最终结论</li>
 * </ol>
 */
public class ReviewResponse {
    private List<ApplicantSnapshot> applicants;
    private List<String> engineLog;
    private List<EligibilityResultDto> results;
    private String ruleVersion;

    public List<ApplicantSnapshot> getApplicants() { return applicants; }
    public void setApplicants(List<ApplicantSnapshot> v) { this.applicants = v; }

    public List<String> getEngineLog() { return engineLog; }
    public void setEngineLog(List<String> v) { this.engineLog = v; }

    public List<EligibilityResultDto> getResults() { return results; }
    public void setResults(List<EligibilityResultDto> v) { this.results = v; }

    public String getRuleVersion() { return ruleVersion; }
    public void setRuleVersion(String v) { this.ruleVersion = v; }
}
