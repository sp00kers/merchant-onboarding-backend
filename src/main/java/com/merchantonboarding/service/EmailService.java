package com.merchantonboarding.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@merchantonboarding.com}")
    private String fromEmail;

    @Value("${app.name:Merchant Onboarding Platform}")
    private String appName;

    @Async
    public void sendNotificationEmail(String to, String subject, String message) {
        if (mailSender == null) {
            System.out.println("[EMAIL MOCK] To: " + to + ", Subject: " + subject + ", Message: " + message);
            return;
        }

        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(fromEmail);
            mailMessage.setTo(to);
            mailMessage.setSubject("[" + appName + "] " + subject);
            mailMessage.setText(message + "\n\n---\nThis is an automated notification from " + appName);
            mailSender.send(mailMessage);
        } catch (Exception e) {
            System.err.println("Failed to send email to " + to + ": " + e.getMessage());
        }
    }

    @Async
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        if (mailSender == null) {
            System.out.println("[EMAIL MOCK - HTML] To: " + to + ", Subject: " + subject);
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("[" + appName + "] " + subject);
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            System.err.println("Failed to send HTML email to " + to + ": " + e.getMessage());
        }
    }

    public void sendCaseStatusEmail(String to, String caseId, String businessName, String oldStatus, String newStatus) {
        String subject = "Case Status Update - " + caseId;
        String htmlContent = buildCaseStatusEmailHtml(caseId, businessName, oldStatus, newStatus);
        sendHtmlEmail(to, subject, htmlContent);
    }

    public void sendCaseAssignmentEmail(String to, String caseId, String businessName) {
        String subject = "New Case Assignment - " + caseId;
        String htmlContent = buildCaseAssignmentEmailHtml(caseId, businessName);
        sendHtmlEmail(to, subject, htmlContent);
    }

    public void sendVerificationCompleteEmail(String to, String caseId, String businessName,
                                               String verificationType, int confidenceScore) {
        String subject = "Verification Complete - " + caseId;
        String htmlContent = buildVerificationEmailHtml(caseId, businessName, verificationType, confidenceScore);
        sendHtmlEmail(to, subject, htmlContent);
    }

    private String buildCaseStatusEmailHtml(String caseId, String businessName, String oldStatus, String newStatus) {
        return """
            <!DOCTYPE html>
            <html>
            <head><style>
                body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                .header { background: #4361ee; color: white; padding: 20px; text-align: center; }
                .content { padding: 20px; background: #f9f9f9; }
                .status { display: inline-block; padding: 4px 12px; border-radius: 4px; font-weight: bold; }
                .status-old { background: #fee2e2; color: #991b1b; }
                .status-new { background: #dcfce7; color: #166534; }
                .footer { padding: 20px; text-align: center; color: #666; font-size: 12px; }
            </style></head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Case Status Update</h1>
                    </div>
                    <div class="content">
                        <p>The status of the following case has been updated:</p>
                        <p><strong>Case ID:</strong> %s</p>
                        <p><strong>Business:</strong> %s</p>
                        <p><strong>Status Change:</strong></p>
                        <p><span class="status status-old">%s</span> to <span class="status status-new">%s</span></p>
                        <p>Please log in to the Merchant Onboarding Platform for more details.</p>
                    </div>
                    <div class="footer">
                        This is an automated message from %s
                    </div>
                </div>
            </body>
            </html>
            """.formatted(caseId, businessName, oldStatus, newStatus, appName);
    }

    private String buildCaseAssignmentEmailHtml(String caseId, String businessName) {
        return """
            <!DOCTYPE html>
            <html>
            <head><style>
                body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                .header { background: #4361ee; color: white; padding: 20px; text-align: center; }
                .content { padding: 20px; background: #f9f9f9; }
                .btn { display: inline-block; padding: 10px 20px; background: #4361ee; color: white; text-decoration: none; border-radius: 4px; }
                .footer { padding: 20px; text-align: center; color: #666; font-size: 12px; }
            </style></head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>New Case Assignment</h1>
                    </div>
                    <div class="content">
                        <p>A new case has been assigned to you for review:</p>
                        <p><strong>Case ID:</strong> %s</p>
                        <p><strong>Business:</strong> %s</p>
                        <p>Please review the case and take appropriate action.</p>
                    </div>
                    <div class="footer">
                        This is an automated message from %s
                    </div>
                </div>
            </body>
            </html>
            """.formatted(caseId, businessName, appName);
    }

    private String buildVerificationEmailHtml(String caseId, String businessName,
                                               String verificationType, int confidenceScore) {
        String scoreColor = confidenceScore >= 70 ? "#166534" : (confidenceScore >= 50 ? "#92400e" : "#991b1b");
        return """
            <!DOCTYPE html>
            <html>
            <head><style>
                body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                .header { background: #4361ee; color: white; padding: 20px; text-align: center; }
                .content { padding: 20px; background: #f9f9f9; }
                .score { font-size: 36px; font-weight: bold; color: %s; }
                .footer { padding: 20px; text-align: center; color: #666; font-size: 12px; }
            </style></head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Verification Complete</h1>
                    </div>
                    <div class="content">
                        <p>A verification has been completed for a case you are assigned to:</p>
                        <p><strong>Case ID:</strong> %s</p>
                        <p><strong>Business:</strong> %s</p>
                        <p><strong>Verification Type:</strong> %s</p>
                        <p><strong>Confidence Score:</strong> <span class="score">%d%%</span></p>
                        <p>Please review the results in the Merchant Onboarding Platform.</p>
                    </div>
                    <div class="footer">
                        This is an automated message from %s
                    </div>
                </div>
            </body>
            </html>
            """.formatted(scoreColor, caseId, businessName, verificationType, confidenceScore, appName);
    }
}
