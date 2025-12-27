package PersonalCPI.PersonalCPI.service;

import PersonalCPI.PersonalCPI.config.S3Buckets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Service
public class S3Service {
    private final S3Client s3Client;
    private final S3Buckets s3Buckets;
    private final S3Presigner presigner;

    @Autowired
    public S3Service(S3Client s3Client, S3Buckets s3Buckets, S3Presigner presigner) {
        this.s3Client = s3Client;
        this.s3Buckets = s3Buckets;
        this.presigner = presigner;
    }

    public void putObject(String key, MultipartFile file) throws Exception{
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(s3Buckets.getName())
                .key(key) // key includes the folder from controller
                .contentType(file.getContentType())
                .build();
        s3Client.putObject(objectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
    }

    // return presigned urls, aka our get object
    public String createPresignedGetUrl(String key) {
        try {
            GetObjectRequest objectRequest = GetObjectRequest.builder()
                    .bucket(s3Buckets.getName())
                    .key(key)
                    .build();
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(java.time.Duration.ofMinutes(10))
                    .getObjectRequest(objectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toExternalForm();

        } catch (Exception e) {
            throw new RuntimeException("Failed to get object");
        }
    }

    // Delete object from S3
    public void deleteObject(String key) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(s3Buckets.getName())
                    .key(key)
                    .build();
            s3Client.deleteObject(deleteRequest);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete object from S3: " + e.getMessage());
        }
    }
}



