package com.dci.clearance.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.email.from:DCI Clearance System <noreply@exakt.com.ph>}")
    private String fromEmail;

    @Async
    public void sendOtpEmail(String to, String otpCode, String firstName) {
        String subject = "Your DCI Clearance Login OTP";
        String html = """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;font-family:Arial,sans-serif;background:#f5f5f5;">
              <div style="max-width:480px;margin:40px auto;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.1);">
                <div style="background:#1a3a6b;padding:24px;text-align:center;">
                  <h1 style="color:#fff;margin:0;font-size:20px;">DCI Clearance System</h1>
                </div>
                <div style="padding:32px 24px;text-align:center;">
                  <h2 style="color:#333;margin:0 0 8px;">Login Verification</h2>
                  <p style="color:#666;font-size:14px;margin:0 0 24px;">Hi %s, use the OTP below to complete your login:</p>
                  <div style="background:#f0f4ff;border:2px dashed #1a3a6b;border-radius:8px;padding:16px;margin:0 0 24px;">
                    <span style="font-size:32px;font-weight:bold;color:#1a3a6b;letter-spacing:8px;font-family:monospace;">%s</span>
                  </div>
                  <p style="color:#999;font-size:12px;margin:0;">This OTP expires in 5 minutes. Do not share this code.</p>
                </div>
                <div style="background:#f9f9f9;padding:16px;text-align:center;">
                  <p style="color:#aaa;font-size:11px;margin:0;">DCI Clearance Verification System &copy; 2026</p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(firstName, otpCode);
        sendHtml(to, subject, html);
    }

    @Async
    public void sendVerificationEmail(String to, String firstName, String verificationUrl) {
        String subject = "Verify Your Email - DCI Clearance System";
        String html = """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;font-family:Arial,sans-serif;background:#f5f5f5;">
              <div style="max-width:480px;margin:40px auto;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.1);">
                <div style="background:#1a3a6b;padding:24px;text-align:center;">
                  <h1 style="color:#fff;margin:0;font-size:20px;">DCI Clearance System</h1>
                </div>
                <div style="padding:32px 24px;text-align:center;">
                  <h2 style="color:#333;margin:0 0 8px;">Verify Your Email</h2>
                  <p style="color:#666;font-size:14px;margin:0 0 24px;">Hi %s, please click the button below to verify your email address and activate your account.</p>
                  <a href="%s" style="display:inline-block;background:#1a3a6b;color:#fff;text-decoration:none;padding:12px 32px;border-radius:8px;font-weight:bold;font-size:14px;">Verify Email Address</a>
                  <p style="color:#999;font-size:12px;margin:24px 0 0;">This link expires in 24 hours. If you did not register, please ignore this email.</p>
                </div>
                <div style="background:#f9f9f9;padding:16px;text-align:center;">
                  <p style="color:#aaa;font-size:11px;margin:0;">DCI Clearance Verification System &copy; 2026</p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(firstName, verificationUrl);
        sendHtml(to, subject, html);
    }

    @Async
    public void sendVerificationNotification(String to, String firstName, String plateNo, String verifierName, String status) {
        String subject = "DCI Clearance - Application " + status;
        String statusLabel = "HPG_VERIFIED".equals(status) ? "Verified by HPG" : "Validated by DCI";
        String message = "HPG_VERIFIED".equals(status)
                ? "Your vehicle clearance application for plate <strong>" + plateNo + "</strong> has been <strong>verified by HPG</strong>."
                : "Your vehicle clearance application for plate <strong>" + plateNo + "</strong> has been <strong>validated by DCI</strong>. You can now download your certificate.";
        String html = """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;font-family:Arial,sans-serif;background:#f5f5f5;">
              <div style="max-width:480px;margin:40px auto;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.1);">
                <div style="background:#1a3a6b;padding:24px;text-align:center;">
                  <h1 style="color:#fff;margin:0;font-size:20px;">DCI Clearance System</h1>
                </div>
                <div style="padding:32px 24px;text-align:center;">
                  <div style="width:56px;height:56px;background:%s;border-radius:50%%;margin:0 auto 16px;text-align:center;line-height:56px;">
                    <span style="color:#ffffff !important;font-size:28px;line-height:56px;vertical-align:middle;">&#10003;&#65038;</span>
                  </div>
                  <h2 style="color:#333;margin:0 0 8px;">%s</h2>
                  <p style="color:#666;font-size:14px;margin:0 0 8px;">Hi %s,</p>
                  <p style="color:#555;font-size:14px;margin:0 0 24px;">%s</p>
                  <p style="color:#999;font-size:12px;margin:0;">Verified by: %s</p>
                </div>
                <div style="background:#f9f9f9;padding:16px;text-align:center;">
                  <p style="color:#aaa;font-size:11px;margin:0;">DCI Clearance Verification System &copy; 2026</p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(
                "HPG_VERIFIED".equals(status) ? "#2563eb" : "#059669",
                statusLabel, firstName, message, verifierName
            );
        sendHtml(to, subject, html);
    }

    @Async
    public void sendCertificateEmail(String to, String firstName, String certificateNo, String plateNo, String voucherCode) {
        sendCertificateEmail(to, firstName, certificateNo, plateNo, voucherCode, null);
    }

    @Async
    public void sendCertificateEmail(String to, String firstName, String certificateNo, String plateNo, String voucherCode, String pdfBase64) {
        String subject = "DCI Clearance Certificate - " + certificateNo;
        String html = """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;font-family:Arial,sans-serif;background:#f5f5f5;">
              <div style="max-width:480px;margin:40px auto;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.1);">
                <div style="background:#1a3a6b;padding:24px;text-align:center;">
                  <h1 style="color:#fff;margin:0;font-size:20px;">DCI Clearance System</h1>
                </div>
                <div style="padding:32px 24px;text-align:center;">
                  <div style="width:56px;height:56px;background:#059669;border-radius:50%%;margin:0 auto 16px;text-align:center;line-height:56px;">
                    <span style="color:#ffffff !important;font-size:28px;line-height:56px;vertical-align:middle;">&#10003;&#65038;</span>
                  </div>
                  <h2 style="color:#333;margin:0 0 8px;">Certificate Issued</h2>
                  <p style="color:#666;font-size:14px;margin:0 0 24px;">Hi %s, your DCI clearance certificate has been issued.</p>
                  <div style="background:#f0fdf4;border:1px solid #bbf7d0;border-radius:8px;padding:16px;margin:0 0 16px;text-align:left;">
                    <p style="margin:0 0 8px;font-size:13px;color:#555;"><strong>Certificate No:</strong> <code style="font-size:14px;color:#166534;background:#dcfce7;padding:6px 10px;border-radius:4px;display:inline-block;margin-left:6px;">%s</code></p>
                    <p style="margin:12px 0 0;font-size:13px;color:#555;"><strong>Plate Number:</strong> %s</p>
                    <p style="margin:8px 0 0;font-size:13px;color:#555;"><strong>Transaction Code:</strong> %s</p>
                  </div>
                  <p style="color:#999;font-size:12px;margin:0;">Please keep this information for your records.</p>
                </div>
                <div style="background:#f9f9f9;padding:16px;text-align:center;">
                  <p style="color:#aaa;font-size:11px;margin:0;">DCI Clearance Verification System &copy; 2026</p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(firstName, certificateNo, plateNo, voucherCode != null ? voucherCode : "N/A");
        sendHtml(to, subject, html, pdfBase64, "Clearance_Certificate_" + certificateNo + ".pdf");
    }

    private void sendHtml(String to, String subject, String html) {
        sendHtml(to, subject, html, null, null);
    }

    private void sendHtml(String to, String subject, String html, String attachmentBase64, String attachmentFilename) {
        try {
            System.out.println("[SMTP TEST] Attempting to send email to: " + to + " with subject: " + subject);
            sendHtmlSync(to, subject, html, attachmentBase64, attachmentFilename);
            System.out.println("[SMTP TEST] SUCCESS: Email successfully sent to " + to);
            log.info("Email sent to {} : {}", to, subject);
        } catch (Exception e) {
            System.err.println("[SMTP TEST] ERROR: Failed to send email to " + to + ". Reason: " + e.getMessage());
            e.printStackTrace();
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
        }
    }

    public void sendHtmlSync(String to, String subject, String html) throws Exception {
        sendHtmlSync(to, subject, html, null, null);
    }

    public void sendHtmlSync(String to, String subject, String html, String attachmentBase64, String attachmentFilename) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);

        if (attachmentBase64 != null && !attachmentBase64.isBlank()) {
            byte[] pdfBytes = java.util.Base64.getDecoder().decode(attachmentBase64.trim());
            helper.addAttachment(attachmentFilename, new org.springframework.core.io.ByteArrayResource(pdfBytes));
        }

        mailSender.send(message);
    }

    public void sendCertificateEmailSync(String to, String firstName, String certificateNo, String plateNo, String voucherCode) throws Exception {
        sendCertificateEmailSync(to, firstName, certificateNo, plateNo, voucherCode, null);
    }

    public void sendCertificateEmailSync(String to, String firstName, String certificateNo, String plateNo, String voucherCode, String pdfBase64) throws Exception {
        String subject = "DCI Clearance Certificate - " + certificateNo;
        String html = """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;font-family:Arial,sans-serif;background:#f5f5f5;">
              <div style="max-width:480px;margin:40px auto;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.1);">
                <div style="background:#1a3a6b;padding:24px;text-align:center;">
                  <h1 style="color:#fff;margin:0;font-size:20px;">DCI Clearance System</h1>
                </div>
                <div style="padding:32px 24px;text-align:center;">
                  <div style="width:56px;height:56px;background:#059669;border-radius:50%%;margin:0 auto 16px;text-align:center;line-height:56px;">
                    <span style="color:#ffffff !important;font-size:28px;line-height:56px;vertical-align:middle;">&#10003;&#65038;</span>
                  </div>
                  <h2 style="color:#333;margin:0 0 8px;">Certificate Issued</h2>
                  <p style="color:#666;font-size:14px;margin:0 0 24px;">Hi %s, your DCI clearance certificate has been issued.</p>
                  <div style="background:#f0fdf4;border:1px solid #bbf7d0;border-radius:8px;padding:16px;margin:0 0 16px;text-align:left;">
                    <p style="margin:0 0 8px;font-size:13px;color:#555;"><strong>Certificate No:</strong> <code style="font-size:14px;color:#166534;background:#dcfce7;padding:6px 10px;border-radius:4px;display:inline-block;margin-left:6px;">%s</code></p>
                    <p style="margin:12px 0 0;font-size:13px;color:#555;"><strong>Plate Number:</strong> %s</p>
                    <p style="margin:8px 0 0;font-size:13px;color:#555;"><strong>Transaction Code:</strong> %s</p>
                  </div>
                  <p style="color:#999;font-size:12px;margin:0;">Please keep this information for your records.</p>
                </div>
                <div style="background:#f9f9f9;padding:16px;text-align:center;">
                  <p style="color:#aaa;font-size:11px;margin:0;">DCI Clearance Verification System &copy; 2026</p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(firstName, certificateNo, plateNo, voucherCode != null ? voucherCode : "N/A");
        sendHtmlSync(to, subject, html, pdfBase64, "Clearance_Certificate_" + certificateNo + ".pdf");
    }
}
