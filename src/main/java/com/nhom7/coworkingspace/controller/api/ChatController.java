package com.nhom7.coworkingspace.controller.api;

import com.nhom7.coworkingspace.dto.response.ApiResponse;
import com.nhom7.coworkingspace.dto.response.ChatMessageResponse;
import com.nhom7.coworkingspace.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Chat history endpoints")
public class ChatController {

    private final ChatService chatService;
    private final MessageSource messageSource;

    @GetMapping("/{userId}/messages")
    @Operation(summary = "Get message history with another user")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getMessages(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<ChatMessageResponse> messages = chatService.getConversation(
                userDetails.getUsername(),
                userId);
        String responseMessage = messageSource.getMessage(
                "chat.history.fetched",
                null,
                LocaleContextHolder.getLocale());
        return ResponseEntity.ok(ApiResponse.success(messages, responseMessage));
    }
}
