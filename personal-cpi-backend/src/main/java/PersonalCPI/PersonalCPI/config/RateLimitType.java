package PersonalCPI.PersonalCPI.config;

public enum RateLimitType {
    UPLOAD,    // For file uploads - 10 requests/minute
    API,       // For regular API calls - 100 requests/minute
    ADMIN      // For admin endpoints - 50 requests/minute
}
