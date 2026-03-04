package com.example.ontology.engine;

import java.util.HashMap;
import java.util.Map;

/**
 * 本体规则版本管理器。
 * <p>
 * 维护"规则集名称 + 版本号"到 Drools {@code ksession} 名称的映射关系，
 * 并作为 {@link RuleEngine} 的工厂入口，对外屏蔽 kmodule.xml 内部细节。
 * </p>
 *
 * <h3>版本映射表（对应 META-INF/kmodule.xml）</h3>
 * <pre>
 *   ruleSet="unemployment"  version="1.0"  →  ksession="unemploymentSessionV1"
 * </pre>
 *
 * <h3>扩展方式</h3>
 * <ol>
 *   <li>在 kmodule.xml 中新增 kbase / ksession（如 unemploymentSessionV2）</li>
 *   <li>在 {@link #REGISTRY} 中添加一行映射</li>
 *   <li>在 {@code ontology/rules/v2.0/} 目录下放置对应 DRL 文件</li>
 * </ol>
 */
public class OntologyRuleVersionManager {

    /**
     * 版本注册表：key = "ruleSet:version"，value = ksession 名称。
     * 与 META-INF/kmodule.xml 中的 ksession name 严格对应。
     */
    private static final Map<String, String> REGISTRY = new HashMap<>();

    static {
        // ── 失业保险金申领规则集 ──────────────────────────────────
        REGISTRY.put("unemployment:1.0", "unemploymentSessionV1");
        // 预留扩展示例（目录/DRL 就绪后取消注释并在 kmodule.xml 中添加对应条目）：
        // REGISTRY.put("unemployment:2.0", "unemploymentSessionV2");
    }

    private OntologyRuleVersionManager() {}

    /**
     * 根据规则集名称与版本号获取对应的 {@link RuleEngine} 实例。
     *
     * @param ruleSet 规则集名称，如 {@code "unemployment"}
     * @param version 版本字符串，如 {@code "1.0"}
     * @return 已初始化完毕的 RuleEngine
     * @throws IllegalArgumentException 若版本映射不存在
     */
    public static RuleEngine getEngine(String ruleSet, String version) {
        String key = ruleSet + ":" + version;
        String sessionName = REGISTRY.get(key);
        if (sessionName == null) {
            throw new IllegalArgumentException(
                "No ksession registered for ruleSet='" + ruleSet + "' version='" + version + "'. "
                + "Available: " + REGISTRY.keySet());
        }
        return new RuleEngine(sessionName);
    }

    /**
     * 查询某规则集当前已注册的最新版本（按字符串字典序最大值）。
     * 仅用于调试 / 运维展示，不建议用于生产路由。
     *
     * @param ruleSet 规则集名称
     * @return 最新版本号字符串，若不存在则抛出异常
     */
    public static String latestVersion(String ruleSet) {
        return REGISTRY.keySet().stream()
                .filter(k -> k.startsWith(ruleSet + ":"))
                .map(k -> k.substring(ruleSet.length() + 1))
                .max(String::compareTo)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No versions registered for ruleSet='" + ruleSet + "'"));
    }
}
