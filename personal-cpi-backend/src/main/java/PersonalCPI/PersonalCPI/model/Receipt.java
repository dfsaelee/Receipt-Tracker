package PersonalCPI.PersonalCPI.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.antlr.v4.runtime.misc.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "receipts")
@Getter
@Setter
public class Receipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "receipt_id")
    private Long receiptId;


    @NotNull
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "store_name")
    private String storeName = "";

    @Column(name = "purchase_date")
    private LocalDate purchaseDate = LocalDate.now();


    @Column(name = "category_id")
    private Long categoryId;


    @NotNull
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;


    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Foreign key relationships

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", insertable = false, updatable = false)
    private Category category;

    public Receipt() {
    }

    public Receipt(Long userId, String storeName, LocalDate purchaseDate, Long categoryId, BigDecimal amount) {
        this.amount = amount;
        this.storeName = storeName;
        this.userId = userId;
        this.purchaseDate = purchaseDate;
        this.categoryId = categoryId;
    }

    public Long getReceiptId() {
        return receiptId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getStoreName() {
        return storeName;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public User getUser() {
        return user;
    }

    public Category getCategory() {
        return category;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName != null ? storeName : "";
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate != null ? purchaseDate : LocalDate.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (purchaseDate == null) {
            purchaseDate = LocalDate.now();
        }
        if (storeName == null) {
            storeName = "";
        }
    }
}

