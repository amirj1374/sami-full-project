package com.sami.app.notification.web;

import com.sami.app.common.api.ApiResponse;
import com.sami.app.notification.dto.StaffNotificationResponse;
import com.sami.app.notification.dto.UnreadNotificationCountResponse;
import com.sami.app.notification.service.StaffNotificationService;
import com.sami.app.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class StaffNotificationController {

    private final StaffNotificationService service;

    @GetMapping
    public ApiResponse<List<StaffNotificationResponse>> list(@AuthenticationPrincipal SecurityUser principal) {
        return ApiResponse.ok(service.list(principal.getId()));
    }

    @GetMapping("/unread-count")
    public ApiResponse<UnreadNotificationCountResponse> unreadCount(
            @AuthenticationPrincipal SecurityUser principal) {
        return ApiResponse.ok(new UnreadNotificationCountResponse(service.unreadCount(principal.getId())));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<StaffNotificationResponse> markRead(
            @AuthenticationPrincipal SecurityUser principal, @PathVariable Long id) {
        return ApiResponse.ok(service.markRead(principal.getId(), id));
    }

    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllRead(@AuthenticationPrincipal SecurityUser principal) {
        service.markAllRead(principal.getId());
        return ApiResponse.ok();
    }
}
