package com.example.shortener.org;

import com.example.shortener.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orgs")
public class OrganizationController {

    public record CreateOrg(@NotBlank String name, @NotBlank String slug) {}

    public record AddMember(
            @NotBlank String userSub,
            String email,
            String displayName,
            OrganizationMember.Role role
    ) {}

    public record ChangeRole(OrganizationMember.Role role) {}

    private final OrganizationService service;
    private final CurrentUser currentUser;

    public OrganizationController(OrganizationService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @PostMapping
    ResponseEntity<OrganizationService.OrgView> create(
            @Valid @RequestBody CreateOrg body,
            Authentication auth,
            HttpServletRequest request
    ) {
        OrganizationService.OrgView org = service.create(
                body.name(),
                body.slug(),
                currentUser.require(auth, request)
        );
        return ResponseEntity.created(URI.create("/api/v1/orgs/" + org.id())).body(org);
    }

    @GetMapping
    List<OrganizationService.OrgView> mine(Authentication auth, HttpServletRequest req) {
        return service.mine(currentUser.require(auth, req).sub());
    }

    @GetMapping("/{orgId}/members")
    List<OrganizationService.MemberView> members(
            @PathVariable UUID orgId,
            Authentication auth,
            HttpServletRequest req
    ) {
        return service.members(orgId, currentUser.require(auth, req).sub());
    }

    @PostMapping("/{orgId}/members")
    OrganizationService.MemberView add(
            @PathVariable UUID orgId,
            @Valid @RequestBody AddMember body,
            Authentication auth,
            HttpServletRequest req
    ) {
        return service.add(
                orgId,
                currentUser.require(auth, req).sub(),
                body.userSub(),
                body.email(),
                body.displayName(),
                body.role() == null ? OrganizationMember.Role.MEMBER : body.role()
        );
    }

    @PatchMapping("/{orgId}/members/{memberId}")
    OrganizationService.MemberView role(
            @PathVariable UUID orgId,
            @PathVariable UUID memberId,
            @RequestBody ChangeRole body,
            Authentication auth,
            HttpServletRequest req
    ) {
        return service.role(orgId, memberId, currentUser.require(auth, req).sub(), body.role());
    }

    public record TransferOwnership(@NotBlank String newOwnerSub) {}

    @PostMapping("/{orgId}/transfer-ownership")
    OrganizationService.MemberView transfer(
            @PathVariable UUID orgId,
            @Valid @RequestBody TransferOwnership body,
            Authentication auth,
            HttpServletRequest req
    ) {
        return service.transferOwnership(
                orgId,
                currentUser.require(auth, req).sub(),
                body.newOwnerSub()
        );
    }
}
