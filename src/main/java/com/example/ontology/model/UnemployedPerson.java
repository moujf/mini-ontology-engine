package com.example.ontology.model;

import java.time.LocalDate;

/**
 * 失业保险金申领人实体。
 * <p>
 * 仅保留申请人身份核心要素：姓名、证件号码、户籍地、是否临近退休。
 * 其余业务字段按归属分散到各关联实体：
 * <ul>
 *   <li>参保缴费 / 停保原因   {@link InsuranceRecord}</li>
 *   <li>用人单位类型           {@link EmployerUnit}</li>
 *   <li>失业登记信息           {@link UnemploymentRegistration}</li>
 *   <li>劳动关系终止           {@link TerminationMaterial}</li>
 *   <li>收款账户信息           {@link BankAccount}</li>
 * </ul>
 * </p>
 *
 * <h3>v2 延迟退休支持</h3>
 * <p>
 * 新增 {@code birthday}（出生日期）和 {@code gender}（性别/岗位类别）属性。
 * {@link #isNearRetirement(UnemploymentPolicy)} 委托给
 * {@link UnemploymentPolicy#isNearRetirement(LocalDate, String, LocalDate)}，
 * 实现基于 2025 延迟退休方案的动态计算，DRL 规则无需任何硬编码年龄。
 * {@link #isRetired(UnemploymentPolicy)} 判断申请人是否已达法定退休年龄——
 * 已退休者已脱离"失业"范畴，DRL 兜底拒绝规则会标注 {@code already-retired}。
 * </p>
 */
public class UnemployedPerson extends OntologyObject {

    /**
     * 构造申领人对象（无出生日期/性别，临近退休使用静态标记）。
     *
     * @param id               证件号码（唯一标识）
     * @param name             姓名
     * @param domicileShenzhen 户籍地是否为深圳
     */
    public UnemployedPerson(String id, String name, boolean domicileShenzhen) {
        this(id, name, domicileShenzhen, false);
    }

    /**
     * 构造申领人对象（含静态临近退休标记，向后兼容）。
     *
     * @param id               证件号码（唯一标识）
     * @param name             姓名
     * @param domicileShenzhen 户籍地是否为深圳
     * @param nearRetirement   是否临近退休（静态预计算标记）
     */
    public UnemployedPerson(String id, String name, boolean domicileShenzhen, boolean nearRetirement) {
        super(id, "UnemployedPerson");
        setAttr("name",             name);
        setAttr("domicileShenzhen", domicileShenzhen);
        setAttr("nearRetirement",   nearRetirement);
        setState("PENDING");
    }

    /**
     * 构造申领人对象（含出生日期和性别，支持延迟退休动态计算）。
     *
     * @param id               证件号码（唯一标识）
     * @param name             姓名
     * @param domicileShenzhen 户籍地是否为深圳
     * @param birthday         出生日期（用于延迟退休动态计算）
     * @param gender           性别/岗位类别：{@code "male"}、{@code "female55"}、{@code "female50"}
     */
    public UnemployedPerson(String id, String name, boolean domicileShenzhen,
                            LocalDate birthday, String gender) {
        super(id, "UnemployedPerson");
        setAttr("name",             name);
        setAttr("domicileShenzhen", domicileShenzhen);
        setAttr("nearRetirement",   false); // 将由 isNearRetirement(policy) 动态判定
        setAttr("birthday",         birthday);
        setAttr("gender",           gender);
        setState("PENDING");
    }

    /** @return 姓名 */
    public String getName()             { return (String)    getAttr("name"); }

    /** @return 户籍地是否为深圳 */
    public boolean isDomicileShenzhen() { return (boolean)   getAttr("domicileShenzhen"); }

    /** @return 出生日期（可为 null） */
    public LocalDate getBirthday()      { return (LocalDate) getAttr("birthday"); }

    /**
     * @return 性别/岗位类别：{@code "male"}、{@code "female55"}、{@code "female50"}
     *         （可为 null，由 UnemploymentPolicy 内部归一化）
     */
    public String getGender()           { return (String)    getAttr("gender"); }

    /**
     * 是否临近退休（无参版本，使用构造时传入的预计算静态标记）。
     *
     * @return true 表示临近退休
     */
    public boolean isNearRetirement()   { return (boolean)   getAttr("nearRetirement"); }

    /**
     * 是否临近退休（动态参数版本）。
     * <p>
     * 有出生日期时委托给 {@link UnemploymentPolicy#isNearRetirement} 动态计算（含 2025 延迟退休方案）；
     * 无出生日期时回退到构造时传入的静态标记（向后兼容）。
     * </p>
     *
     * @param policy 当前生效的政策参数
     * @return true 表示距法定退休年龄（含延迟）不足 {@code nearRetirementYears} 年
     */
    public boolean isNearRetirement(UnemploymentPolicy policy) {
        LocalDate birthday = getBirthday();
        if (birthday != null) {
            return policy.isNearRetirement(birthday, getGender(), LocalDate.now());
        }
        return isNearRetirement();
    }

    /**
     * 是否已达到法定退休年龄（动态参数版本）。
     * <p>
     * 有出生日期时委托给 {@link UnemploymentPolicy#isRetired} 动态计算（含 2025 延迟退休方案）；
     * 无出生日期时保守返回 {@code false}（不认定已退休）。
     * </p>
     * <p>
     * 已退休者已脱离"失业"范畴，不得申领失业保险金。DRL 四个情形规则均以
     * {@code isRetired($policy) == false} 为前置条件，兜底拒绝规则会在
     * reason 中标注 {@code already-retired}。
     * </p>
     *
     * @param policy 当前生效的政策参数
     * @return true 表示已达法定退休年龄（含延迟），不得申领失业保险金
     */
    public boolean isRetired(UnemploymentPolicy policy) {
        LocalDate birthday = getBirthday();
        if (birthday != null) {
            return policy.isRetired(birthday, getGender(), LocalDate.now());
        }
        return false;
    }

    /**
     * 是否拥有剩余失业保险领取期限（由关联 InsuranceRecord 预计算后存入）。
     *
     * @return true 表示有剩余可领月数
     */
    public boolean hasRemainingEntitlement() {
        Object val = getAttr("hasRemainingEntitlement");
        return val != null && (boolean) val;
    }

    /**
     * 设置是否拥有剩余领取期限标记（由外部服务在组装事实时写入）。
     *
     * @param hasRemaining true 表示有剩余可领月数
     */
    public void setHasRemainingEntitlement(boolean hasRemaining) {
        setAttr("hasRemainingEntitlement", hasRemaining);
    }
}
