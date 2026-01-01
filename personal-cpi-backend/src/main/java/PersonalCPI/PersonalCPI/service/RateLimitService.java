package PersonalCPI.PersonalCPI.service;

import PersonalCPI.PersonalCPI.config.RateLimitType;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {
    
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    
    public boolean tryConsume(Long userId, RateLimitType type) {
        String key = userId + ":" + type.name();
        Bucket bucket = buckets.computeIfAbsent(key, k -> createBucket(type));
        return bucket.tryConsume(1);
    }
    
    public long getAvailableTokens(Long userId, RateLimitType type) {
        String key = userId + ":" + type.name();
        Bucket bucket = buckets.computeIfAbsent(key, k -> createBucket(type));
        return bucket.getAvailableTokens();
    }
    
    public long getSecondsUntilRefill(Long userId, RateLimitType type) {
        // Return the refill period in seconds
        return 60; // 1 minute for all types
    }
    
    public long getLimit(RateLimitType type) {
        return switch (type) {
            case UPLOAD -> 10;
            case API -> 100;
            case ADMIN -> 50;
        };
    }
    
    private Bucket createBucket(RateLimitType type) {
        Bandwidth limit = switch (type) {
            case UPLOAD -> Bandwidth.classic(
                    10, 
                    Refill.intervally(10, Duration.ofMinutes(1))
            );
            case API -> Bandwidth.classic(
                    100, 
                    Refill.intervally(100, Duration.ofMinutes(1))
            );
            case ADMIN -> Bandwidth.classic(
                    50, 
                    Refill.intervally(50, Duration.ofMinutes(1))
            );
        };
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
