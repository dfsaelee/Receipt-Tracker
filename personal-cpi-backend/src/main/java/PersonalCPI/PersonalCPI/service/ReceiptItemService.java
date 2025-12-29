package PersonalCPI.PersonalCPI.service;

import PersonalCPI.PersonalCPI.dto.ReceiptItemDto;
import PersonalCPI.PersonalCPI.model.Receipt;
import PersonalCPI.PersonalCPI.model.ReceiptItem;
import PersonalCPI.PersonalCPI.repository.ReceiptItemRepository;
import PersonalCPI.PersonalCPI.repository.ReceiptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service layer for ReceiptItem operations.
 * Handles business logic for managing receipt items.
 */
@Service
public class ReceiptItemService {
    
    private final ReceiptItemRepository receiptItemRepository;
    private final ReceiptRepository receiptRepository;

    @Autowired
    public ReceiptItemService(ReceiptItemRepository receiptItemRepository, ReceiptRepository receiptRepository) {
        this.receiptItemRepository = receiptItemRepository;
        this.receiptRepository = receiptRepository;
    }

    /**
     * Get all items for a specific receipt
     * @param userId User ID for authorization
     * @param receiptId Receipt ID
     * @return List of receipt items
     */
    @Transactional(readOnly = true)
    public List<ReceiptItemDto> getReceiptItems(Long userId, Long receiptId) {
        // Verify receipt belongs to user
        verifyReceiptOwnership(userId, receiptId);
        
        List<ReceiptItem> items = receiptItemRepository.findByReceiptId(receiptId);
        return items.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific receipt item by ID
     * @param userId User ID for authorization
     * @param receiptItemId Receipt item ID
     * @return Receipt item DTO
     */
    @Transactional(readOnly = true)
    public ReceiptItemDto getReceiptItemById(Long userId, Long receiptItemId) {
        ReceiptItem item = receiptItemRepository.findById(receiptItemId)
                .orElseThrow(() -> new IllegalArgumentException("Receipt item not found"));
        
        // Verify receipt belongs to user
        verifyReceiptOwnership(userId, item.getReceiptId());
        
        return convertToDto(item);
    }

    /**
     * Create a new receipt item
     * @param userId User ID for authorization
     * @param itemDto Receipt item data
     * @return Created receipt item DTO
     */
    @Transactional
    public ReceiptItemDto createReceiptItem(Long userId, ReceiptItemDto itemDto) {
        // Verify receipt belongs to user
        verifyReceiptOwnership(userId, itemDto.getReceiptId());
        
        ReceiptItem item = new ReceiptItem();
        item.setReceiptId(itemDto.getReceiptId());
        item.setItemName(itemDto.getItemName());
        item.setQuantity(itemDto.getQuantity());
        item.setUnitPrice(itemDto.getUnitPrice());
        
        ReceiptItem savedItem = receiptItemRepository.save(item);
        return convertToDto(savedItem);
    }

    /**
     * Update an existing receipt item
     * @param userId User ID for authorization
     * @param receiptItemId Receipt item ID
     * @param itemDto Updated receipt item data
     * @return Updated receipt item DTO
     */
    @Transactional
    public ReceiptItemDto updateReceiptItem(Long userId, Long receiptItemId, ReceiptItemDto itemDto) {
        ReceiptItem item = receiptItemRepository.findById(receiptItemId)
                .orElseThrow(() -> new IllegalArgumentException("Receipt item not found"));
        
        // Verify receipt belongs to user
        verifyReceiptOwnership(userId, item.getReceiptId());
        
        item.setItemName(itemDto.getItemName());
        item.setQuantity(itemDto.getQuantity());
        item.setUnitPrice(itemDto.getUnitPrice());
        
        ReceiptItem updatedItem = receiptItemRepository.save(item);
        return convertToDto(updatedItem);
    }

    /**
     * Delete a receipt item
     * @param userId User ID for authorization
     * @param receiptItemId Receipt item ID
     */
    @Transactional
    public void deleteReceiptItem(Long userId, Long receiptItemId) {
        ReceiptItem item = receiptItemRepository.findById(receiptItemId)
                .orElseThrow(() -> new IllegalArgumentException("Receipt item not found"));
        
        // Verify receipt belongs to user
        verifyReceiptOwnership(userId, item.getReceiptId());
        
        receiptItemRepository.deleteById(receiptItemId);
    }

    /**
     * Create multiple receipt items at once
     * @param userId User ID for authorization
     * @param receiptId Receipt ID
     * @param itemDtos List of receipt items to create
     * @return List of created receipt item DTOs
     */
    @Transactional
    public List<ReceiptItemDto> createReceiptItems(Long userId, Long receiptId, List<ReceiptItemDto> itemDtos) {
        // Verify receipt belongs to user
        verifyReceiptOwnership(userId, receiptId);
        
        List<ReceiptItem> items = itemDtos.stream()
                .map(dto -> {
                    ReceiptItem item = new ReceiptItem();
                    item.setReceiptId(receiptId);
                    item.setItemName(dto.getItemName());
                    item.setQuantity(dto.getQuantity());
                    item.setUnitPrice(dto.getUnitPrice());
                    return item;
                })
                .collect(Collectors.toList());
        
        List<ReceiptItem> savedItems = receiptItemRepository.saveAll(items);
        return savedItems.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Delete all items for a receipt
     * @param userId User ID for authorization
     * @param receiptId Receipt ID
     */
    @Transactional
    public void deleteAllReceiptItems(Long userId, Long receiptId) {
        // Verify receipt belongs to user
        verifyReceiptOwnership(userId, receiptId);
        
        receiptItemRepository.deleteByReceiptId(receiptId);
    }

    /**
     * Verify that a receipt belongs to the specified user
     * @param userId User ID
     * @param receiptId Receipt ID
     * @throws IllegalArgumentException if receipt not found or doesn't belong to user
     */
    private void verifyReceiptOwnership(Long userId, Long receiptId) {
        Optional<Receipt> receipt = receiptRepository.findById(receiptId);
        
        if (receipt.isEmpty()) {
            throw new IllegalArgumentException("Receipt not found");
        }
        
        if (!receipt.get().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Receipt does not belong to user");
        }
    }

    /**
     * Convert ReceiptItem entity to DTO
     * @param item ReceiptItem entity
     * @return ReceiptItemDto
     */
    private ReceiptItemDto convertToDto(ReceiptItem item) {
        return new ReceiptItemDto(
                item.getReceiptItemId(),
                item.getReceiptId(),
                item.getItemName(),
                item.getQuantity(),
                item.getUnitPrice()
        );
    }
}
