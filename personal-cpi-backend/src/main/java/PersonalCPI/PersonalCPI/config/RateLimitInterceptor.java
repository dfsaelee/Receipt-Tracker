package PersonalCPI.PersonalCPI.config;

import PersonalCPI.PersonalCPI.exception.RateLimitExceededException;
import PersonalCPI.PersonalCPI.model.User;
import PersonalCPI.PersonalCPI.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    
    private final RateLimitService rateLimitService;
    
    @Autowired
    public RateLimitInterceptor(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Skip rate limiting for auth endpoints
        String requestURI = request.getRequestURI();
        if (requestURI.startsWith("/auth/")) {
            return true;
        }
        
        // Get authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || 
            !(authentication.getPrincipal() instanceof User)) {
            return true; // Let security filter handle authentication
        }
        
        User user = (User) authentication.getPrincipal();
        Long userId = user.getId();
        
        // Determine rate limit type based on endpoint
        RateLimitType limitType = determineRateLimitType(requestURI);
        
        // Check rate limit
        if (!rateLimitService.tryConsume(userId, limitType)) {
            long retryAfter = rateLimitService.getSecondsUntilRefill(userId, limitType);
            
            // Add rate limit headers
            response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimitService.getLimit(limitType)));
            response.setHeader("X-RateLimit-Remaining", "0");
            response.setHeader("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() / 1000 + retryAfter));
            
            throw new RateLimitExceededException(
                    "Too many requests. Please try again later.",
                    retryAfter
            );
        }
        
        // Add rate limit headers to successful requests
        long remaining = rateLimitService.getAvailableTokens(userId, limitType);
        long resetTime = System.currentTimeMillis() / 1000 + 60; // Reset in 1 minute
        
        response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimitService.getLimit(limitType)));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        response.setHeader("X-RateLimit-Reset", String.valueOf(resetTime));
        
        return true;
    }
    
    private RateLimitType determineRateLimitType(String requestURI) {
        if (requestURI.contains("/upload")) {
            return RateLimitType.UPLOAD;
        } else if (requestURI.startsWith("/api/admin/")) {
            return RateLimitType.ADMIN;
        } else {
            return RateLimitType.API;
        }
    }
}
