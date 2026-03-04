package com.example.ontology.action;

import com.example.ontology.model.OntologyObject;

/**
 * 本体动作接口。
 * <p>
 * 所有后处理动作的统一契约，遵循策略模式（Strategy Pattern）。
 * 每个实现类负责判断自己是否支持某类对象，并定义具体的处理逻辑。
 * 通过 {@link ActionEngine} 统一调度，支持注册多个动作实现。
 * </p>
 */
public interface OntologyAction {

    /**
     * 判断当前动作是否支持处理指定对象。
     *
     * @param o 待处理的本体对象
     * @return  若支持则返回 {@code true}，否则返回 {@code false}
     */
    boolean supports(OntologyObject o);

    /**
     * 对指定对象执行动作逻辑。
     * 仅在 {@link #supports(OntologyObject)} 返回 {@code true} 时被调用。
     *
     * @param o 待处理的本体对象
     */
    void run(OntologyObject o);
}
