package PersonalCPI.PersonalCPI.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
@Component
public class S3Test implements CommandLineRunner {

    private final S3Service s3Service;

    public S3Test(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    @Override
    public void run(String... args) throws Exception {
//        Path path = Paths.get("C:\\Users\\turni\\Downloads\\1000000320.jpg");
//
//        MultipartFile file = new MockMultipartFile(
//                "file",
//                "1000000320.jpg",
//                "image/jpeg",
//                Files.readAllBytes(path)
//        );
//
//        s3Service.putObject("images/1000000320.jpg", file);
//        System.out.println("Upload via service successful");
    }
}
