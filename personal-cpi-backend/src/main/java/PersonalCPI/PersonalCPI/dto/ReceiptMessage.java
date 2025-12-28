package PersonalCPI.PersonalCPI.dto;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * Message format for SQS queue containing extracted receipt data.
 * Sent by Lambda after Textract extraction, consumed by Spring Boot backend.
 */
public class ReceiptMessage {
    private String storeName;
    private BigDecimal amount;
    private Date purchaseDate;
    private String imageKey;
    private Long userId;
    private List<ReceiptItemMessage> items;

    public ReceiptMessage() {
    }

    public ReceiptMessage(String storeName, BigDecimal amount, Date purchaseDate, 
                         String imageKey, Long userId, List<ReceiptItemMessage> items) {
        this.storeName = storeName;
        this.amount = amount;
        this.purchaseDate = purchaseDate;
        this.imageKey = imageKey;
        this.userId = userId;
        this.items = items;
    }

    // Getters and setters
    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Date getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(Date purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public String getImageKey() {
        return imageKey;
    }

    public void setImageKey(String imageKey) {
        this.imageKey = imageKey;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<ReceiptItemMessage> getItems() {
        return items;
    }

    public void setItems(List<ReceiptItemMessage> items) {
        this.items = items;
    }

    public static class ReceiptItemMessage {
        private String itemName;
        private Integer quantity;
        private BigDecimal price;

        public ReceiptItemMessage() {
        }

        public ReceiptItemMessage(String itemName, Integer quantity, BigDecimal price) {
            this.itemName = itemName;
            this.quantity = quantity;
            this.price = price;
        }

        public String getItemName() {
            return itemName;
        }

        public void setItemName(String itemName) {
            this.itemName = itemName;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }
    }
}
