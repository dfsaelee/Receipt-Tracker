package PersonalCPI.PersonalCPI.repository;

import PersonalCPI.PersonalCPI.model.ReceiptItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for ReceiptItem entity.
 */
@Repository
public interface ReceiptItemRepository extends JpaRepository<ReceiptItem, Long> {

    List<ReceiptItem> findByReceiptId(Long receiptId);

    void deleteByReceiptId(Long receiptId);

    long countByReceiptId(Long receiptId);
}
