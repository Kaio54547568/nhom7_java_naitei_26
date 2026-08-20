package com.nhom7.coworkingspace.controller;

import com.nhom7.coworkingspace.controller.api.AuthController;
import com.nhom7.coworkingspace.dto.request.SignUpRequest;
import com.nhom7.coworkingspace.service.AuthService;
import com.nhom7.coworkingspace.service.OtpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private OtpService otpService;

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(otpService, authService)).build();
    }

    @Test
    void sendConfirmationShouldReturnAccepted() throws Exception {
        mockMvc.perform(post("/api/auth/send-confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@coworking.test\"}"))
                .andExpect(status().isAccepted());

        verify(otpService).sendConfirmationOtp("user@coworking.test");
    }

    @Test
    void sendConfirmationShouldRejectInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/auth/send-confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(otpService);
    }

    @Test
    void signUpShouldReturnCreated() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Test User",
                                  "email": "user@coworking.test",
                                  "password": "password123",
                                  "phone": "0901234567"
                                }
                                """))
                .andExpect(status().isCreated());

        verify(authService).signUp(new SignUpRequest(
                "Test User",
                "user@coworking.test",
                "password123",
                "0901234567"));
    }

    @Test
    void forgotPasswordShouldReturnAccepted() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"active@coworking.test\"}"))
                .andExpect(status().isAccepted());

        verify(otpService).sendPasswordResetOtp("active@coworking.test");
    }
}
