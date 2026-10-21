package com.smartinventory.notificationservice.service;

/**
 * Service interface for sending emails.
 */
public interface EmailService {

    /**
     * Sends an email to the recipient.
     *
     * @param to      recipient email address
     * @param subject email subject line
     * @param body    email body content (plain text or html)
     * @throws Exception if sending fails
     */
    void sendEmail(String to, String subject, String body) throws Exception;
}
