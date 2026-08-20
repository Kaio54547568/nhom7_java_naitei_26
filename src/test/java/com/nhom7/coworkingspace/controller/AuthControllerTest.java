package com.nhom7.coworkingspace.controller;

import com.nhom7.coworkingspace.controller.api.AuthController;
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

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(otpService)).build();
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
}
