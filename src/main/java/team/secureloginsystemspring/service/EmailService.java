package team.secureloginsystemspring.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;


@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendActivationEmail(String toEmail, String activationToken) {
        String subject = "Account Activation";
        String activationLink = "http://localhost:8080/activate?token=" + activationToken;
        String message = "Click the following link to activate your account: " + activationLink;

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(fromEmail);
        mailMessage.setTo(toEmail);
        mailMessage.setSubject(subject);
        mailMessage.setText(message);

        try {
            mailSender.send(mailMessage);
            System.out.println("Activation email sent to: " + toEmail);
        } catch (Exception e) {
            System.err.println("Error sending email: " + e.getMessage());
        }
    }

}