package com.nhom7.coworkingspace.controller.websocket;

import com.nhom7.coworkingspace.dto.request.ChatMessageRequest;
import com.nhom7.coworkingspace.dto.response.ChatMessageResponse;
import com.nhom7.coworkingspace.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private static final String MESSAGE_DESTINATION = "/queue/messages";

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void sendMessage(
            @Valid @Payload ChatMessageRequest request,
            Principal principal) {
        ChatMessageResponse response = chatService.sendMessage(principal.getName(), request);

        messagingTemplate.convertAndSendToUser(
                response.getReceiverEmail(),
                MESSAGE_DESTINATION,
                response);
        messagingTemplate.convertAndSendToUser(
                response.getSenderEmail(),
                MESSAGE_DESTINATION,
                response);
    }
}
