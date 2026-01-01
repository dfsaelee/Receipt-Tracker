package PersonalCPI.PersonalCPI.validation;

import PersonalCPI.PersonalCPI.exception.FileValidationException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

public class FileUploadValidator {
    
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "application/pdf"
    );
    
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            ".jpg", ".jpeg", ".png", ".pdf"
    );
    
    // Magic numbers (file signatures) for allowed file types
    private static final byte[] JPEG_MAGIC = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};
    private static final byte[] PDF_MAGIC = new byte[]{0x25, 0x50, 0x44, 0x46}; // %PDF
    
    public static void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileValidationException("File is required");
        }
        
        validateFileSize(file);
        validateContentType(file);
        validateFileExtension(file);
        validateFileSignature(file);
        validateFilename(file);
    }
    
    private static void validateFileSize(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileValidationException("File too large (max 10MB)");
        }
        
        if (file.getSize() == 0) {
            throw new FileValidationException("File is empty");
        }
    }
    
    private static void validateContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new FileValidationException("Only JPEG, PNG, and PDF files allowed");
        }
    }
    
    private static void validateFileExtension(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new FileValidationException("Filename is required");
        }
        
        String lowercaseFilename = filename.toLowerCase();
        boolean hasValidExtension = ALLOWED_EXTENSIONS.stream()
                .anyMatch(lowercaseFilename::endsWith);
        
        if (!hasValidExtension) {
            throw new FileValidationException("Invalid file type");
        }
    }
    
    private static void validateFileSignature(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            byte[] fileHeader = new byte[8];
            int bytesRead = is.read(fileHeader);
            
            if (bytesRead < 3) {
                throw new FileValidationException("File is corrupted");
            }
            
            boolean isValidSignature = 
                    startsWithMagicNumber(fileHeader, JPEG_MAGIC) ||
                    startsWithMagicNumber(fileHeader, PNG_MAGIC) ||
                    startsWithMagicNumber(fileHeader, PDF_MAGIC);
            
            if (!isValidSignature) {
                throw new FileValidationException("Invalid file type");
            }
        } catch (IOException e) {
            throw new FileValidationException("Failed to read file", e);
        }
    }
    
    private static boolean startsWithMagicNumber(byte[] fileHeader, byte[] magicNumber) {
        if (fileHeader.length < magicNumber.length) {
            return false;
        }
        
        for (int i = 0; i < magicNumber.length; i++) {
            if (fileHeader[i] != magicNumber[i]) {
                return false;
            }
        }
        return true;
    }
    
    private static void validateFilename(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            return;
        }
        
        // Check for path traversal attempts
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new FileValidationException("Invalid filename");
        }
        
        // Check for null bytes
        if (filename.contains("\0")) {
            throw new FileValidationException("Invalid filename");
        }
        
        // Check filename length
        if (filename.length() > 255) {
            throw new FileValidationException("Filename too long");
        }
    }
    
    public static String sanitizeFilename(String filename) {
        if (filename == null) {
            return "file";
        }
        
        // Remove path traversal characters and other dangerous characters
        return filename
                .replaceAll("[^a-zA-Z0-9._-]", "_")
                .replaceAll("\\.{2,}", ".")
                .substring(0, Math.min(filename.length(), 255));
    }
}
