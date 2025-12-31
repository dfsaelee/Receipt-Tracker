// tells which urls can access HTTP methods, and sets rules for each url
package PersonalCPI.PersonalCPI.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    private final AuthenticationProvider authenticationProvider;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

   public SecurityConfiguration(
           JwtAuthenticationFilter jwtAuthenticationFilter,
           AuthenticationProvider authenticationProvider
   ) {
       this.authenticationProvider = authenticationProvider;
       this.jwtAuthenticationFilter = jwtAuthenticationFilter;
   }

    @Bean // configure security chain and rules
    public SecurityFilterChain securityFilterChain(HttpSecurity http)  throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Add this line
                .csrf(csrf ->csrf.disable()) // disable csrf protection
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/auth/**").permitAll() // authorize anything with auth header else not
                        .requestMatchers("/api/admin/**").authenticated() // Admin endpoints require authentication
                        .requestMatchers("/api/receipts/**").authenticated() // permitAll
                        .requestMatchers("/api/cpi/**").authenticated() // CPI endpoints require authentication
                        .anyRequest().authenticated()
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // every session needs jwt
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean // configure cors settings, of what each our specified url can access
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // set urls that can be accessed
        configuration.setAllowedOrigins(List.of("https://backend.com",
                "http://localhost:8080",
                "http://localhost:3000",
                "https://yourdomain.com")); // FIX: Place actual domain here

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
