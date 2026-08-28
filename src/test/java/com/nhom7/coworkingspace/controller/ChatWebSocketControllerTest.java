package com.nhom7.coworkingspace.controller;

import com.nhom7.coworkingspace.controller.websocket.ChatWebSocketController;
import com.nhom7.coworkingspace.dto.request.ChatMessageRequest;
import com.nhom7.coworkingspace.dto.response.ChatMessageResponse;
import com.nhom7.coworkingspace.service.ChatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.security.Principal;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatWebSocketController - Unit Tests")
class ChatWebSocketControllerTest {

    @Mock
    private ChatService chatService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private Principal principal;

    @InjectMocks
    private ChatWebSocketController controller;

    @Test
    @DisplayName("A STOMP message is persisted then delivered privately to sender and receiver")
    void sendMessagePersistsAndDeliversToBothParticipants() {
        ChatMessageRequest request = ChatMessageRequest.builder()
                .receiverId(2L)
                .content("Hello")
                .build();
        ChatMessageResponse response = ChatMessageResponse.builder()
                .id(8L)
                .senderEmail("sender@coworking.test")
                .receiverEmail("receiver@coworking.test")
                .content("Hello")
                .build();
        given(principal.getName()).willReturn("sender@coworking.test");
        given(chatService.sendMessage("sender@coworking.test", request)).willReturn(response);

        controller.sendMessage(request, principal);

        verify(chatService).sendMessage("sender@coworking.test", request);
        verify(messagingTemplate).convertAndSendToUser(
                "receiver@coworking.test",
                "/queue/messages",
                response);
        verify(messagingTemplate).convertAndSendToUser(
                "sender@coworking.test",
                "/queue/messages",
                response);
    }
}
