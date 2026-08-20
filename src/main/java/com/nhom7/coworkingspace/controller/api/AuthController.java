package com.nhom7.coworkingspace.controller.api;

import com.nhom7.coworkingspace.dto.request.SendConfirmationRequest;
import com.nhom7.coworkingspace.dto.request.SignUpRequest;
import com.nhom7.coworkingspace.service.AuthService;
import com.nhom7.coworkingspace.service.OtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final OtpService otpService;
    private final AuthService authService;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public void signUp(@Valid @RequestBody SignUpRequest request) {
        authService.signUp(request);
    }

    @PostMapping("/send-confirm")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void sendConfirmation(@Valid @RequestBody SendConfirmationRequest request) {
        otpService.sendConfirmationOtp(request.email());
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void forgotPassword(@Valid @RequestBody SendConfirmationRequest request) {
        otpService.sendPasswordResetOtp(request.email());
    }
}
