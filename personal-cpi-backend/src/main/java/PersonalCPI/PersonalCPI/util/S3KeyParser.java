package PersonalCPI.PersonalCPI.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for parsing S3 object keys.
 * Expected key format: receipts/{userId}/{filename}
 */
public class S3KeyParser {
    private static final Pattern USER_ID_PATTERN = Pattern.compile("receipts/(\\d+)/");
    private static final String EXPECTED_FORMAT = "receipts/{userId}/{filename}";

    private S3KeyParser() {
        // Utility class - prevent instantiation
    }

    /**
     * Extracts the user ID from an S3 object key.
     */
    public static Long extractUserId(String objectKey) {
        if (objectKey == null || objectKey.isEmpty()) {
            throw new IllegalArgumentException("Invalid S3 key: " + objectKey + ". Expected format: " + EXPECTED_FORMAT);
        }

        Matcher matcher = USER_ID_PATTERN.matcher(objectKey);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Invalid S3 key: " + objectKey + ". Expected format: " + EXPECTED_FORMAT);
        }

        try {
            String userIdStr = matcher.group(1);
            return Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid S3 key: " + objectKey + ". Expected format: " + EXPECTED_FORMAT);
        }
    }

    /**
     * Validates that an S3 object key matches the expected format.
     */
    public static boolean isValidKey(String objectKey) {
        if (objectKey == null || objectKey.isEmpty()) {
            return false;
        }
        return USER_ID_PATTERN.matcher(objectKey).find();
    }
}
