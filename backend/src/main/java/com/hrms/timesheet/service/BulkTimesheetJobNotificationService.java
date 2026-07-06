package com.hrms.timesheet.service;

import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class BulkTimesheetJobNotificationService {

    private final JavaMailSender mailSender;
    private final Environment environment;

    public BulkTimesheetJobNotificationService(JavaMailSender mailSender, Environment environment) {
        this.mailSender = mailSender;
        this.environment = environment;
    }

    public void sendCompletion(String userEmail, String subject, String body) {
        String host = environment.getProperty("spring.mail.host");
        if (!StringUtils.hasText(host)) {
            return;
        }
        String recipient = firstNonBlank(userEmail, environment.getProperty("hrms.notifications.bulk-job-email-to"));
        if (!StringUtils.hasText(recipient)) {
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    private String firstNonBlank(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : fallback;
    }
}
