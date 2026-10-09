package com.lmf.finpro.infrastructure.web.controller;

import com.lmf.finpro.application.insight.InsightApplicationService;
import com.lmf.finpro.infrastructure.security.AuthenticatedUser;
import com.lmf.finpro.infrastructure.web.dto.insight.InsightResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Insights automáticos do grupo ativo: assinatura esquecida, despesa fora do padrão e cliente. */
@RestController
@RequestMapping("/api/insights")
@RequiredArgsConstructor
public class InsightController {

    private static final String TYPE_PREFIX = "INSIGHT_";

    private final InsightApplicationService insightApplicationService;

    @GetMapping
    public List<InsightResponse> list(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return insightApplicationService.list(currentUser.householdId()).stream()
                .map(
                        insight ->
                                new InsightResponse(
                                        insight.type().name().substring(TYPE_PREFIX.length()),
                                        insight.subject(),
                                        insight.amount(),
                                        insight.reference(),
                                        insight.count()))
                .toList();
    }
}
