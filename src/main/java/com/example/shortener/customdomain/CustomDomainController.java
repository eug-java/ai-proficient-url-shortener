package com.example.shortener.customdomain;

import com.example.shortener.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orgs/{orgId}/domains")
public class CustomDomainController {

    public record AddDomain(@NotBlank String hostname) {}

    private final CustomDomainService domains;
    private final CurrentUser currentUser;
    private final boolean allowSkipDns;

    public CustomDomainController(
            CustomDomainService domains,
            CurrentUser currentUser,
            @Value("${app.custom-domains.allow-skip-dns-verify:false}") boolean allowSkipDns
    ) {
        this.domains = domains;
        this.currentUser = currentUser;
        this.allowSkipDns = allowSkipDns;
    }

    @GetMapping
    List<CustomDomainService.DomainView> list(
            @PathVariable UUID orgId,
            Authentication auth,
            HttpServletRequest request
    ) {
        return domains.list(orgId, currentUser.require(auth, request).sub());
    }

    @PostMapping
    CustomDomainService.DomainView add(
            @PathVariable UUID orgId,
            @Valid @RequestBody AddDomain body,
            Authentication auth,
            HttpServletRequest request
    ) {
        return domains.add(orgId, currentUser.require(auth, request).sub(), body.hostname());
    }

    @PostMapping("/{domainId}/verify")
    CustomDomainService.DomainView verify(
            @PathVariable UUID orgId,
            @PathVariable UUID domainId,
            Authentication auth,
            HttpServletRequest request
    ) {
        return domains.verify(orgId, currentUser.require(auth, request).sub(), domainId, allowSkipDns);
    }
}
