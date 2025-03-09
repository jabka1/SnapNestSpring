package team.snapnestspring.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import team.snapnestspring.model.Album;
import team.snapnestspring.model.User;
import team.snapnestspring.repository.AlbumUserRoleRepository;
import team.snapnestspring.repository.UserRepository;

import java.util.List;


@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private UserRepository userRepository;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Autowired
    private AlbumUserRoleRepository albumUserRoleRepository;

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

    public void sendTwoFactorCode(String toEmail, String twoFactorCode) {
        String subject = "Your 2FA Code";
        String message = "Your 2FA code is: " + twoFactorCode;

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(fromEmail);
        mailMessage.setTo(toEmail);
        mailMessage.setSubject(subject);
        mailMessage.setText(message);

        try {
            mailSender.send(mailMessage);
            System.out.println("2FA email sent to: " + toEmail);
        } catch (Exception e) {
            System.err.println("Error sending email: " + e.getMessage());
        }
    }

    public void sendPasswordRecoveryEmail(String username, String token) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        String emailContent = "Use this token to reset your password: " + token;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Password Recovery");
        message.setText(emailContent);
        mailSender.send(message);
    }

    public void sendAlbumInvitationEmails(Album album, List<User> users) {
        for (User user : users) {
            String subject = "You're added to a joint album!";
            String message = "Hello " + user.getUsername() + ",\n\nYou have been added to the album '" + album.getName() + "'.\n";
            sendEmail(user.getEmail(), subject, message);
        }
    }

    private void sendEmail(String toEmail, String subject, String message) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(fromEmail);
        mailMessage.setTo(toEmail);
        mailMessage.setSubject(subject);
        mailMessage.setText(message);

        try {
            mailSender.send(mailMessage);
            System.out.println("Email sent to: " + toEmail);
        } catch (Exception e) {
            System.err.println("Error sending email: " + e.getMessage());
        }
    }
}