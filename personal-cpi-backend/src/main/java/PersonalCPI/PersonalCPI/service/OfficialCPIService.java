package PersonalCPI.PersonalCPI.service;

import PersonalCPI.PersonalCPI.model.Category;
import PersonalCPI.PersonalCPI.model.OfficialCpiData;
import PersonalCPI.PersonalCPI.repository.CategoryRepository;
import PersonalCPI.PersonalCPI.repository.OfficialCpiDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Service for fetching and managing official CPI data from Bureau of Labor Statistics.
 * Supports both v1 (no key) and v2 (with key) BLS APIs.
 */
@Service
public class OfficialCPIService {
    
    private final OfficialCpiDataRepository officialCpiDataRepository;
    private final CategoryRepository categoryRepository;
    private final RestTemplate restTemplate;
    
    @Value("${bls.api.key:}")
    private String blsApiKey;
    
    @Value("${bls.api.version:v1}")
    private String blsApiVersion;
    
    @Autowired
    public OfficialCPIService(
            OfficialCpiDataRepository officialCpiDataRepository,
            CategoryRepository categoryRepository,
            RestTemplate restTemplate) {
        this.officialCpiDataRepository = officialCpiDataRepository;
        this.categoryRepository = categoryRepository;
        this.restTemplate = restTemplate;
    }
    
    /**
     * Get official CPI data for a specific month
     * Returns cached data from database if available
     */
    @Transactional(readOnly = true)
    public Optional<OfficialCpiData> getOfficialCPI(Integer year, Integer month, Long categoryId) {
        if (categoryId == null) {
            return officialCpiDataRepository.findByYearAndMonthAndCategoryIdIsNull(year, month);
        } else {
            return officialCpiDataRepository.findByYearAndMonthAndCategoryId(year, month, categoryId);
        }
    }
    
    /**
     * Get overall official CPI for a specific month
     */
    @Transactional(readOnly = true)
    public Optional<OfficialCpiData> getOverallOfficialCPI(Integer year, Integer month) {
        return officialCpiDataRepository.findByYearAndMonthAndCategoryIdIsNull(year, month);
    }
    
    /**
     * Get all official CPI data for a specific month (all categories + overall)
     */
    @Transactional(readOnly = true)
    public List<OfficialCpiData> getAllCPIForMonth(Integer year, Integer month) {
        return officialCpiDataRepository.findByYearAndMonth(year, month);
    }
    
    /**
     * Fetch and store latest CPI data from BLS API
     * NOTE: This is a placeholder - actual BLS API integration requires more complex logic
     * For now, this method is designed to be called manually or via scheduled job
     */
    @Transactional
    public void fetchAndStoreLatestData() {
        // TODO: Implement BLS API integration
        // This would involve:
        // 1. Determine which API version to use based on blsApiKey
        // 2. Call BLS API with appropriate series IDs
        // 3. Parse JSON response
        // 4. Calculate MoM and YoY changes
        // 5. Save to database
        
        throw new UnsupportedOperationException(
                "BLS API integration not yet implemented. " +
                "Please manually import CPI data via SQL script or use the manual import endpoint.");
    }
    
    /**
     * Manually save official CPI data
     * Useful for importing historical data or when API is unavailable
     */
    @Transactional
    public OfficialCpiData saveOfficialCpiData(OfficialCpiData data) {
        return officialCpiDataRepository.save(data);
    }
    
    /**
     * Check if CPI data exists for a specific month
     */
    @Transactional(readOnly = true)
    public boolean hasDataForMonth(Integer year, Integer month) {
        return officialCpiDataRepository.existsByYearAndMonth(year, month);
    }
    
    /**
     * Get the most recent CPI data available
     */
    @Transactional(readOnly = true)
    public Optional<OfficialCpiData> getLatestData() {
        return officialCpiDataRepository.findTopByOrderByYearDescMonthDesc();
    }
}
