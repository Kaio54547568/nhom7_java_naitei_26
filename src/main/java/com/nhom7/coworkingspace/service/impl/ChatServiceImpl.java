package com.nhom7.coworkingspace.service.impl;

import com.nhom7.coworkingspace.dto.request.ChatMessageRequest;
import com.nhom7.coworkingspace.dto.response.ChatMessageResponse;
import com.nhom7.coworkingspace.entity.Message;
import com.nhom7.coworkingspace.entity.User;
import com.nhom7.coworkingspace.exception.AppException;
import com.nhom7.coworkingspace.repository.MessageRepository;
import com.nhom7.coworkingspace.repository.UserRepository;
import com.nhom7.coworkingspace.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ChatMessageResponse sendMessage(String senderEmail, ChatMessageRequest request) {
        User sender = findUserByEmail(senderEmail);
        User receiver = findUserById(request.getReceiverId());
        rejectSelfConversation(sender, receiver);

        Message message = Message.builder()
                .sender(sender)
                .receiver(receiver)
                .content(request.getContent().trim())
                .build();

        return toResponse(messageRepository.save(message));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getConversation(String currentUserEmail, Long otherUserId) {
        User currentUser = findUserByEmail(currentUserEmail);
        User otherUser = findUserById(otherUserId);
        rejectSelfConversation(currentUser, otherUser);

        return messageRepository.findConversation(currentUser.getId(), otherUser.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("user.not.found", HttpStatus.NOT_FOUND));
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException("user.not.found", HttpStatus.NOT_FOUND));
    }

    private void rejectSelfConversation(User firstUser, User secondUser) {
        if (firstUser.getId().equals(secondUser.getId())) {
            throw new AppException("chat.self.not.allowed");
        }
    }

    private ChatMessageResponse toResponse(Message message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getName())
                .senderEmail(message.getSender().getEmail())
                .receiverId(message.getReceiver().getId())
                .receiverName(message.getReceiver().getName())
                .receiverEmail(message.getReceiver().getEmail())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
