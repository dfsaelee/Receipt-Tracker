package PersonalCPI.PersonalCPI.util;

import java.math.BigDecimal;
import java.util.Optional;

public class TestAmountParser {
    public static void main(String[] args) {
        System.out.println("Testing Amount Parser with European Format");
        System.out.println("===========================================\n");
        
        // Test cases from the Walmart receipt
        testAmount("51,74", "51.74");
        testAmount("2,50", "2.50");
        testAmount("4.38", "4.38");
        
        // Additional test cases
        testAmount("1.234,56", "1234.56");
        testAmount("1,234.56", "1234.56");
        testAmount("$51,74", "51.74");
        testAmount("€2,50", "2.50");
        
        System.out.println("\n✅ All tests completed!");
    }
    
    private static void testAmount(String input, String expected) {
        Optional<BigDecimal> result = ReceiptDataParser.parseAmount(input);
        BigDecimal expectedValue = new BigDecimal(expected);
        
        if (result.isPresent() && result.get().compareTo(expectedValue) == 0) {
            System.out.println("✅ PASS: '" + input + "' → " + result.get());
        } else {
            System.out.println("❌ FAIL: '" + input + "' → " + 
                (result.isPresent() ? result.get() : "empty") + 
                " (expected: " + expected + ")");
        }
    }
}
