package com.nhom7.coworkingspace.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Locale;

@Component
public class OtpCodeGenerator {

    private static final int OTP_BOUND = 1_000_000;

    private final SecureRandom secureRandom = new SecureRandom();

    public String generateCode() {
        return String.format(Locale.ROOT, "%06d", secureRandom.nextInt(OTP_BOUND));
    }
}
