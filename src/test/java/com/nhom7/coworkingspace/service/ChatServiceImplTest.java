package com.nhom7.coworkingspace.service;

import com.nhom7.coworkingspace.dto.request.ChatMessageRequest;
import com.nhom7.coworkingspace.dto.response.ChatMessageResponse;
import com.nhom7.coworkingspace.entity.Message;
import com.nhom7.coworkingspace.entity.User;
import com.nhom7.coworkingspace.exception.AppException;
import com.nhom7.coworkingspace.repository.MessageRepository;
import com.nhom7.coworkingspace.repository.UserRepository;
import com.nhom7.coworkingspace.service.impl.ChatServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatServiceImpl - Unit Tests")
class ChatServiceImplTest {

    private static final String SENDER_EMAIL = "sender@coworking.test";

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    private ChatServiceImpl chatService;
    private User sender;
    private User receiver;

    @BeforeEach
    void setUp() {
        chatService = new ChatServiceImpl(messageRepository, userRepository);
        sender = User.builder().id(1L).name("Sender").email(SENDER_EMAIL).build();
        receiver = User.builder().id(2L).name("Receiver").email("receiver@coworking.test").build();
    }

    @Test
    @DisplayName("Sending a message stores the authenticated sender and trimmed content")
    void sendMessagePersistsAndMapsResponse() {
        ChatMessageRequest request = ChatMessageRequest.builder()
                .receiverId(2L)
                .content("  Hello  ")
                .build();
        LocalDateTime sentAt = LocalDateTime.of(2026, 8, 27, 21, 0);

        given(userRepository.findByEmail(SENDER_EMAIL)).willReturn(Optional.of(sender));
        given(userRepository.findById(2L)).willReturn(Optional.of(receiver));
        given(messageRepository.save(org.mockito.ArgumentMatchers.any(Message.class)))
                .willAnswer(invocation -> {
                    Message message = invocation.getArgument(0);
                    message.setId(10L);
                    message.setCreatedAt(sentAt);
                    return message;
                });

        ChatMessageResponse response = chatService.sendMessage(SENDER_EMAIL, request);

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getSender()).isEqualTo(sender);
        assertThat(messageCaptor.getValue().getReceiver()).isEqualTo(receiver);
        assertThat(messageCaptor.getValue().getContent()).isEqualTo("Hello");
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getSenderEmail()).isEqualTo(SENDER_EMAIL);
        assertThat(response.getReceiverId()).isEqualTo(2L);
        assertThat(response.getCreatedAt()).isEqualTo(sentAt);
    }

    @Test
    @DisplayName("History contains both directions in repository order")
    void getConversationMapsBothDirections() {
        Message outgoing = message(11L, sender, receiver, "First", 20, 0);
        Message incoming = message(12L, receiver, sender, "Second", 20, 1);

        given(userRepository.findByEmail(SENDER_EMAIL)).willReturn(Optional.of(sender));
        given(userRepository.findById(2L)).willReturn(Optional.of(receiver));
        given(messageRepository.findConversation(1L, 2L)).willReturn(List.of(outgoing, incoming));

        List<ChatMessageResponse> result = chatService.getConversation(SENDER_EMAIL, 2L);

        assertThat(result).extracting(ChatMessageResponse::getContent)
                .containsExactly("First", "Second");
        assertThat(result.get(1).getSenderId()).isEqualTo(2L);
        verify(messageRepository).findConversation(1L, 2L);
    }

    @Test
    @DisplayName("Unknown peer returns 404")
    void getConversationRejectsUnknownUser() {
        given(userRepository.findByEmail(SENDER_EMAIL)).willReturn(Optional.of(sender));
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.getConversation(SENDER_EMAIL, 99L))
                .isInstanceOf(AppException.class)
                .hasMessage("user.not.found")
                .extracting("status")
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("A user cannot chat with their own account")
    void sendMessageRejectsSelfConversation() {
        ChatMessageRequest request = ChatMessageRequest.builder()
                .receiverId(1L)
                .content("Hello me")
                .build();
        given(userRepository.findByEmail(SENDER_EMAIL)).willReturn(Optional.of(sender));
        given(userRepository.findById(1L)).willReturn(Optional.of(sender));

        assertThatThrownBy(() -> chatService.sendMessage(SENDER_EMAIL, request))
                .isInstanceOf(AppException.class)
                .hasMessage("chat.self.not.allowed");
    }

    private Message message(
            Long id,
            User messageSender,
            User messageReceiver,
            String content,
            int hour,
            int minute) {
        return Message.builder()
                .id(id)
                .sender(messageSender)
                .receiver(messageReceiver)
                .content(content)
                .createdAt(LocalDateTime.of(2026, 8, 27, hour, minute))
                .build();
    }
}
