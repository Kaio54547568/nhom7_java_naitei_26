package com.nhom7.coworkingspace.service;

public interface EmailService {

    void sendPlainTextEmail(String recipient, String subject, String content);

    void sendHtmlEmail(String recipient, String subject, String htmlContent);
}
