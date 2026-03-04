package com.example.ontology.model;

import java.util.*;

/**
 * 本体对象抽象基类。
 * <p>
 * 模拟 Palantir Ontology 中的 Object 概念，所有业务实体均继承此类。
 * 每个对象拥有唯一标识（id）、类型（type）、状态（state）
 * 以及一个动态属性 Map（attrs），支持任意键值对扩展，无需修改类结构。
 * </p>
 */
public abstract class OntologyObject {

    /** 对象唯一标识符 */
    private String id;

    /** 对象类型名称，如 "Person"、"EligibilityResult" */
    private String type;

    /** 当前状态，如 "ACTIVE"、"NEW"、"DONE" */
    private String state;

    /** 动态属性集合，存储各子类特有的业务字段 */
    private Map<String, Object> attrs = new HashMap<>();

    /**
     * 构造本体对象。
     *
     * @param id   对象唯一标识
     * @param type 对象类型名称
     */
    public OntologyObject(String id, String type) {
        this.id = id;
        this.type = type;
    }

    /** @return 对象唯一标识 */
    public String getId() { return id; }

    /** @return 对象类型名称 */
    public String getType() { return type; }

    /**
     * 设置当前状态。
     *
     * @param s 新状态值
     */
    public void setState(String s) { state = s; }

    /** @return 当前状态 */
    public String getState() { return state; }

    /**
     * 设置动态属性。
     *
     * @param k 属性键
     * @param v 属性值
     */
    public void setAttr(String k, Object v) { attrs.put(k, v); }

    /**
     * 获取动态属性。
     *
     * @param k 属性键
     * @return  属性值，若不存在则返回 null
     */
    public Object getAttr(String k) { return attrs.get(k); }

    /**
     * 返回对象的可读描述，格式：{type}({id}) state={state} {attrs}
     */
    @Override
    public String toString() {
        return type + "(" + id + ") state=" + state + " " + attrs;
    }
}
