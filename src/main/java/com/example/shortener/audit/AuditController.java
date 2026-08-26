package com.example.shortener.audit;

import com.example.shortener.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orgs/{orgId}/audit")
public class AuditController {

    private final AuditService audit;
    private final CurrentUser currentUser;

    public AuditController(AuditService audit, CurrentUser currentUser) {
        this.audit = audit;
        this.currentUser = currentUser;
    }

    @GetMapping
    List<AuditService.AuditView> list(
            @PathVariable UUID orgId,
            Authentication auth,
            HttpServletRequest request
    ) {
        return audit.list(orgId, currentUser.require(auth, request).sub());
    }
}
