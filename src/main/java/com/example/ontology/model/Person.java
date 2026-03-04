package com.example.ontology.model;

/**
 * 人员实体，代表待审查的业务对象。
 * <p>
 * 继承 {@link OntologyObject}，通过动态属性 Map 存储
 * {@code age}（年龄）和 {@code years}（工龄）两个业务字段。
 * 初始状态为 {@code ACTIVE}。
 * </p>
 */
public class Person extends OntologyObject {

    /**
     * 构造人员对象。
     *
     * @param id    人员唯一标识，如 "P1"
     * @param age   年龄
     * @param years 参保工龄（年）
     */
    public Person(String id, int age, int years) {
        super(id, "Person");
        setAttr("age", age);
        setAttr("years", years);
        setState("ACTIVE");
    }

    /**
     * 获取年龄，供 Drools 规则条件中直接调用。
     *
     * @return 年龄
     */
    public int getAge() { return (int) getAttr("age"); }

    /**
     * 获取工龄，供 Drools 规则条件中直接调用。
     *
     * @return 工龄（年）
     */
    public int getYears() { return (int) getAttr("years"); }
}
