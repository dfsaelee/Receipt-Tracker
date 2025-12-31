package PersonalCPI.PersonalCPI.controller;

import PersonalCPI.PersonalCPI.service.OfficialCPIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin controller for managing official CPI data
 */
@RestController
@RequestMapping("/api/admin/cpi")
public class AdminCPIController {
    
    private final OfficialCPIService officialCPIService;
    
    @Autowired
    public AdminCPIController(OfficialCPIService officialCPIService) {
        this.officialCPIService = officialCPIService;
    }
    
    /**
     * Trigger BLS API data fetch for all categories
     * POST /api/admin/cpi/fetch
     */
    @PostMapping("/fetch")
    public ResponseEntity<?> fetchBlsData() {
        try {
            officialCPIService.fetchAndStoreLatestData();
            return ResponseEntity.ok(Map.of(
                "message", "BLS CPI data fetched and stored successfully",
                "note", "Data for overall CPI + 8 categories has been imported for the last 24 months"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to fetch BLS data: " + e.getMessage()
            ));
        }
    }
}
