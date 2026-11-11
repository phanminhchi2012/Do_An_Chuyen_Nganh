package com.example.scalys.services;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class ObjectStorageService {
    private final S3Client client;
    private final String bucket;

    public ObjectStorageService(@Value("${file-storage.backend}") String backend,
                                @Value("${file-storage.bucket}") String bucket,
                                @Value("${file-storage.endpoint}") String endpoint,
                                @Value("${file-storage.region}") String region,
                                @Value("${file-storage.access-key}") String accessKey,
                                @Value("${file-storage.secret-key}") String secretKey) {
        this.bucket = bucket;
        if ("filesystem".equals(backend)) {
            this.client = null;
        } else if ("neon".equals(backend) && !bucket.isBlank() && !endpoint.isBlank()
                && !region.isBlank() && !accessKey.isBlank() && !secretKey.isBlank()) {
            this.client = S3Client.builder()
                    .endpointOverride(URI.create(endpoint))
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                    .forcePathStyle(true)
                    .build();
        } else {
            throw new IllegalStateException("Invalid file storage configuration");
        }
    }

    public boolean enabled() { return client != null; }

    public void put(String key, MultipartFile file, String contentType) throws IOException {
        try (var source = file.getInputStream()) {
            client.putObject(PutObjectRequest.builder().bucket(bucket).key(key)
                    .contentType(contentType).build(), RequestBody.fromInputStream(source, file.getSize()));
        }
    }

    public StoredObject get(String key) {
        try {
            var response = client.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
            return new StoredObject(new InputStreamResource(response) {
                @Override public long contentLength() { return response.response().contentLength(); }
            }, response.response().contentLength());
        } catch (NoSuchKeyException missing) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }
    }

    public void delete(String key) {
        client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }

    @PreDestroy
    public void close() {
        if (client != null) client.close();
    }

    public record StoredObject(Resource resource, long length) { }
}
