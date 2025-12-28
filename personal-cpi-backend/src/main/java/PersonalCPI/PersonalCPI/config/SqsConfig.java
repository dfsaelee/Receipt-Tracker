package PersonalCPI.PersonalCPI.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * Configuration for AWS SQS client.
 */
@Configuration
@EnableScheduling  // Enable scheduled tasks for queue polling
public class SqsConfig {
    
    @Value("${aws.region:us-west-1}")
    private String awsRegion;
    
    @Bean
    public SqsClient sqsClient() {
        return SqsClient.builder()
            .region(Region.of(awsRegion))
            .build();
    }
}
