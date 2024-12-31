/*
package com.imdc.milkdespencer.common;

import java.util.Properties;
import java.util.Random;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class PasswordResetEmailSender {

    private final String smtpHost;
    private final String smtpPort;
    private final String senderEmail;
    private final String senderPassword;

    public PasswordResetEmailSender(String smtpHost, String smtpPort, String senderEmail, String senderPassword) {
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.senderEmail = senderEmail;
        this.senderPassword = senderPassword;
    }

    private String generateOTP() {
        // Generate a random 6-digit OTP
        Random random = new Random();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }

    public void sendPasswordResetEmail(String recipientEmail) {
        // Generate OTP
        String otp = generateOTP();

        // Email properties
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", smtpHost);
        properties.put("mail.smtp.port", smtpPort);

        // Get the Session object
        Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, senderPassword);
            }
        });

        try {
            // Create a default MimeMessage object
            Message message = new MimeMessage(session);

            // Set From: header field
            message.setFrom(new InternetAddress(senderEmail));

            // Set To: header field
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));

            // Set Subject: header field
            message.setSubject("Password Reset OTP");

            // Set the email body
            String body = "Your OTP for password reset is: " + otp;
            message.setText(body);

            // Send the message
            Transport.send(message);

            System.out.println("Password reset email sent successfully.");

        } catch (MessagingException e) {
            e.printStackTrace();
            System.err.println("Error sending email: " + e.getMessage());
        }
    }

    // Example usage
    */
/*public static void sendMail() {
        String smtpHost = "your_smtp_server.com";
        String smtpPort = "587";
        String senderEmail = "your_email@gmail.com";
        String senderPassword = "your_email_password";

        PasswordResetEmailSender emailSender = new PasswordResetEmailSender(smtpHost, smtpPort, senderEmail, senderPassword);
        String recipientEmail = "recipient_email@example.com";

        emailSender.sendPasswordResetEmail(recipientEmail);
    }*//*

}
*/
