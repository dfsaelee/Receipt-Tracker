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
     * Fetch and store latest CPI data from BLS API for all categories
     * Fetches last 24 months of data for overall + all 8 categories
     */
    @Transactional
    public void fetchAndStoreLatestData() {
        System.out.println("Starting BLS API data fetch...");
        
        // Determine API version based on key availability
        boolean useV2 = blsApiKey != null && !blsApiKey.isEmpty();
        String apiUrl = useV2 
            ? "https://api.bls.gov/publicAPI/v2/timeseries/data/"
            : "https://api.bls.gov/publicAPI/v1/timeseries/data/";
        
        System.out.println("Using BLS API " + (useV2 ? "v2 (with key)" : "v1 (no key)"));
        
        // Get all series IDs (overall + categories)
        List<String> seriesIds = new java.util.ArrayList<>();
        seriesIds.add("CUUR0000SA0");  // Overall CPI
        
        // Add category series IDs
        List<Category> categories = categoryRepository.findAll();
        for (Category category : categories) {
            if (category.getBlsSeriesId() != null && !category.getBlsSeriesId().isEmpty()) {
                seriesIds.add(category.getBlsSeriesId());
            }
        }
        
        // Fetch data for last 24 months
        int currentYear = java.time.Year.now().getValue();
        int startYear = currentYear - 2;
        
        System.out.println("Series IDs to fetch: " + seriesIds);
        System.out.println("Total series: " + seriesIds.size());
        
        try {
            // Build request body
            String requestBody = buildBlsApiRequest(seriesIds, startYear, currentYear, useV2);
            System.out.println("Request body: " + requestBody);
            
            // Set headers
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            
            org.springframework.http.HttpEntity<String> request = new org.springframework.http.HttpEntity<>(requestBody, headers);
            
            // Make API call
            System.out.println("Calling BLS API: " + apiUrl);
            org.springframework.http.ResponseEntity<String> response = restTemplate.postForEntity(
                apiUrl, request, String.class);
            
            System.out.println("Response status: " + response.getStatusCode());
            System.out.println("Response body: " + response.getBody());
            
            if (response.getStatusCode().is2xxSuccessful()) {
                parseAndStoreBlsResponse(response.getBody(), categories);
                System.out.println("BLS data fetch completed successfully");
            } else {
                throw new RuntimeException("BLS API returned error: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            System.err.println("Error fetching BLS data: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch BLS data: " + e.getMessage(), e);
        }
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
    
    /**
     * Build BLS API request body
     */
    private String buildBlsApiRequest(List<String> seriesIds, int startYear, int endYear, boolean includeKey) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"seriesid\":[");
        for (int i = 0; i < seriesIds.size(); i++) {
            json.append("\"").append(seriesIds.get(i)).append("\"");
            if (i < seriesIds.size() - 1) json.append(",");
        }
        json.append("],");
        json.append("\"startyear\":\"").append(startYear).append("\",");
        json.append("\"endyear\":\"").append(endYear).append("\"");
        
        if (includeKey && blsApiKey != null && !blsApiKey.isEmpty()) {
            json.append(",\"registrationkey\":\"").append(blsApiKey).append("\"");
        }
        
        json.append("}");
        return json.toString();
    }
    
    /**
     * Parse BLS API response and store data
     */
    private void parseAndStoreBlsResponse(String jsonResponse, List<Category> categories) {
        try {
            // Simple JSON parsing (you could use Jackson for more robust parsing)
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(jsonResponse);
            
            if (!root.get("status").asText().equals("REQUEST_SUCCEEDED")) {
                throw new RuntimeException("BLS API request failed: " + root.get("message").asText());
            }
            
            com.fasterxml.jackson.databind.JsonNode seriesArray = root.get("Results").get("series");
            
            // Process each series
            for (com.fasterxml.jackson.databind.JsonNode series : seriesArray) {
                String seriesId = series.get("seriesID").asText();
                com.fasterxml.jackson.databind.JsonNode dataArray = series.get("data");
                
                // Determine category ID from series ID
                Long categoryId = getCategoryIdFromSeriesId(seriesId, categories);
                
                // Parse and store each data point
                java.util.List<OfficialCpiData> dataPoints = new java.util.ArrayList<>();
                for (com.fasterxml.jackson.databind.JsonNode dataPoint : dataArray) {
                    int year = Integer.parseInt(dataPoint.get("year").asText());
                    String period = dataPoint.get("period").asText();
                    
                    // Skip annual averages
                    if (!period.startsWith("M")) continue;
                    
                    int month = Integer.parseInt(period.substring(1));
                    String valueStr = dataPoint.get("value").asText();
                    
                    // Skip invalid values (e.g., "-" for missing data)
                    if (valueStr.equals("-") || valueStr.isEmpty()) {
                        System.out.println("Skipping invalid value for " + seriesId + " " + year + "-" + month + ": " + valueStr);
                        continue;
                    }
                    
                    BigDecimal indexValue = new BigDecimal(valueStr);
                    
                    OfficialCpiData data = new OfficialCpiData();
                    data.setYear(year);
                    data.setMonth(month);
                    data.setCategoryId(categoryId);
                    data.setIndexValue(indexValue);
                    
                    dataPoints.add(data);
                }
                
                // Sort by year and month
                dataPoints.sort((a, b) -> {
                    int yearComp = a.getYear().compareTo(b.getYear());
                    return yearComp != 0 ? yearComp : a.getMonth().compareTo(b.getMonth());
                });
                
                // Calculate MoM and YoY
                for (int i = 0; i < dataPoints.size(); i++) {
                    OfficialCpiData current = dataPoints.get(i);
                    
                    // MoM
                    if (i > 0) {
                        OfficialCpiData previous = dataPoints.get(i - 1);
                        BigDecimal momChange = current.getIndexValue()
                            .subtract(previous.getIndexValue())
                            .divide(previous.getIndexValue(), 4, java.math.RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(2, java.math.RoundingMode.HALF_UP);
                        current.setMomChangePercent(momChange);
                    }
                    
                    // YoY
                    if (i >= 12) {
                        OfficialCpiData previousYear = dataPoints.get(i - 12);
                        BigDecimal yoyChange = current.getIndexValue()
                            .subtract(previousYear.getIndexValue())
                            .divide(previousYear.getIndexValue(), 4, java.math.RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(2, java.math.RoundingMode.HALF_UP);
                        current.setYoyChangePercent(yoyChange);
                    }
                    
                    // Save or update to database
                    Optional<OfficialCpiData> existing;
                    if (categoryId == null) {
                        // Use special method for NULL category (overall CPI)
                        existing = officialCpiDataRepository.findByYearAndMonthAndCategoryIdIsNull(
                            current.getYear(), current.getMonth());
                    } else {
                        existing = officialCpiDataRepository.findByYearAndMonthAndCategoryId(
                            current.getYear(), current.getMonth(), categoryId);
                    }
                    
                    if (existing.isPresent()) {
                        // Update existing record
                        OfficialCpiData existingData = existing.get();
                        existingData.setIndexValue(current.getIndexValue());
                        existingData.setMomChangePercent(current.getMomChangePercent());
                        existingData.setYoyChangePercent(current.getYoyChangePercent());
                        OfficialCpiData saved = officialCpiDataRepository.save(existingData);
                        System.out.println("Updated: " + seriesId + " " + saved.getYear() + "-" + saved.getMonth() + " = " + saved.getIndexValue());
                    } else {
                        // Insert new record
                        OfficialCpiData saved = officialCpiDataRepository.save(current);
                        System.out.println("Saved: " + seriesId + " " + saved.getYear() + "-" + saved.getMonth() + " = " + saved.getIndexValue());
                    }
                }
                
                System.out.println("Saved " + dataPoints.size() + " records for series " + seriesId);
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse BLS response: " + e.getMessage(), e);
        }
    }
    
    /**
     * Map BLS series ID to category ID
     */
    private Long getCategoryIdFromSeriesId(String seriesId, List<Category> categories) {
        if (seriesId.equals("CUUR0000SA0")) {
            return null;  // Overall CPI
        }
        
        for (Category category : categories) {
            if (seriesId.equals(category.getBlsSeriesId())) {
                return category.getCategoryId();
            }
        }
        
        return null;
    }
}
