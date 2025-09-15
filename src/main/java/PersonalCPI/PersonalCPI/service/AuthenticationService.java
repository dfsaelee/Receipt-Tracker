// logic for creating new users and authenticating users with email
package PersonalCPI.PersonalCPI.service;

import PersonalCPI.PersonalCPI.dto.LoginUserDto;
import PersonalCPI.PersonalCPI.dto.RegisterUserDto;
import PersonalCPI.PersonalCPI.dto.VerifyUserDto;
import PersonalCPI.PersonalCPI.model.User;
import PersonalCPI.PersonalCPI.repository.UserRepository;
import jakarta.mail.MessagingException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    public AuthenticationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            EmailService emailService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.emailService = emailService;
    }

    // if new user Signs up
    public User signup(RegisterUserDto input) {
        User user = new User(input.getUsername(), input.getEmail(), passwordEncoder.encode(input.getPassword()));
        user.setVerificationCode(generateVerificationCode());
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(15));
        user.setEnabled(false);
        userRepository.save(user); // FIX, might not want to saver user before auth
        sendVerificationEmail(user);
        return user;
    }

    // if user logs in from login screen
    public User authenticate(LoginUserDto input) {
        User user = userRepository.findByEmail(input.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found")); // if not existing
        if (!user.isEnabled()) {
            throw new RuntimeException("Account not verified. Please verify"); // account not verified yet
        }
        // actual authentication for user logging in
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        input.getEmail(),
                        input.getPassword()
                )
        );
        return  user;
    }

    // handles checking the verification code, and validates user if correct code
    public void verifyUser(VerifyUserDto input) {
        Optional<User> optionalUser = userRepository.findByEmail(input.getEmail());
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("Verification Code has expired");
            }
            if (user.getVerificationCode().equals(input.getVerificationCode())) {
                user.setEnabled(true);
                user.setVerificationCode(null);
                user.setVerificationCodeExpiresAt(null);
                userRepository.save(user); // valid user, and no longer needs verification code
            } else {
                throw new RuntimeException("Invalid verification code");
            }
        } else {
            throw new RuntimeException("User not found");
        }
    }

    // resend functionality
    public void resendVerificationCode(String email) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (user.isEnabled()) {
                throw new RuntimeException("Account is already verified");
            }
            user.setVerificationCode(generateVerificationCode());
            user.setVerificationCodeExpiresAt(LocalDateTime.now().plusHours(1));
            userRepository.save(user);
        } else {
            throw new RuntimeException("User not found");
        }
    }

    // send verification email
    public  void sendVerificationEmail(User user) {
        String subject = "Account Verification";
        String verificationCode = user.getVerificationCode();
        String htmlMessage =
                "<!DOCTYPE html>" +
                        "<html lang='en'>" +
                        "<head>" +
                        "  <meta charset='UTF-8'>" +
                        "  <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                        "  <style>" +
                        "    body { font-family: Arial, sans-serif; background-color: #f7f7f7; padding: 20px; }" +
                        "    .container { max-width: 600px; margin: auto; background: #ffffff; padding: 20px; border-radius: 8px; }" +
                        "    h2 { color: #333333; }" +
                        "    .code-box { display: inline-block; padding: 12px 20px; margin-top: 20px;" +
                        "                background-color: #4CAF50; color: #ffffff; font-size: 18px;" +
                        "                border-radius: 4px; letter-spacing: 2px; }" +
                        "    .footer { margin-top: 30px; font-size: 12px; color: #777777; }" +
                        "  </style>" +
                        "</head>" +
                        "<body>" +
                        "  <div class='container'>" +
                        "    <h2>Account Verification</h2>" +
                        "    <p>Please use the following code to verify your account:</p>" +
                        "    <div class='code-box'>" + verificationCode + "</div>" +
                        "    <p class='footer'>If you didn’t request this code, you can ignore this email.</p>" +
                        "  </div>" +
                        "</body>" +
                        "</html>";
        try {
            emailService.sendVerificationEmail(user.getEmail(), subject, htmlMessage);
        } catch(MessagingException e) {
           e.printStackTrace();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // generates random 6 digit verification code
    private String generateVerificationCode() {
        Random random = new Random();
        int code = random.nextInt(900000) + 100000;
        return String.valueOf(code);
    }
}
