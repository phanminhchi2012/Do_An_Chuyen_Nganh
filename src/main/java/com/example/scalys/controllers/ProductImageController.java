package com.example.scalys.controllers;

import com.example.scalys.services.ProductImageService;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ProductImageController {
    private final ProductImageService images;

    public ProductImageController(ProductImageService images) {
        this.images = images;
    }

    @PostMapping(path = "/api/admin/product-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) {
        return Map.of("url", images.store(file));
    }

    @GetMapping("/api/product-images/{filename}")
    public ResponseEntity<Resource> get(@PathVariable String filename) throws IOException {
        var file = images.open(filename);
        MediaType type = filename.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok()
                .contentType(type)
                .contentLength(file.length())
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .body(file.resource());
    }
}
