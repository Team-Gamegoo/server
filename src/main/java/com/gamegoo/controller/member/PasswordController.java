package com.gamegoo.controller.member;

import com.gamegoo.apiPayload.ApiResponse;
import com.gamegoo.apiPayload.code.status.ErrorStatus;
import com.gamegoo.apiPayload.exception.handler.MemberHandler;
import com.gamegoo.dto.member.MemberRequest;
import com.gamegoo.service.member.AuthService;
import com.gamegoo.service.member.PasswordService;
import com.gamegoo.util.JWTUtil;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/member/password")
@Slf4j
public class PasswordController {

    private final PasswordService passwordService;
    private final AuthService authService;

    @PostMapping("/check")
    @Operation(summary = "JWT 토큰이 필요한 비밀번호 확인 API 입니다.", description = "API for checking password with JWT")
    public ApiResponse<String> checkPasswordWithJWT(
            @Valid @RequestBody MemberRequest.PasswordCheckRequestDTO passwordCheckRequestDTO) {
        Long currentUserId = JWTUtil.getCurrentUserId(); //헤더에 있는 jwt 토큰에서 id를 가져오는 코드

        boolean isPasswordValid = passwordService.checkPasswordById(currentUserId,
                passwordCheckRequestDTO.getPassword()); //request body에 있는 password와 currentUserId를 전달

        if (isPasswordValid) {
            return ApiResponse.onSuccess("비밀번호가 일치합니다.");
        } else {
            throw new MemberHandler(ErrorStatus.PASSWORD_INVALID);
        }
    }

    @PostMapping("/jwt/reset")
    @Operation(summary = "JWT 토큰이 필요한 비밀번호 재설정 API 입니다.", description = "API for reseting password with JWT")
    public ApiResponse<String> resetPasswordWithJWT(
            @Valid @RequestBody MemberRequest.PasswordRequestJWTDTO passwordRequestDTO) {
        Long currentUserId = JWTUtil.getCurrentUserId();
        passwordService.updatePasswordById(currentUserId, passwordRequestDTO.getOldPassword(),
                passwordRequestDTO.getNewPassword());

        return ApiResponse.onSuccess("비밀번호 재설정을 완료했습니다.");
    }

    @PostMapping("/reset")
    @Operation(summary = "비밀번호 재설정 API 입니다.", description = "API for reseting password")
    public ApiResponse<String> resetPassword(@Valid @RequestBody MemberRequest.PasswordRequestDTO passwordRequestDTO) {
        // dto
        String email = passwordRequestDTO.getEmail();
        String verifyCode = passwordRequestDTO.getVerifyCode();
        String newPassword = passwordRequestDTO.getNewPassword();

        // 인증코드 검증
        authService.verifyCode(email, verifyCode);

        // 비밀번호 재설정
        passwordService.updatePasswordWithEmail(email, newPassword);

        return ApiResponse.onSuccess("비밀번호 재설정을 완료했습니다.");
    }

}
