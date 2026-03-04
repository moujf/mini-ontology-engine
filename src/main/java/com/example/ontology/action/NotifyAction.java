package com.example.ontology.action;

import com.example.ontology.model.EligibilityResult;
import com.example.ontology.model.OntologyObject;

/**
 * 通知动作实现。
 * <p>
 * 处理 {@link EligibilityResult} 类型的对象，根据结果 id 的后缀
 * 区分审查通过（APPROVED）和审查拒绝（REJECTED），输出通知信息，
 * 并将对象状态从 {@code NEW} 更新为 {@code DONE}。
 * </p>
 */
public class NotifyAction implements OntologyAction {

    /**
     * 仅支持 {@link EligibilityResult} 类型的对象。
     *
     * @param o 待判断的本体对象
     * @return  若为 EligibilityResult 则返回 {@code true}
     */
    @Override
    public boolean supports(OntologyObject o) {
        return o instanceof EligibilityResult;
    }

    /**
     * 执行通知动作：
     * <ul>
     *   <li>id 以 {@code -NOT-ELIG} 结尾 → 打印 REJECTED 通知</li>
     *   <li>id 以 {@code -ELIG} 结尾    → 打印 APPROVED 通知</li>
     * </ul>
     * 执行后将对象状态置为 {@code DONE}。
     *
     * @param o EligibilityResult 对象
     */
    @Override
    public void run(OntologyObject o) {
        EligibilityResult e = (EligibilityResult) o;
        if (o.getId().endsWith("-NOT-ELIG")) {
            System.out.println("ACTION notify -> REJECTED: " + o);
            String reason = e.getRejectReason();
            if (reason != null && !reason.isEmpty()) {
                System.out.println("  reject-reason : " + reason);
            }
        } else {
            System.out.println("ACTION notify -> APPROVED: " + o);
        }
        o.setState("DONE");
    }
}
