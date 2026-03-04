package com.example.ontology.engine;

import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

/**
 * Drools 规则引擎封装类。
 * <p>
 * 负责初始化 KIE 容器，加载 {@code META-INF/kmodule.xml} 中定义的
 * {@code ksession} 会话，并提供对象插入与规则触发的统一入口。
 * </p>
 *
 * <h3>推荐用法（版本感知）</h3>
 * <pre>{@code
 * // 通过版本管理器获取指定版本的引擎（推荐）
 * RuleEngine re = OntologyRuleVersionManager.getEngine("unemployment", "1.0");
 *
 * // 直接指定 ksession 名称（高级用法）
 * RuleEngine re = new RuleEngine("unemploymentSessionV1");
 * }</pre>
 */
public class RuleEngine {

    /** Drools 工作内存会话，存储业务对象并执行规则匹配 */
    private KieSession ks;

    /**
     * 构造规则引擎，使用指定的 ksession 名称（对应 kmodule.xml 中的定义）。
     * <p>
     * 推荐通过 {@link OntologyRuleVersionManager#getEngine(String, String)}
     * 间接调用，以获得版本路由能力。
     * </p>
     *
     * @param sessionName kmodule.xml 中定义的 ksession name，
     *                    如 {@code "unemploymentSessionV1"}
     */
    public RuleEngine(String sessionName) {
        KieServices s = KieServices.Factory.get();
        KieContainer c = s.getKieClasspathContainer();
        ks = c.newKieSession(sessionName);
    }

    /**
     * 返回当前 KieSession，可用于插入对象或查询工作内存。
     *
     * @return KieSession 实例
     */
    public KieSession session() { return ks; }

    /**
     * 触发工作内存中所有匹配的规则。
     * 执行后，符合条件的规则的 then 块将被执行。
     */
    public void fire() { ks.fireAllRules(); }
}
