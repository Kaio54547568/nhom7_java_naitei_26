package com.nhom7.coworkingspace.controller;

import com.nhom7.coworkingspace.controller.api.ChatController;
import com.nhom7.coworkingspace.dto.response.ChatMessageResponse;
import com.nhom7.coworkingspace.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatController - Unit Tests")
class ChatControllerTest {

    @Mock
    private ChatService chatService;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private ChatController chatController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(chatController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        UserDetails principal = User.withUsername("current@coworking.test")
                .password("password")
                .roles("USER")
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/chats/{userId}/messages returns the authenticated user's conversation")
    void getMessagesReturnsConversation() throws Exception {
        ChatMessageResponse response = ChatMessageResponse.builder()
                .id(5L)
                .senderId(1L)
                .receiverId(2L)
                .content("Hello")
                .createdAt(LocalDateTime.of(2026, 8, 27, 21, 0))
                .build();
        given(chatService.getConversation("current@coworking.test", 2L))
                .willReturn(List.of(response));
        given(messageSource.getMessage(eq("chat.history.fetched"), any(), any(Locale.class)))
                .willReturn("Message history fetched successfully");

        mockMvc.perform(get("/api/chats/{userId}/messages", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Message history fetched successfully"))
                .andExpect(jsonPath("$.data[0].id").value(5L))
                .andExpect(jsonPath("$.data[0].content").value("Hello"));

        verify(chatService).getConversation("current@coworking.test", 2L);
    }
}
