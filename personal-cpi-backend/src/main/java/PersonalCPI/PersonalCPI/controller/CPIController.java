package PersonalCPI.PersonalCPI.controller;

import PersonalCPI.PersonalCPI.dto.CategoryCpiDto;
import PersonalCPI.PersonalCPI.dto.OfficialCpiDto;
import PersonalCPI.PersonalCPI.dto.PersonalCpiDto;
import PersonalCPI.PersonalCPI.model.Category;
import PersonalCPI.PersonalCPI.model.OfficialCpiData;
import PersonalCPI.PersonalCPI.model.PersonalCpiMonthly;
import PersonalCPI.PersonalCPI.model.User;
import PersonalCPI.PersonalCPI.repository.CategoryRepository;
import PersonalCPI.PersonalCPI.service.CPIComparisonService;
import PersonalCPI.PersonalCPI.service.OfficialCPIService;
import PersonalCPI.PersonalCPI.service.PersonalCPIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST Controller for Personal CPI endpoints
 */
@RestController
@RequestMapping("/api/cpi")
public class CPIController {
    
    private final PersonalCPIService personalCPIService;
    private final OfficialCPIService officialCPIService;
    private final CPIComparisonService cpiComparisonService;
    private final CategoryRepository categoryRepository;
    
    @Autowired
    public CPIController(
            PersonalCPIService personalCPIService,
            OfficialCPIService officialCPIService,
            CPIComparisonService cpiComparisonService,
            CategoryRepository categoryRepository) {
        this.personalCPIService = personalCPIService;
        this.officialCPIService = officialCPIService;
        this.cpiComparisonService = cpiComparisonService;
        this.categoryRepository = categoryRepository;
    }
    
    /**
     * Get authenticated user's personal CPI for a specific month
     * GET /api/cpi/personal?year=2024&month=12
     */
    @GetMapping("/personal")
    public ResponseEntity<?> getPersonalCPI(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        try {
            Long userId = getAuthenticatedUserId();
            
            // Default to current month if not specified
            YearMonth targetMonth = (year != null && month != null) 
                    ? YearMonth.of(year, month) 
                    : YearMonth.now();
            
            List<PersonalCpiMonthly> data = personalCPIService.getPersonalCPISummary(
                    userId, targetMonth.getYear(), targetMonth.getMonthValue());
            
            if (data.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "No personal CPI data found for " + targetMonth));
            }
            
            PersonalCpiDto dto = convertToPersonalCpiDto(data);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve personal CPI: " + e.getMessage()));
        }
    }
    
    /**
     * Get comparison between personal and official CPI
     * GET /api/cpi/comparison?year=2024&month=12
     */
    @GetMapping("/comparison")
    public ResponseEntity<?> getComparison(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        try {
            Long userId = getAuthenticatedUserId();
            
            // Default to current month if not specified
            YearMonth targetMonth = (year != null && month != null) 
                    ? YearMonth.of(year, month) 
                    : YearMonth.now();
            
            CPIComparisonService.ComparisonResult result = cpiComparisonService.compareToOfficialCPI(
                    userId, targetMonth.getYear(), targetMonth.getMonthValue());
            
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate comparison: " + e.getMessage()));
        }
    }
    
    /**
     * Get time series comparison data
     * GET /api/cpi/comparison/timeseries?startDate=2024-01-01&endDate=2024-12-31
     */
    @GetMapping("/comparison/timeseries")
    public ResponseEntity<?> getComparisonTimeSeries(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            Long userId = getAuthenticatedUserId();
            
            List<CPIComparisonService.ComparisonResult> results = 
                    cpiComparisonService.getComparisonTimeSeries(userId, startDate, endDate);
            
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate time series: " + e.getMessage()));
        }
    }
    
    /**
     * Trigger calculation of personal CPI
     * POST /api/cpi/calculate?year=2024&month=12
     */
    @PostMapping("/calculate")
    public ResponseEntity<?> calculatePersonalCPI(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        try {
            Long userId = getAuthenticatedUserId();
            
            if (year != null && month != null) {
                // Calculate for specific month (synchronous)
                personalCPIService.calculateMonthlyPersonalCPI(userId, year, month);
                return ResponseEntity.ok(Map.of(
                        "message", "Personal CPI calculated successfully for " + year + "-" + month));
            } else {
                // Recalculate all (asynchronous - runs in background)
                personalCPIService.recalculateAllForUserAsync(userId);
                return ResponseEntity.ok(Map.of(
                        "message", "CPI calculation started in background (limited to last 24 months). Check server logs for progress."));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to calculate CPI: " + e.getMessage()));
        }
    }
    
    /**
     * Get official CPI data
     * GET /api/cpi/official?year=2024&month=12&categoryId=1
     */
    @GetMapping("/official")
    public ResponseEntity<?> getOfficialCPI(
            @RequestParam Integer year,
            @RequestParam Integer month,
            @RequestParam(required = false) Long categoryId) {
        try {
            Optional<OfficialCpiData> data = officialCPIService.getOfficialCPI(year, month, categoryId);
            
            if (data.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "No official CPI data found for " + year + "-" + month));
            }
            
            OfficialCpiDto dto = new OfficialCpiDto(
                    data.get().getIndexValue(),
                    data.get().getMomChangePercent(),
                    data.get().getYoyChangePercent());
            
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve official CPI: " + e.getMessage()));
        }
    }
    
    /**
     * Helper method to get authenticated user ID
     */
    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User is not authenticated");
        }
        
        User user = (User) authentication.getPrincipal();
        return user.getId();
    }
    
    /**
     * Convert PersonalCpiMonthly list to DTO
     */
    private PersonalCpiDto convertToPersonalCpiDto(List<PersonalCpiMonthly> data) {
        // Find overall data (categoryId is null)
        PersonalCpiMonthly overall = data.stream()
                .filter(d -> d.getCategoryId() == null)
                .findFirst()
                .orElse(null);
        
        if (overall == null) {
            throw new IllegalStateException("No overall CPI data found");
        }
        
        // Get category data
        List<CategoryCpiDto> categories = new ArrayList<>();
        Map<Long, String> categoryNames = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getCategoryId, Category::getName));
        
        for (PersonalCpiMonthly categoryData : data) {
            if (categoryData.getCategoryId() != null) {
                String categoryName = categoryNames.get(categoryData.getCategoryId());
                
                // Calculate weight percent
                BigDecimal weightPercent = categoryData.getTotalSpending()
                        .divide(overall.getTotalSpending(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
                
                CategoryCpiDto categoryDto = new CategoryCpiDto(
                        categoryData.getCategoryId(),
                        categoryName,
                        categoryData.getTotalSpending(),
                        weightPercent,
                        categoryData.getMomChangePercent(),
                        categoryData.getYoyChangePercent());
                
                categories.add(categoryDto);
            }
        }
        
        return new PersonalCpiDto(
                overall.getTotalSpending(),
                overall.getMomChangePercent(),
                overall.getYoyChangePercent(),
                categories);
    }
}
