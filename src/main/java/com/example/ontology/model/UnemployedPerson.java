package com.example.ontology.model;

/**
 * 失业保险金申领人实体。
 * <p>
 * 仅保留申请人身份核心要素：姓名、证件号码、户籍地、是否临近退休。
 * 其余业务字段按归属分散到各关联实体：
 * <ul>
 *   <li>参保缴费 / 停保原因  → {@link InsuranceRecord}</li>
 *   <li>用人单位类型          → {@link EmployerUnit}</li>
 *   <li>失业登记信息          → {@link UnemploymentRegistration}</li>
 *   <li>劳动关系终止          → {@link TerminationMaterial}</li>
 *   <li>收款账户信息          → {@link BankAccount}</li>
 * </ul>
 * </p>
 */
public class UnemployedPerson extends OntologyObject {

    /**
     * 构造申领人对象。
     *
     * @param id               证件号码（唯一标识）
     * @param name             姓名
     * @param domicileShenzhen 户籍地是否为深圳
     */
    public UnemployedPerson(String id, String name, boolean domicileShenzhen) {
        this(id, name, domicileShenzhen, false);
    }

    /**
     * 构造申领人对象（含临近退休标记）。
     *
     * @param id               证件号码（唯一标识）
     * @param name             姓名
     * @param domicileShenzhen 户籍地是否为深圳
     * @param nearRetirement   是否临近退休（距法定退休年龄不足5年）
     */
    public UnemployedPerson(String id, String name, boolean domicileShenzhen, boolean nearRetirement) {
        super(id, "UnemployedPerson");
        setAttr("name",             name);
        setAttr("domicileShenzhen", domicileShenzhen);
        setAttr("nearRetirement",   nearRetirement);
        setState("PENDING");
    }

    /** @return 姓名 */
    public String getName() { return (String) getAttr("name"); }

    /** @return 户籍地是否为深圳 */
    public boolean isDomicileShenzhen() { return (boolean) getAttr("domicileShenzhen"); }

    /**
     * 是否临近退休（无参版本，使用构造时传入的预计算标记）。
     * 用于规则引擎中快速判定情形三，不依赖政策参数对象。
     *
     * @return true 表示临近退休
     */
    public boolean isNearRetirement() { return (boolean) getAttr("nearRetirement"); }

    /**
     * 是否临近退休（动态参数版本，基于 {@link UnemploymentPolicy} 政策参数判定）。
     * <p>
     * 当申请人预存了年龄信息（{@code age} 属性）时，通过政策参数动态计算；
     * 否则回退到构造时传入的静态标记，保持向后兼容。
     * </p>
     *
     * @param policy 当前生效的政策参数，包含法定退休年龄和临近退休年限
     * @return true 表示距法定退休年龄不足 {@code policy.getNearRetirementYears()} 年
     */
    public boolean isNearRetirement(UnemploymentPolicy policy) {
        Object ageAttr = getAttr("age");
        if (ageAttr == null) {
            // 无年龄属性时回退到静态标记
            return isNearRetirement();
        }
        int age = (int) ageAttr;
        Object genderAttr = getAttr("gender");
        // 默认按男性退休年龄；gender="F" 时使用女性退休年龄
        int retirementAge = ("F".equals(genderAttr))
                ? policy.getFemaleRetirementAge()
                : policy.getMaleRetirementAge();
        return (retirementAge - age) <= policy.getNearRetirementYears();
    }

    /**
     * 是否拥有剩余失业保险领取期限（由关联 InsuranceRecord 预计算后存入）。
     * 用于规则引擎的 eval 条件，避免在 DRL 中直接访问 InsuranceRecord 的字段。
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

