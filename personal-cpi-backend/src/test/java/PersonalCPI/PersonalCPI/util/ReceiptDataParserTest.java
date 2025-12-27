package PersonalCPI.PersonalCPI.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ReceiptDataParserTest {

    // Parsing different date formats 
    @Test
    void parseDate_withYYYYMMDD_returnsDate() {
        Optional<LocalDate> result = ReceiptDataParser.parseDate("2024-12-26");
        
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(LocalDate.of(2024, 12, 26));
    }

    @Test
    void parseDate_withMMDDYYYY_returnsDate() {
        Optional<LocalDate> result = ReceiptDataParser.parseDate("12/26/2024");
        
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(LocalDate.of(2024, 12, 26));
    }

    @Test
    void parseDate_withDDMMYYYY_returnsDate() {
        Optional<LocalDate> result = ReceiptDataParser.parseDate("26/12/2024");
        
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(LocalDate.of(2024, 12, 26));
    }

    @Test
    void parseDate_withShortMonthName_returnsDate() {
        Optional<LocalDate> result = ReceiptDataParser.parseDate("Dec 26, 2024");
        
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(LocalDate.of(2024, 12, 26));
    }

    @Test
    void parseDate_withFullMonthName_returnsDate() {
        Optional<LocalDate> result = ReceiptDataParser.parseDate("December 26, 2024");
        
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(LocalDate.of(2024, 12, 26));
    }

    @Test
    void parseDate_withNull_returnsEmpty() {
        Optional<LocalDate> result = ReceiptDataParser.parseDate(null);
        
        assertThat(result).isEmpty();
    }

    @Test
    void parseDate_withEmptyString_returnsEmpty() {
        Optional<LocalDate> result = ReceiptDataParser.parseDate("");
        
        assertThat(result).isEmpty();
    }

    @Test
    void parseDate_withWhitespace_returnsEmpty() {
        Optional<LocalDate> result = ReceiptDataParser.parseDate("   ");
        
        assertThat(result).isEmpty();
    }

    @Test
    void parseDate_withInvalidFormat_returnsEmpty() {
        Optional<LocalDate> result = ReceiptDataParser.parseDate("invalid date");
        
        assertThat(result).isEmpty();
    }

    @Test
    void parseDate_withExtraWhitespace_trimsAndParses() {
        Optional<LocalDate> result = ReceiptDataParser.parseDate("  2024-12-26  ");
        
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(LocalDate.of(2024, 12, 26));
    }

    // Amount Parsing Tests
    @Test
    void parseAmount_withSimpleAmount_returnsDecimal() {
        Optional<BigDecimal> result = ReceiptDataParser.parseAmount("42.50");
        
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualByComparingTo("42.50");
    }

    @Test
    void parseAmount_withDollarSign_removesAndParses() {
        Optional<BigDecimal> result = ReceiptDataParser.parseAmount("$123.45");
        
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualByComparingTo("123.45");
    }

    @Test
    void parseAmount_withCommas_removesAndParses() {
        Optional<BigDecimal> result = ReceiptDataParser.parseAmount("1,234.56");
        
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualByComparingTo("1234.56");
    }

    @Test
    void parseAmount_withDollarSignAndCommas_parsesCorrectly() {
        Optional<BigDecimal> result = ReceiptDataParser.parseAmount("$2,500.99");
        
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualByComparingTo("2500.99");
    }

    @Test
    void parseAmount_withWhitespace_trimsAndParses() {
        Optional<BigDecimal> result = ReceiptDataParser.parseAmount("  99.99  ");
        
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualByComparingTo("99.99");
    }

    @Test
    void parseAmount_withNull_returnsEmpty() {
        Optional<BigDecimal> result = ReceiptDataParser.parseAmount(null);
        
        assertThat(result).isEmpty();
    }

    @Test
    void parseAmount_withEmptyString_returnsEmpty() {
        Optional<BigDecimal> result = ReceiptDataParser.parseAmount("");
        
        assertThat(result).isEmpty();
    }

    @Test
    void parseAmount_withOnlySymbols_returnsEmpty() {
        Optional<BigDecimal> result = ReceiptDataParser.parseAmount("$$$");
        
        assertThat(result).isEmpty();
    }

    @Test
    void parseAmount_withInvalidFormat_returnsEmpty() {
        Optional<BigDecimal> result = ReceiptDataParser.parseAmount("abc");
        
        assertThat(result).isEmpty();
    }

    @Test
    void parseAmount_withWholeNumber_parsesCorrectly() {
        Optional<BigDecimal> result = ReceiptDataParser.parseAmount("100");
        
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualByComparingTo("100");
    }

    // Quantity Parsing Tests
    @Test
    void parseQuantity_withSimpleNumber_returnsInteger() {
        Optional<Integer> result = ReceiptDataParser.parseQuantity("5");
        
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(5);
    }

    @Test
    void parseQuantity_withExtraCharacters_extractsNumber() {
        Optional<Integer> result = ReceiptDataParser.parseQuantity("x3");
        
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(3);
    }

    @Test
    void parseQuantity_withWhitespace_trimsAndParses() {
        Optional<Integer> result = ReceiptDataParser.parseQuantity("  10  ");
        
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(10);
    }

    @Test
    void parseQuantity_withNull_returnsEmpty() {
        Optional<Integer> result = ReceiptDataParser.parseQuantity(null);
        
        assertThat(result).isEmpty();
    }

    @Test
    void parseQuantity_withEmptyString_returnsEmpty() {
        Optional<Integer> result = ReceiptDataParser.parseQuantity("");
        
        assertThat(result).isEmpty();
    }

    @Test
    void parseQuantity_withNoDigits_returnsEmpty() {
        Optional<Integer> result = ReceiptDataParser.parseQuantity("abc");
        
        assertThat(result).isEmpty();
    }

    @Test
    void parseQuantity_withMixedCharacters_extractsDigits() {
        Optional<Integer> result = ReceiptDataParser.parseQuantity("qty: 7 items");
        
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(7);
    }

    // Confidence Parsing Tests
    @Test
    void parseConfidence_withValidNumber_returnsDouble() {
        Optional<Double> result = ReceiptDataParser.parseConfidence("95.5");
        
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(95.5);
    }

    @Test
    void parseConfidence_withWholeNumber_parsesCorrectly() {
        Optional<Double> result = ReceiptDataParser.parseConfidence("100");
        
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(100.0);
    }

    @Test
    void parseConfidence_withNull_returnsEmpty() {
        Optional<Double> result = ReceiptDataParser.parseConfidence(null);
        
        assertThat(result).isEmpty();
    }

    @Test
    void parseConfidence_withEmptyString_returnsEmpty() {
        Optional<Double> result = ReceiptDataParser.parseConfidence("");
        
        assertThat(result).isEmpty();
    }

    @Test
    void parseConfidence_withInvalidFormat_returnsEmpty() {
        Optional<Double> result = ReceiptDataParser.parseConfidence("not a number");
        
        assertThat(result).isEmpty();
    }

    @Test
    void parseConfidence_withWhitespace_trimsAndParses() {
        Optional<Double> result = ReceiptDataParser.parseConfidence("  85.3  ");
        
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(85.3);
    }
}
