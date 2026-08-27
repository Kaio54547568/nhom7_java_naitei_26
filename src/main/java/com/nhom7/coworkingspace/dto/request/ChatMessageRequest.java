package com.nhom7.coworkingspace.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageRequest {

    @NotNull(message = "{validation.chat.receiver.required}")
    private Long receiverId;

    @NotBlank(message = "{validation.chat.content.required}")
    @Size(max = 2000, message = "{validation.chat.content.size}")
    private String content;
}
