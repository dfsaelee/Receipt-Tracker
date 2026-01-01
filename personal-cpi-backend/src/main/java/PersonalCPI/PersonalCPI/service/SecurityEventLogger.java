package PersonalCPI.PersonalCPI.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SecurityEventLogger {
    
    private static final Logger logger = LoggerFactory.getLogger("SECURITY");
    
    public void logAuthenticationFailure(String username, String reason) {
        logger.warn("Authentication failed for user '{}': {}", username, reason);
    }
    
    public void logAuthorizationFailure(Long userId, String resource, String action) {
        logger.warn("Authorization failed: User {} attempted to {} resource '{}'", 
                userId, action, resource);
    }
    
    public void logRateLimitViolation(Long userId, String endpoint) {
        logger.warn("Rate limit exceeded: User {} on endpoint '{}'", userId, endpoint);
    }
    
    public void logInvalidFileUpload(Long userId, String filename, String reason) {
        logger.warn("Invalid file upload: User {} attempted to upload '{}' - {}", 
                userId, filename, reason);
    }
    
    public void logSuspiciousInput(Long userId, String field, String value, String reason) {
        logger.warn("Suspicious input detected: User {} - field '{}' - reason: {}", 
                userId, field, reason);
    }
    
    public void logAccessDenied(Long userId, Long resourceId, String resourceType) {
        logger.warn("Access denied: User {} attempted to access {} with ID {}", 
                userId, resourceType, resourceId);
    }
    
    public void logInvalidInput(String field, String value, String reason) {
        logger.warn("Invalid input: field '{}' - reason: {}", field, reason);
    }
}
