package com.example.ontology.web.controller;

import com.example.ontology.web.dto.ApplicantRequest;
import com.example.ontology.web.dto.ReviewResponse;
import com.example.ontology.web.service.ReviewService;
import org.springframework.web.bind.annotation.*;

/**
 * REST 控制器，提供前端所需的审查数据接口。
 *
 * <pre>
 *   GET  /api/review   — 执行示例审查，返回三阶段 JSON 数据
 *   POST /api/review   — 提交单条自定义申请人，执行审查并返回结果
 * </pre>
 */
@RestController
@RequestMapping("/api")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /** 执行内置示例审查 */
    @GetMapping("/review")
    public ReviewResponse review() {
        return reviewService.runDemoReview();
    }

    /** 提交自定义申请人数据，执行规则并返回结果 */
    @PostMapping("/review")
    public ReviewResponse submitApplicant(@RequestBody ApplicantRequest req) {
        return reviewService.submitApplicant(req);
    }
}
