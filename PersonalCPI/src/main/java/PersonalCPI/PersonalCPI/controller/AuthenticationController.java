package PersonalCPI.PersonalCPI.controller;

import PersonalCPI.PersonalCPI.dto.*;
import PersonalCPI.PersonalCPI.model.User;
import PersonalCPI.PersonalCPI.responses.LoginResponse;
import PersonalCPI.PersonalCPI.service.AuthenticationService;
import PersonalCPI.PersonalCPI.service.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequestMapping("/auth")
@RestController
public class AuthenticationController {
    private final JwtService jwtService;
    private final AuthenticationService authenticationService;
    public AuthenticationController(JwtService jwtService, AuthenticationService authenticationService) {
            this.jwtService = jwtService;
        this.authenticationService = authenticationService;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> register(@RequestBody RegisterUserDto registerUserDto) {
        try {
            UserResponseDto registeredUser = authenticationService.signup(registerUserDto); // Service returns User
            return ResponseEntity.ok(registeredUser);
        }
        catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT) // handle resend on frontend
                    .body(Map.of("error", "User already exists", "message", e.getMessage()));
        }
        catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Signup failed", "message", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticate(@RequestBody LoginUserDto loginUserDto) {
        try {
            User authenticatedUser = authenticationService.authenticate(loginUserDto); // will throw in authentication service if invalid
            String jwtToken = jwtService.generateToken(authenticatedUser);
            LoginResponse loginResponse = new LoginResponse(jwtService.getExpirationTime(), jwtToken);
            return ResponseEntity.ok(loginResponse);
        }
        catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyUser(@RequestBody VerifyUserDto verifyUserDto) {
        try {
            authenticationService.verifyUser(verifyUserDto);
            return ResponseEntity.ok("Account Verified Successfully");
        }
        catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/resend")
    public ResponseEntity<?> resendVerificationCode(@RequestBody ResendVerificationDto resendVerificationDto) {
        try {
            authenticationService.resendVerificationCode(resendVerificationDto.getEmail());
            return ResponseEntity.ok("verification code sent");
        }
        catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
