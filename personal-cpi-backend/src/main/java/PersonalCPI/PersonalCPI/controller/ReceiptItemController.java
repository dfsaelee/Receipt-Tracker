package PersonalCPI.PersonalCPI.controller;

import PersonalCPI.PersonalCPI.dto.ReceiptItemDto;
import PersonalCPI.PersonalCPI.model.User;
import PersonalCPI.PersonalCPI.service.ReceiptItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for ReceiptItem operations.
 * Handles HTTP requests for managing receipt items.
 */
@RestController
@RequestMapping("/api/receipt-items")
public class ReceiptItemController {

    private final ReceiptItemService receiptItemService;

    @Autowired
    public ReceiptItemController(ReceiptItemService receiptItemService) {
        this.receiptItemService = receiptItemService;
    }

    /**
     * Get all items for a specific receipt
     */
    @GetMapping("/receipt/{receiptId}")
    public ResponseEntity<?> getReceiptItems(@PathVariable Long receiptId) {
        try {
            Long userId = getAuthenticatedUserId();
            List<ReceiptItemDto> items = receiptItemService.getReceiptItems(userId, receiptId);
            return ResponseEntity.ok(items);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve receipt items"));
        }
    }

    /**
     * Get a specific receipt item by ID
     */
    @GetMapping("/{receiptItemId}")
    public ResponseEntity<?> getReceiptItemById(@PathVariable Long receiptItemId) {
        try {
            Long userId = getAuthenticatedUserId();
            ReceiptItemDto item = receiptItemService.getReceiptItemById(userId, receiptItemId);
            return ResponseEntity.ok(item);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve receipt item"));
        }
    }

    /**
     * Create a new receipt item
     */
    @PostMapping
    public ResponseEntity<?> createReceiptItem(@RequestBody ReceiptItemDto itemDto) {
        try {
            Long userId = getAuthenticatedUserId();
            ReceiptItemDto createdItem = receiptItemService.createReceiptItem(userId, itemDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdItem);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create receipt item"));
        }
    }

    /**
     * Create multiple receipt items at once
     */
    @PostMapping("/receipt/{receiptId}/batch")
    public ResponseEntity<?> createReceiptItems(
            @PathVariable Long receiptId,
            @RequestBody List<ReceiptItemDto> itemDtos) {
        try {
            Long userId = getAuthenticatedUserId();
            List<ReceiptItemDto> createdItems = receiptItemService.createReceiptItems(userId, receiptId, itemDtos);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdItems);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create receipt items"));
        }
    }

    /**
     * Update an existing receipt item
     */
    @PutMapping("/{receiptItemId}")
    public ResponseEntity<?> updateReceiptItem(
            @PathVariable Long receiptItemId,
            @RequestBody ReceiptItemDto itemDto) {
        try {
            Long userId = getAuthenticatedUserId();
            ReceiptItemDto updatedItem = receiptItemService.updateReceiptItem(userId, receiptItemId, itemDto);
            return ResponseEntity.ok(updatedItem);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update receipt item"));
        }
    }

    /**
     * Delete a receipt item
     */
    @DeleteMapping("/{receiptItemId}")
    public ResponseEntity<?> deleteReceiptItem(@PathVariable Long receiptItemId) {
        try {
            Long userId = getAuthenticatedUserId();
            receiptItemService.deleteReceiptItem(userId, receiptItemId);
            return ResponseEntity.ok(Map.of("message", "Receipt item deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete receipt item"));
        }
    }

    /**
     * Delete all items for a receipt
     */
    @DeleteMapping("/receipt/{receiptId}")
    public ResponseEntity<?> deleteAllReceiptItems(@PathVariable Long receiptId) {
        try {
            Long userId = getAuthenticatedUserId();
            receiptItemService.deleteAllReceiptItems(userId, receiptId);
            return ResponseEntity.ok(Map.of("message", "All receipt items deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete receipt items"));
        }
    }

    /**
     * Get authenticated user ID from security context
     */
    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User is not authenticated");
        }

        User user = (User) authentication.getPrincipal();
        return user.getId();
    }
}
