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
     * 是否临近退休（距法定退休年龄不足5年）。
     * 用于规则引擎判断情形三。
     *
     * @return true 表示临近退休
     */
    public boolean isNearRetirement() { return (boolean) getAttr("nearRetirement"); }
}

