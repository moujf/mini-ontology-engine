package com.example.ontology.model;

import java.time.LocalDate;

/**
 * 失业保险申领政策参数单例实体。
 * <p>
 * 封装《深圳市失业保险金申领操作规程》中所有可变的数值型政策参数，
 * 使规则引擎中的 DRL 文件完全无硬编码数字，实现动态规则参数化。
 * </p>
 *
 * <h3>v2 — 延迟退休动态计算</h3>
 * <p>
 * 根据《国务院关于渐进式延迟法定退休年龄的办法》（2025年起施行），
 * 法定退休年龄不再是固定常数，而是基于出生年月的阶梯函数：
 * <ul>
 *   <li>男性：1965年1月起，每4个自然月延迟1个月，上限65岁（2044年1月）</li>
 *   <li>女性（原55岁岗位）：1970年1月起，每4个自然月延迟1个月，上限58岁（2043年1月）</li>
 *   <li>女性（原50岁岗位）：1975年1月起，每2个自然月延迟1个月，上限55岁（2034年12月）</li>
 * </ul>
 * 本实现使用"female55"和"female50"两类 gender 标识区分原岗位。
 * DRL 规则通过 {@code $policy.calculateStatutoryRetirementAge($p.getBirthday(), $p.getGender())}
 * 获取精确退休年龄月数，永不出现 60/55/50 等硬编码数字。
 * </p>
 *
 * <h3>Palantir 本体论设计说明</h3>
 * <ul>
 *   <li>规则引擎启动时将此对象作为唯一单例插入工作内存</li>
 *   <li>DRL 规则通过 {@code $policy : UnemploymentPolicy()} 匹配并绑定</li>
 *   <li>所有阈值判断通过方法引用，政策调整时只需更改此对象的算法</li>
 * </ul>
 *
 * <h3>参数说明</h3>
 * <pre>
 *   minContributionMonths  — 享受失业保险待遇的最低缴费月数（默认 12）
 *   nearRetirementYears    — 临近退休判定年限（距退休不足 N 年，默认 5）
 *   — maleRetirementAge / femaleRetirementAge 已废弃，由 calculateStatutoryRetirementAge 代替 —
 * </pre>
 */
public class UnemploymentPolicy extends OntologyObject {

    /**
     * 构造默认政策参数（基于深圳市现行操作规程 + 2025延迟退休方案）。
     */
    public UnemploymentPolicy() {
        super("POLICY-CURRENT", "UnemploymentPolicy");
        setAttr("minContributionMonths", 12);
        setAttr("nearRetirementYears",   5);
        // 保留以下字段供前端展示"基准退休年龄"（仅展示用，计算已由动态方法接管）
        setAttr("maleBaseRetirementAge",    60);
        setAttr("femaleBase55RetirementAge", 55);
        setAttr("femaleBase50RetirementAge", 50);
    }

    /**
     * 构造自定义政策参数（用于测试或未来政策调整）。
     *
     * @param minContributionMonths 最低缴费月数
     * @param nearRetirementYears   临近退休判定年限
     */
    public UnemploymentPolicy(int minContributionMonths, int nearRetirementYears) {
        super("POLICY-CURRENT", "UnemploymentPolicy");
        setAttr("minContributionMonths", minContributionMonths);
        setAttr("nearRetirementYears",   nearRetirementYears);
        setAttr("maleBaseRetirementAge",     60);
        setAttr("femaleBase55RetirementAge", 55);
        setAttr("femaleBase50RetirementAge", 50);
    }

    /**
     * 向后兼容的四参数构造器（旧代码兼容，nearRetirementYears 取 maleRetirementAge 参数位置无意义，
     * 改为只取 minContributionMonths 和 nearRetirementYears）。
     *
     * @deprecated 请改用 {@link #UnemploymentPolicy(int, int)}
     */
    @Deprecated
    public UnemploymentPolicy(int minContributionMonths,
                               int maleRetirementAge,
                               int femaleRetirementAge,
                               int nearRetirementYears) {
        super("POLICY-CURRENT", "UnemploymentPolicy");
        setAttr("minContributionMonths",     minContributionMonths);
        setAttr("nearRetirementYears",       nearRetirementYears);
        setAttr("maleBaseRetirementAge",     maleRetirementAge);
        setAttr("femaleBase55RetirementAge", femaleRetirementAge);
        setAttr("femaleBase50RetirementAge", 50);
    }

