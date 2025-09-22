// provide functions for authentication
package PersonalCPI.PersonalCPI.config;

import PersonalCPI.PersonalCPI.model.User;
import PersonalCPI.PersonalCPI.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

@Configuration
public class ApplicationConfiguration {
    private final UserRepository userRepository;
    public ApplicationConfiguration(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean // encoding passwords
    BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(); // Depreciated function

        authProvider.setUserDetailsService(userDetailsService()); // depreciated function
        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }

    // test:
    @Bean
    UserDetailsService userDetailsService() {
        return identifier -> {
            System.out.println("UserDetailsService called with: '" + identifier + "'");

            // try by username
            Optional<User> user = userRepository.findByUsername(identifier);

            // then try by email
            if (user.isEmpty()) {
                user = userRepository.findByEmail(identifier);
            }

            if (user.isPresent()) {
                System.out.println("Found user: " + user.get().getUsername());
            } else {
                System.out.println("No user found with identifier: " + identifier);
            }
            return user.orElseThrow(() -> new UsernameNotFoundException("User not found"));
        };
    }
}
