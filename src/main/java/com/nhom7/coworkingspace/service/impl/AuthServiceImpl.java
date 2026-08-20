package com.nhom7.coworkingspace.service.impl;

import com.nhom7.coworkingspace.dto.request.SignUpRequest;
import com.nhom7.coworkingspace.entity.Role;
import com.nhom7.coworkingspace.entity.User;
import com.nhom7.coworkingspace.exception.EmailAlreadyExistsException;
import com.nhom7.coworkingspace.repository.RoleRepository;
import com.nhom7.coworkingspace.repository.UserRepository;
import com.nhom7.coworkingspace.service.AuthService;
import com.nhom7.coworkingspace.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String USER_ROLE = "USER";
    private static final String INACTIVE_STATUS = "INACTIVE";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;

    @Override
    @Transactional
    public void signUp(SignUpRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }

        Role userRole = roleRepository.findByName(USER_ROLE)
                .orElseThrow(() -> new IllegalStateException("Default USER role is not configured"));
        User user = User.builder()
                .name(request.name().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .phone(request.phone())
                .status(INACTIVE_STATUS)
                .isIdentityVerified(false)
                .isBusinessVerified(false)
                .language("vi")
                .roles(new HashSet<>(Set.of(userRole)))
                .build();

        userRepository.saveAndFlush(user);
        otpService.sendConfirmationOtp(email);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
