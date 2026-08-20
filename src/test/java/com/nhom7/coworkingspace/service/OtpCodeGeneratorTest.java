package com.nhom7.coworkingspace.service;

import com.nhom7.coworkingspace.util.OtpCodeGenerator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OtpCodeGeneratorTest {

    private final OtpCodeGenerator otpCodeGenerator = new OtpCodeGenerator();

    @Test
    void generateCodeShouldReturnSixDigits() {
        String code = otpCodeGenerator.generateCode();

        assertThat(code).matches("\\d{6}");
    }
}
