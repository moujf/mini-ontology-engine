package com.example.ontology.action;

import com.example.ontology.model.OntologyObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 动作分发引擎。
 * <p>
 * 维护一组 {@link OntologyAction} 实现，当 {@link #fire(OntologyObject)} 被调用时，
 * 遍历所有已注册的动作，对支持该对象的动作依次执行。
 * 支持注册多个动作，天然具备扩展性，新增动作无需修改已有代码（开闭原则）。
 * </p>
 */
public class ActionEngine {

    /** 已注册的动作列表，按注册顺序依次执行 */
    private List<OntologyAction> list = new ArrayList<>();

    /**
     * 注册一个动作到引擎。
     *
     * @param a 要注册的动作实现
     */
    public void add(OntologyAction a) { list.add(a); }

    /**
     * 对指定对象触发所有支持它的动作。
     *
     * @param o 待处理的本体对象
     */
    public void fire(OntologyObject o) {
        for (var a : list) if (a.supports(o)) a.run(o);
    }
}
