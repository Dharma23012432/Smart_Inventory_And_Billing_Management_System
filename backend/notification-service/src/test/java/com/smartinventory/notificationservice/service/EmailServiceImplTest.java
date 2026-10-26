package com.smartinventory.notificationservice.service;

import com.smartinventory.notificationservice.service.impl.EmailServiceImpl;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link EmailServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailServiceImpl(mailSender, "no-reply@smartinventory.com");
    }

    @Test
    @DisplayName("Successfully creates and sends HTML MimeMessage with configured from address")
    void testSendEmail_HtmlMessage() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        String to = "customer@example.com";
        String subject = "Invoice - BILL-20261019-0001";
        String htmlBody = "<html><body><h1>Invoice</h1><p>Customer details</p></body></html>";

        emailService.sendEmail(to, subject, htmlBody);

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(messageCaptor.capture());

        MimeMessage sentMessage = messageCaptor.getValue();
        assertNotNull(sentMessage);
        assertEquals(subject, sentMessage.getSubject());
        assertEquals(1, sentMessage.getAllRecipients().length);
        assertEquals(to, sentMessage.getAllRecipients()[0].toString());
        assertEquals("no-reply@smartinventory.com", sentMessage.getFrom()[0].toString());
    }

    @Test
    @DisplayName("Successfully sends plain text email")
    void testSendEmail_PlainText() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendEmail("supplier@example.com", "Test Subject", "Simple plain text content");

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }
}
