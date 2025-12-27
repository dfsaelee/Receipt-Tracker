package PersonalCPI.PersonalCPI.dto;

import PersonalCPI.PersonalCPI.model.Receipt;
import PersonalCPI.PersonalCPI.model.ReceiptItem;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO that wraps a Receipt with its associated ReceiptItems.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptWithItems {
    private Receipt receipt;
    private List<ReceiptItem> items = new ArrayList<>();

    public ReceiptWithItems(Receipt receipt) {
        this.receipt = receipt;
        this.items = new ArrayList<>();
    }

    public void addItem(ReceiptItem item) {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        this.items.add(item);
    }

    public boolean hasItems() {
        return items != null && !items.isEmpty();
    }

    public int getItemCount() {
        return items != null ? items.size() : 0;
    }
}