    // ────────────────────────────────────────────────────────────
    // 核心：延迟退休动态计算
    // ────────────────────────────────────────────────────────────

    /**
     * 根据出生日期和性别/岗位类别，计算法定退休年龄（精确到月，以"月数"表示）。
     *
     * <h4>延迟退休规则（2025年起施行）</h4>
     * <ul>
     *   <li><b>male</b>（男性，原60岁）：
     *       1965年1月起出生每4个自然月延迟1个月，最高延至65岁（2044年1月）</li>
     *   <li><b>female55</b>（女干部/管理岗，原55岁）：
     *       1970年1月起出生每4个自然月延迟1个月，最高延至58岁（2043年1月）</li>
     *   <li><b>female50</b>（女工人/普通岗，原50岁）：
     *       1975年1月起出生每2个自然月延迟1个月，最高延至55岁（2034年12月）</li>
     * </ul>
     *
     * @param birthday 出生日期
     * @param gender   性别/岗位标识：{@code "male"}、{@code "female55"}、{@code "female50"}；
     *                 传 {@code null} 或 {@code "M"} 按 male 处理，{@code "F"} 按 female55 处理
     * @return 法定退休年龄，单位：月（例如 60岁=720，63岁4个月=760）
     */
    public int calculateStatutoryRetirementAge(LocalDate birthday, String gender) {
        if (birthday == null) {
            // 无出生日期时回退到基准年龄
            return resolveBaseAgeMonths(gender);
        }

        String g = normalizeGender(gender);
        switch (g) {
            case "male":     return calcMaleRetirementMonths(birthday);
            case "female55": return calcFemale55RetirementMonths(birthday);
            case "female50": return calcFemale50RetirementMonths(birthday);
            default:         return calcMaleRetirementMonths(birthday);
        }
    }

    /**
     * 便捷版：直接判断某人在给定参考日期时是否已达到法定退休年龄。
     *
     * @param birthday      出生日期
     * @param gender        性别/岗位标识
     * @param referenceDate 参考日期（通常为申请日）
     * @return true 表示已退休或超龄
     */
    public boolean isRetired(LocalDate birthday, String gender, LocalDate referenceDate) {
        if (birthday == null || referenceDate == null) return false;
        int statutoryMonths = calculateStatutoryRetirementAge(birthday, gender);
        LocalDate retirementDate = birthday.plusMonths(statutoryMonths);
        return !referenceDate.isBefore(retirementDate);
    }

    /**
     * 判断某人在给定参考日期时是否临近退休（距退休不足 nearRetirementYears 年）。
     * <p>
     * 这是 DRL 规则中 {@code isNearRetirement($policy)} 的底层实现，
     * 确保判断逻辑完全封装在政策对象中，DRL 永远不出现具体年龄数字。
     * </p>
     *
     * @param birthday      出生日期
     * @param gender        性别/岗位标识
     * @param referenceDate 参考日期（通常为申请日）
     * @return true 表示临近退休（距退休日期不足 nearRetirementYears 年且尚未退休）
     */
    public boolean isNearRetirement(LocalDate birthday, String gender, LocalDate referenceDate) {
        if (birthday == null || referenceDate == null) return false;
        int statutoryMonths = calculateStatutoryRetirementAge(birthday, gender);
        LocalDate retirementDate = birthday.plusMonths(statutoryMonths);
        if (!referenceDate.isBefore(retirementDate)) return false; // 已退休
        long monthsToRetirement = referenceDate.until(retirementDate, java.time.temporal.ChronoUnit.MONTHS);
        return monthsToRetirement <= (long) getNearRetirementYears() * 12;
    }

    // ────────────────────────────────────────────────────────────
    // 内部实现：各类人员阶梯计算
    // ────────────────────────────────────────────────────────────

