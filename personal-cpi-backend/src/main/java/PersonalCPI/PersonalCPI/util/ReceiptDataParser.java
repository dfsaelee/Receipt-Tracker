package PersonalCPI.PersonalCPI.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * Utility class for parsing receipt data strings into proper types.
 */
public class ReceiptDataParser {
    private static final Logger logger = LoggerFactory.getLogger(ReceiptDataParser.class);

    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("MM/dd/yy"),      // Added for dates like 09/01/12
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("MMM dd, yyyy"),
        DateTimeFormatter.ofPattern("MMMM dd, yyyy"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd")
    };

    private ReceiptDataParser() {
        // Utility class - prevent instantiation
    }

    /**
     * Parses a date string using multiple format attempts.
     */
    public static Optional<LocalDate> parseDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return Optional.empty();
        }

        String trimmedDate = dateString.trim();
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return Optional.of(LocalDate.parse(trimmedDate, formatter));
            } catch (DateTimeParseException e) {
                // Try next format
            }
        }

        logger.warn("Could not parse date: '{}', tried {} formats", dateString, DATE_FORMATTERS.length);
        return Optional.empty();
    }

    /**
     * Parses an amount string to BigDecimal, removing currency symbols and commas.
     */
    public static Optional<BigDecimal> parseAmount(String amountString) {
        if (amountString == null || amountString.trim().isEmpty()) {
            return Optional.empty();
        }

        try {
            // Remove currency symbols, commas, and whitespace
            String cleanAmount = amountString.trim()
                .replaceAll("[^0-9.]", "");
            
            if (cleanAmount.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(new BigDecimal(cleanAmount));
        } catch (NumberFormatException e) {
            logger.warn("Could not parse amount: '{}'", amountString, e);
            return Optional.empty();
        }
    }

    /**
     * Parses a quantity string to Integer, removing non-numeric characters.
     */
    public static Optional<Integer> parseQuantity(String quantityString) {
        if (quantityString == null || quantityString.trim().isEmpty()) {
            return Optional.empty();
        }

        try {
            // Remove non-numeric characters
            String cleanQuantity = quantityString.trim()
                .replaceAll("[^0-9]", "");
            
            if (cleanQuantity.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(Integer.parseInt(cleanQuantity));
        } catch (NumberFormatException e) {
            logger.warn("Could not parse quantity: '{}'", quantityString, e);
            return Optional.empty();
        }
    }

    /**
     * Parses a confidence string to Double.
     */
    public static Optional<Double> parseConfidence(String confidenceString) {
        if (confidenceString == null || confidenceString.trim().isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.of(Double.parseDouble(confidenceString.trim()));
        } catch (NumberFormatException e) {
            logger.warn("Could not parse confidence: '{}'", confidenceString, e);
            return Optional.empty();
        }
    }
}
