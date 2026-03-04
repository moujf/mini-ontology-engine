package com.example.ontology.model;

/**
 * 失业保险申领政策参数单例实体。
 * <p>
 * 封装《深圳市失业保险金申领操作规程》中所有可变的数值型政策参数，
 * 使规则引擎中的 DRL 文件完全无硬编码数字，实现动态规则参数化。
 * </p>
 *
 * <h3>Palantir 本体论设计说明</h3>
 * <ul>
 *   <li>规则引擎启动时将此对象作为唯一单例插入工作内存</li>
 *   <li>DRL 规则通过 {@code $policy : UnemploymentPolicy()} 匹配并绑定</li>
 *   <li>所有阈值判断通过 getter 方法引用，政策调整时只需更改此对象属性</li>
 * </ul>
 *
 * <h3>参数说明</h3>
 * <pre>
 *   minContributionMonths  — 享受失业保险待遇的最低缴费月数（默认 12）
 *   maleRetirementAge      — 男性法定退休年龄（默认 60）
 *   femaleRetirementAge    — 女性法定退休年龄（默认 55）
 *   nearRetirementYears    — 临近退休判定年限（距退休不足 N 年，默认 5）
 * </pre>
 */
public class UnemploymentPolicy extends OntologyObject {

    /**
     * 构造默认政策参数（基于深圳市现行操作规程）。
     */
    public UnemploymentPolicy() {
        super("POLICY-CURRENT", "UnemploymentPolicy");
        setAttr("minContributionMonths", 12);
        setAttr("maleRetirementAge",     60);
        setAttr("femaleRetirementAge",   55);
        setAttr("nearRetirementYears",   5);
    }

    /**
     * 构造自定义政策参数（用于测试或未来政策调整）。
     *
     * @param minContributionMonths 最低缴费月数
     * @param maleRetirementAge     男性法定退休年龄
     * @param femaleRetirementAge   女性法定退休年龄
     * @param nearRetirementYears   临近退休判定年限
     */
    public UnemploymentPolicy(int minContributionMonths,
                               int maleRetirementAge,
                               int femaleRetirementAge,
                               int nearRetirementYears) {
        super("POLICY-CURRENT", "UnemploymentPolicy");
        setAttr("minContributionMonths", minContributionMonths);
        setAttr("maleRetirementAge",     maleRetirementAge);
        setAttr("femaleRetirementAge",   femaleRetirementAge);
        setAttr("nearRetirementYears",   nearRetirementYears);
    }

    /**
     * 享受失业保险待遇的最低缴费月数（一般情形一要求 ≥ 12）。
     *
     * @return 最低缴费月数
     */
    public int getMinContributionMonths() {
        return (int) getAttr("minContributionMonths");
    }

    /**
     * 男性法定退休年龄。
     *
     * @return 男性退休年龄（岁）
     */
    public int getMaleRetirementAge() {
        return (int) getAttr("maleRetirementAge");
    }

    /**
     * 女性法定退休年龄。
     *
     * @return 女性退休年龄（岁）
     */
    public int getFemaleRetirementAge() {
        return (int) getAttr("femaleRetirementAge");
    }

    /**
     * 临近退休判定年限：距法定退休年龄不足此年数即视为临近退休。
     *
     * @return 临近退休判定年限（年）
     */
    public int getNearRetirementYears() {
        return (int) getAttr("nearRetirementYears");
    }
}
