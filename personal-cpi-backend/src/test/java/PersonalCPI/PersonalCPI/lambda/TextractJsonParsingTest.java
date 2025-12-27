package PersonalCPI.PersonalCPI.lambda;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.textract.model.AnalyzeExpenseResponse;
import software.amazon.awssdk.services.textract.model.ExpenseDocument;
import software.amazon.awssdk.services.textract.model.ExpenseField;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test parsing of actual Textract AnalyzeExpense JSON responses.
 * This validates that we can correctly extract fields from real Textract output.
 */
class TextractJsonParsingTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parseTextractJson_extractsSummaryFields() throws Exception {
        // Load the sample JSON
        InputStream jsonStream = getClass().getClassLoader()
            .getResourceAsStream("textract-sample-response.json");
        
        assertThat(jsonStream).isNotNull();
        
        JsonNode rootNode = objectMapper.readTree(jsonStream);
        JsonNode expenseDocuments = rootNode.get("ExpenseDocuments");
        
        assertThat(expenseDocuments).isNotNull();
        assertThat(expenseDocuments.isArray()).isTrue();
        assertThat(expenseDocuments.size()).isEqualTo(1);
        
        // Parse first document
        JsonNode firstDoc = expenseDocuments.get(0);
        JsonNode summaryFields = firstDoc.get("SummaryFields");
        
        assertThat(summaryFields).isNotNull();
        assertThat(summaryFields.size()).isEqualTo(3);
        
        // Verify vendor name
        JsonNode vendorField = summaryFields.get(0);
        assertThat(vendorField.get("Type").get("Text").asText()).isEqualTo("VENDOR_NAME");
        assertThat(vendorField.get("ValueDetection").get("Text").asText()).isEqualTo("Walmart Supercenter");
        assertThat(vendorField.get("ValueDetection").get("Confidence").asDouble()).isGreaterThan(95.0);
        
        // Verify date
        JsonNode dateField = summaryFields.get(1);
        assertThat(dateField.get("Type").get("Text").asText()).isEqualTo("INVOICE_RECEIPT_DATE");
        assertThat(dateField.get("ValueDetection").get("Text").asText()).isEqualTo("12/26/2024");
        
        // Verify amount
        JsonNode amountField = summaryFields.get(2);
        assertThat(amountField.get("Type").get("Text").asText()).isEqualTo("AMOUNT_DUE");
        assertThat(amountField.get("ValueDetection").get("Text").asText()).isEqualTo("$45.99");
    }

    @Test
    void parseTextractJson_extractsLineItems() throws Exception {
        // Load the sample JSON
        InputStream jsonStream = getClass().getClassLoader()
            .getResourceAsStream("textract-sample-response.json");
        
        JsonNode rootNode = objectMapper.readTree(jsonStream);
        JsonNode firstDoc = rootNode.get("ExpenseDocuments").get(0);
        JsonNode lineItemGroups = firstDoc.get("LineItemGroups");
        
        assertThat(lineItemGroups).isNotNull();
        assertThat(lineItemGroups.size()).isEqualTo(1);
        
        JsonNode lineItems = lineItemGroups.get(0).get("LineItems");
        assertThat(lineItems.size()).isEqualTo(2);
        
        // Verify first item (Milk)
        JsonNode firstItem = lineItems.get(0);
        JsonNode firstItemFields = firstItem.get("LineItemExpenseFields");
        
        assertThat(firstItemFields.get(0).get("Type").get("Text").asText()).isEqualTo("ITEM");
        assertThat(firstItemFields.get(0).get("ValueDetection").get("Text").asText()).isEqualTo("Milk 2%");
        
        assertThat(firstItemFields.get(1).get("Type").get("Text").asText()).isEqualTo("QUANTITY");
        assertThat(firstItemFields.get(1).get("ValueDetection").get("Text").asText()).isEqualTo("2");
        
        assertThat(firstItemFields.get(2).get("Type").get("Text").asText()).isEqualTo("PRICE");
        assertThat(firstItemFields.get(2).get("ValueDetection").get("Text").asText()).isEqualTo("$3.99");
        
        // Verify second item (Bread)
        JsonNode secondItem = lineItems.get(1);
        JsonNode secondItemFields = secondItem.get("LineItemExpenseFields");
        
        assertThat(secondItemFields.get(0).get("ValueDetection").get("Text").asText()).isEqualTo("Bread Wheat");
        assertThat(secondItemFields.get(1).get("ValueDetection").get("Text").asText()).isEqualTo("1");
        assertThat(secondItemFields.get(2).get("ValueDetection").get("Text").asText()).isEqualTo("$2.50");
    }

    @Test
    void parseTextractJson_verifiesConfidenceLevels() throws Exception {
        // Load the sample JSON
        InputStream jsonStream = getClass().getClassLoader()
            .getResourceAsStream("textract-sample-response.json");
        
        JsonNode rootNode = objectMapper.readTree(jsonStream);
        JsonNode summaryFields = rootNode.get("ExpenseDocuments").get(0).get("SummaryFields");
        
        // All summary fields should have high confidence (>90%)
        for (JsonNode field : summaryFields) {
            double confidence = field.get("ValueDetection").get("Confidence").asDouble();
            assertThat(confidence).isGreaterThan(90.0);
        }
        
        // Line items should have acceptable confidence (>75%)
        JsonNode lineItems = rootNode.get("ExpenseDocuments").get(0)
            .get("LineItemGroups").get(0).get("LineItems");
        
        for (JsonNode item : lineItems) {
            for (JsonNode field : item.get("LineItemExpenseFields")) {
                double confidence = field.get("ValueDetection").get("Confidence").asDouble();
                assertThat(confidence).isGreaterThan(75.0);
            }
        }
    }

    @Test
    void parseTextractJson_extractsAllRequiredFields() throws Exception {
        // Load the sample JSON
        InputStream jsonStream = getClass().getClassLoader()
            .getResourceAsStream("textract-sample-response.json");
        
        JsonNode rootNode = objectMapper.readTree(jsonStream);
        JsonNode summaryFields = rootNode.get("ExpenseDocuments").get(0).get("SummaryFields");
        
        // Verify we have all required fields
        boolean hasVendor = false;
        boolean hasDate = false;
        boolean hasAmount = false;
        
        for (JsonNode field : summaryFields) {
            String fieldType = field.get("Type").get("Text").asText();
            switch (fieldType) {
                case "VENDOR_NAME" -> hasVendor = true;
                case "INVOICE_RECEIPT_DATE" -> hasDate = true;
                case "AMOUNT_DUE" -> hasAmount = true;
            }
        }
        
        assertThat(hasVendor).isTrue();
        assertThat(hasDate).isTrue();
        assertThat(hasAmount).isTrue();
    }
}