    /** 男性（原60岁）：1965-01起每4月延1月，上限65岁 */
    private int calcMaleRetirementMonths(LocalDate birthday) {
        final LocalDate threshold = LocalDate.of(1965, 1, 1);
        final int baseMonths = 720;   // 60 * 12
        final int maxMonths  = 780;   // 65 * 12
        final int stepEvery  = 4;     // 每4个自然月延迟1个月
        return calcDelayedRetirementMonths(birthday, threshold, baseMonths, maxMonths, stepEvery);
    }

    /** 女干部/管理岗（原55岁）：1970-01起每4月延1月，上限58岁 */
    private int calcFemale55RetirementMonths(LocalDate birthday) {
        final LocalDate threshold = LocalDate.of(1970, 1, 1);
        final int baseMonths = 660;   // 55 * 12
        final int maxMonths  = 696;   // 58 * 12
        final int stepEvery  = 4;
        return calcDelayedRetirementMonths(birthday, threshold, baseMonths, maxMonths, stepEvery);
    }

    /** 女工人/普通岗（原50岁）：1975-01起每2月延1月，上限55岁 */
    private int calcFemale50RetirementMonths(LocalDate birthday) {
        final LocalDate threshold = LocalDate.of(1975, 1, 1);
        final int baseMonths = 600;   // 50 * 12
        final int maxMonths  = 660;   // 55 * 12
        final int stepEvery  = 2;
        return calcDelayedRetirementMonths(birthday, threshold, baseMonths, maxMonths, stepEvery);
    }

    /**
     * 通用阶梯延迟计算：
     * birthday < threshold → 返回 baseMonths；
     * 否则，按 (birthday - threshold) 所经历的每 stepEvery 个自然月延迟 1 个月，
     * 结果不超过 maxMonths。
     */
    private int calcDelayedRetirementMonths(LocalDate birthday,
                                             LocalDate threshold,
                                             int baseMonths,
                                             int maxMonths,
                                             int stepEvery) {
        if (birthday.isBefore(threshold)) return baseMonths;
        long monthsAfterThreshold = threshold.until(birthday, java.time.temporal.ChronoUnit.MONTHS);
        int delayMonths = (int) (monthsAfterThreshold / stepEvery);
        return Math.min(baseMonths + delayMonths, maxMonths);
    }

    private String normalizeGender(String gender) {
        if (gender == null) return "male";
        switch (gender.toLowerCase()) {
            case "male": case "m": case "男": return "male";
            case "female55": case "f55": return "female55";
            case "female50": case "f50": return "female50";
            case "female": case "f": case "女": return "female55"; // 默认按干部岗
            default: return "male";
        }
    }

    private int resolveBaseAgeMonths(String gender) {
        String g = normalizeGender(gender);
        switch (g) {
            case "female55": return 660;
            case "female50": return 600;
            default:         return 720;
        }
    }

    // ────────────────────────────────────────────────────────────
    // Getters
    // ────────────────────────────────────────────────────────────

    /** 享受失业保险待遇的最低缴费月数（一般情形一要求 ≥ 12）。 */
    public int getMinContributionMonths() {
        return (int) getAttr("minContributionMonths");
    }

    /** 临近退休判定年限：距法定退休年龄不足此年数即视为临近退休。 */
    public int getNearRetirementYears() {
        return (int) getAttr("nearRetirementYears");
    }

    /** 男性基准退休年龄（岁，不含延迟），仅供展示用。 */
    public int getMaleBaseRetirementAge() {
        return (int) getAttr("maleBaseRetirementAge");
    }

    /** 女性（原55岁岗位）基准退休年龄（岁），仅供展示用。 */
    public int getFemaleBase55RetirementAge() {
        return (int) getAttr("femaleBase55RetirementAge");
    }

    /** 女性（原50岁岗位）基准退休年龄（岁），仅供展示用。 */
    public int getFemaleBase50RetirementAge() {
        return (int) getAttr("femaleBase50RetirementAge");
    }

    // ── 向后兼容 getter（旧代码依赖） ─────────────────────────
    /** @deprecated 请使用 {@link #getMaleBaseRetirementAge()} */
    @Deprecated
    public int getMaleRetirementAge() { return getMaleBaseRetirementAge(); }

    /** @deprecated 请使用 {@link #getFemaleBase55RetirementAge()} */
    @Deprecated
    public int getFemaleRetirementAge() { return getFemaleBase55RetirementAge(); }
}
