package com.gamegoo.controller.notification;

import com.gamegoo.apiPayload.ApiResponse;
import com.gamegoo.converter.NotificationConverter;
import com.gamegoo.domain.notification.Notification;
import com.gamegoo.dto.notification.NotificationResponse;
import com.gamegoo.dto.notification.NotificationResponse.notificationReadDTO;
import com.gamegoo.service.notification.NotificationService;
import com.gamegoo.util.JWTUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/v1/notification")
@Tag(name = "Notification", description = "Notification 관련 API")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "알림 목록 조회 API", description = "알림 팝업 화면에서 알림 목록을 조회하는 API 입니다.")
    @Parameter(name = "cursor", description = "페이징을 위한 커서, Long 타입 notificationId를 보내주세요. " +
            "보내지 않으면 가장 최근 알림 10개를 조회합니다.")
    @GetMapping
    public ApiResponse<NotificationResponse.cursorNotificationListDTO> getNotificationList(
            @RequestParam(name = "cursor", required = false) Long cursor) {

        Long memberId = JWTUtil.getCurrentUserId();

        Slice<Notification> notifications = notificationService.getNotificationListByCursor(memberId, cursor);

        return ApiResponse.onSuccess(NotificationConverter.toCursorNotificationListDTO(notifications));
    }

    @Operation(summary = "알림 전체 목록 조회 API", description = "알림 전체보기 화면에서 알림 목록을 조회하는 API 입니다.")
    @Parameter(name = "page", description = "페이지 번호, 1 이상의 숫자를 입력해 주세요.")
    @GetMapping("/total")
    public ApiResponse<NotificationResponse.pageNotificationListDTO> getTotalNotificationList(
            @RequestParam(name = "page") Integer page) {
        Long memberId = JWTUtil.getCurrentUserId();

        Page<Notification> notifications = notificationService.getNotificationListByPage(memberId, page - 1);

        return ApiResponse.onSuccess(NotificationConverter.toPageNotificationListDTO(notifications));
    }

    @Operation(summary = "알림 읽음 처리 API", description = "특정 알림을 읽음 처리하는 API 입니다.")
    @Parameter(name = "notificationId", description = "읽음 처리할 알림의 id 입니다.")
    @PatchMapping("/{notificationId}")
    public ApiResponse<NotificationResponse.notificationReadDTO> getTotalNotificationList(
            @PathVariable(name = "notificationId") Long notificationId) {
        Long memberId = JWTUtil.getCurrentUserId();

        Notification notification = notificationService.readNotification(memberId, notificationId);

        NotificationResponse.notificationReadDTO result = notificationReadDTO.builder()
                .notificationId(notification.getId())
                .message("알림 읽음 처리 성공")
                .build();

        return ApiResponse.onSuccess(result);
    }

    @Operation(summary = "안읽은 알림 개수 조회 API", description = "해당 회원의 안읽은 알림의 개수를 조회하는 API 입니다.")
    @GetMapping("/unread/count")
    public ApiResponse<Long> getUnreadNotificationCount() {
        Long memberId = JWTUtil.getCurrentUserId();

        return ApiResponse.onSuccess(notificationService.countUnreadNotification(memberId));
    }

}
