package com.smartinventory.notificationservice.service.impl;

import com.smartinventory.notificationservice.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link EmailService} using Spring Boot's {@link JavaMailSender}.
 */
@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;
    private final String fromEmail;

    public EmailServiceImpl(JavaMailSender mailSender,
                            @Value("${app.mail.from:${spring.mail.username:no-reply@smartinventory.com}}") String fromEmail) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
    }

    @Override
    public void sendEmail(String to, String subject, String body) throws Exception {
        log.info("Dispatching email to: '{}', Subject: '{}'", to, subject);
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        if (fromEmail != null && !fromEmail.isBlank()) {
            helper.setFrom(fromEmail);
        }
        helper.setTo(to);
        helper.setSubject(subject);
        boolean isHtml = body != null && (body.contains("<html") || body.contains("<table") || body.contains("<div>") || body.contains("<p>") || body.contains("<!DOCTYPE"));
        helper.setText(body, isHtml);
        mailSender.send(mimeMessage);
        log.info("Email successfully dispatched to: '{}'", to);
    }
}
