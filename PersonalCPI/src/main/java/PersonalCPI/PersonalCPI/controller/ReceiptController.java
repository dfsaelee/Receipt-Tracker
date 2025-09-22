package PersonalCPI.PersonalCPI.controller;

import PersonalCPI.PersonalCPI.dto.MonthlySpendingDto;
import PersonalCPI.PersonalCPI.dto.ReceiptCreateDto;
import PersonalCPI.PersonalCPI.dto.ReceiptResponseDto;
import PersonalCPI.PersonalCPI.dto.SpendingSummaryDto;
import PersonalCPI.PersonalCPI.model.User;
import PersonalCPI.PersonalCPI.service.JwtService;
import PersonalCPI.PersonalCPI.service.ReceiptService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api/receipts")
public class ReceiptController {
    private final ReceiptService receiptService;
    private final JwtService jwtService;

    @Autowired
    public ReceiptController(ReceiptService receiptService, JwtService jwtService) {
        this.jwtService = jwtService;
        this.receiptService = receiptService;
    }

    // get user id from jwt token
    private Long getUserIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing or invalid Authorization header");
        }

        String jwt = authHeader.substring(7); // Remove "Bearer " prefix
        String username = jwtService.extractUsername(jwt);

        if (username == null) {
            throw new IllegalArgumentException("Invalid JWT token");
        }

        return jwtService.extractUserId(jwt); // You'll need to implement this method
    }
    @PostMapping
    public ResponseEntity<?> createReceipt( @RequestBody ReceiptCreateDto receiptDto) {
        try {
            Long userId = getAuthenticatedUserId();
            ReceiptResponseDto createdReceipt = receiptService.createReceipt(userId, receiptDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdReceipt);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create receipt"));
        }
    }

     //Get all receipts for the authenticated user
    @GetMapping("/all")
    public ResponseEntity<?> getUserReceipts() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            Long userId = getAuthenticatedUserId();
            List<ReceiptResponseDto> receipts = receiptService.getUserReceipts(userId);
            return ResponseEntity.ok(receipts);
        } catch (Exception e) {
            System.out.println("Controller error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve receipts"));
        }
    }

     // Get receipts by date range
    @GetMapping("/date-range")
    public ResponseEntity<?> getUserReceiptsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            Long userId = getAuthenticatedUserId();
            List<ReceiptResponseDto> receipts = receiptService.getUserReceiptsByDateRange(userId, startDate, endDate);
            return ResponseEntity.ok(receipts);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve receipts"));
        }
    }

    // Get receipts by category
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<?> getUserReceiptsByCategory(@PathVariable Long categoryId) {
        try {
            Long userId = getAuthenticatedUserId();
            List<ReceiptResponseDto> receipts = receiptService.getUserReceiptsByCategory(userId, categoryId);
            return ResponseEntity.ok(receipts);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve receipts"));
        }
    }

    // Get a specific receipt by ID
    @GetMapping("/{receiptId}")
    public ResponseEntity<?> getReceiptById(@PathVariable Long receiptId) {
        try {
            Long userId = getAuthenticatedUserId();
            ReceiptResponseDto receipt = receiptService.getReceiptById(userId, receiptId);
            return ResponseEntity.ok(receipt);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve receipt"));
        }
    }

    // Update an existing receipt
    @PutMapping("/{receiptId}")
    public ResponseEntity<?> updateReceipt(@PathVariable Long receiptId, @RequestBody ReceiptCreateDto receiptDto) {
        try {
            Long userId = getAuthenticatedUserId();
            ReceiptResponseDto updatedReceipt = receiptService.updateReceipt(userId, receiptId, receiptDto);
            return ResponseEntity.ok(updatedReceipt);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update receipt"));
        }
    }

     // Delete a receipt
    @DeleteMapping("/{receiptId}")
    public ResponseEntity<?> deleteReceipt(@PathVariable Long receiptId) {
        try {
            Long userId = getAuthenticatedUserId();
            receiptService.deleteReceipt(userId, receiptId);
            return ResponseEntity.ok(Map.of("message", "Receipt deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete receipt"));
        }
    }

    /**
     * Get current month spending by category
     */
    @GetMapping("/summary/current-month")
    public ResponseEntity<?> getCurrentMonthSpending() {
        try {
            Long userId = getAuthenticatedUserId();
            List<SpendingSummaryDto> summary = receiptService.getCurrentMonthSpendingByCategory(userId);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve spending summary"));
        }
    }

    /**
     * Get spending by category for a specific period
     */
    @GetMapping("/summary/period")
    public ResponseEntity<?> getSpendingByPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            Long userId = getAuthenticatedUserId();
            List<SpendingSummaryDto> summary = receiptService.getSpendingByCategoryForPeriod(userId, startDate, endDate);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve spending summary"));
        }
    }

    /**
     * Get monthly spending summary
     */
    @GetMapping("/summary/monthly")
    public ResponseEntity<?> getMonthlySpending() {
        try {
            Long userId = getAuthenticatedUserId();
            List<MonthlySpendingDto> summary = receiptService.getMonthlySpendingSummary(userId);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve monthly spending summary"));
        }
    }


    // Get total spending for the user
    @GetMapping("/total")
    public ResponseEntity<?> getTotalSpending() {
        try {
            Long userId = getAuthenticatedUserId();
            BigDecimal total = receiptService.getTotalSpending(userId);
            return ResponseEntity.ok(Map.of("totalSpending", total));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve total spending"));
        }
    }

     // Get recent receipts
    @GetMapping("/recent")
    public ResponseEntity<?> getRecentReceipts() {
        try {
            Long userId = getAuthenticatedUserId();
            List<ReceiptResponseDto> receipts = receiptService.getRecentReceipts(userId);
            return ResponseEntity.ok(receipts);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve recent receipts"));
        }
    }

    public Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User is not authenticated");
        }

        User user = (User) authentication.getPrincipal();
        System.out.println("User ID: " + user.getId());
        return user.getId();
    }

    @GetMapping("/test-auth")
    public ResponseEntity<?> testAuth() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return ResponseEntity.ok("Authentication works! User: " + auth.getName());
        } else {
            return ResponseEntity.status(401).body("Not authenticated");
        }
    }
}

