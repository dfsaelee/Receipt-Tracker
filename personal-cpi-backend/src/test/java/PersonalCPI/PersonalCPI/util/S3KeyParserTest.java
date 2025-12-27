package PersonalCPI.PersonalCPI.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class S3KeyParserTest {

    @Test 
    void extractUserId_withValidKey_returnsUserId() {
        String key = "receipts/670/251212_image.jpg";

        Long userId = S3KeyParser.extractUserId(key);

        assertThat(userId).isEqualTo(670L);
    }
    
    @Test
    void extractUserId_withDifferentUserId_returnsCorrectId() {
        String key = "receipts/456789/image.png";
        
        Long userId = S3KeyParser.extractUserId(key);
        
        assertThat(userId).isEqualTo(456789L);
    }

    @Test
    void extractUserId_withNullKey_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> S3KeyParser.extractUserId(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid S3 key");
    }

    @Test
    void extractUserId_withEmptyKey_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> S3KeyParser.extractUserId(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid S3 key");
    }

    @Test
    void extractUserId_withInvalidFormat_throwsIllegalArgumentException() {
        String key = "invalid/path/file.jpg";
        
        assertThatThrownBy(() -> S3KeyParser.extractUserId(key))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid S3 key")
            .hasMessageContaining("receipts/{userId}/{filename}");
    }

    @Test
    void extractUserId_withMissingUserId_throwsIllegalArgumentException() {
        String key = "receipts//file.jpg";
        
        assertThatThrownBy(() -> S3KeyParser.extractUserId(key))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void extractUserId_withNonNumericUserId_throwsIllegalArgumentException() {
        String key = "receipts/abc/file.jpg";
        
        assertThatThrownBy(() -> S3KeyParser.extractUserId(key))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void isValidKey_withValidKey_returnsTrue() {
        String key = "receipts/123/file.jpg";
        
        boolean isValid = S3KeyParser.isValidKey(key);
        
        assertThat(isValid).isTrue();
    }

    @Test
    void isValidKey_withInvalidKey_returnsFalse() {
        String key = "invalid/path/file.jpg";
        
        boolean isValid = S3KeyParser.isValidKey(key);
        
        assertThat(isValid).isFalse();
    }

    @Test
    void isValidKey_withNullKey_returnsFalse() {
        boolean isValid = S3KeyParser.isValidKey(null);
        
        assertThat(isValid).isFalse();
    }

    @Test
    void isValidKey_withEmptyKey_returnsFalse() {
        boolean isValid = S3KeyParser.isValidKey("");
        
        assertThat(isValid).isFalse();
    }

    @Test
    void extractUserId_withComplexFilename_extractsCorrectly() {
        String key = "receipts/999/2024-12-26_walmart_receipt_final.png";
        
        Long userId = S3KeyParser.extractUserId(key);
        
        assertThat(userId).isEqualTo(999L);
    }

    @Test
    void extractUserId_withNestedPath_extractsCorrectly() {
        String key = "receipts/777/subfolder/receipt.jpg";
        
        Long userId = S3KeyParser.extractUserId(key);
        
        assertThat(userId).isEqualTo(777L);
    }
}
