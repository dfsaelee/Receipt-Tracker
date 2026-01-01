package PersonalCPI.PersonalCPI.util;

import org.owasp.encoder.Encode;

public class InputSanitizer {
    
    /**
     * Sanitize text input to prevent XSS attacks
     */
    public static String sanitizeText(String input) {
        if (input == null) {
            return null;
        }
        
        // Trim whitespace
        String sanitized = input.trim();
        
        // Encode HTML to prevent XSS
        sanitized = Encode.forHtml(sanitized);
        
        return sanitized;
    }
    
    /**
     * Sanitize and limit text length
     */
    public static String sanitizeText(String input, int maxLength) {
        String sanitized = sanitizeText(input);
        if (sanitized != null && sanitized.length() > maxLength) {
            return sanitized.substring(0, maxLength);
        }
        return sanitized;
    }
    
    /**
     * Remove all HTML tags from input
     */
    public static String stripHtml(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll("<[^>]*>", "").trim();
    }
    
    /**
     * Sanitize filename to prevent path traversal
     */
    public static String sanitizeFilename(String filename) {
        if (filename == null) {
            return "file";
        }
        
        // Remove path separators and dangerous characters
        return filename
                .replaceAll("[/\\\\]", "")
                .replaceAll("\\.{2,}", ".")
                .replaceAll("[^a-zA-Z0-9._-]", "_")
                .substring(0, Math.min(filename.length(), 255));
    }
}
