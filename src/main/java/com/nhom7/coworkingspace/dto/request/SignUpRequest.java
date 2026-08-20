package com.nhom7.coworkingspace.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
        @NotBlank(message = "Name must not be blank")
        @Size(max = 150, message = "Name must not exceed 150 characters")
        String name,

        @NotBlank(message = "Email must not be blank")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Password must not be blank")
        @Size(min = 8, max = 72, message = "Password must contain between 8 and 72 characters")
        String password,

        @Size(max = 20, message = "Phone must not exceed 20 characters")
        String phone) {
}
