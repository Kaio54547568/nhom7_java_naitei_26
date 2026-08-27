package com.nhom7.coworkingspace.service;

import com.nhom7.coworkingspace.dto.request.ChatMessageRequest;
import com.nhom7.coworkingspace.dto.response.ChatMessageResponse;

import java.util.List;

public interface ChatService {

    ChatMessageResponse sendMessage(String senderEmail, ChatMessageRequest request);

    List<ChatMessageResponse> getConversation(String currentUserEmail, Long otherUserId);
}
