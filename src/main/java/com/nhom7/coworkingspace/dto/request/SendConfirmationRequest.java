package com.nhom7.coworkingspace.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SendConfirmationRequest(
        @NotBlank(message = "Email must not be blank")
        @Email(message = "Email must be valid")
        String email) {
}
