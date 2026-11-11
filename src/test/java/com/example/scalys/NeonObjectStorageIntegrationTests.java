package com.example.scalys;

import com.example.scalys.services.ObjectStorageService;
import com.example.scalys.services.ProductImageService;
import com.example.scalys.controllers.ProductImageController;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@EnabledIfEnvironmentVariable(named = "FILE_STORAGE_BACKEND", matches = "neon")
class NeonObjectStorageIntegrationTests {
    @Test
    void uploadsAndReadsAnImageThroughThePrivateBucket() throws Exception {
        var storage = new ObjectStorageService("neon", "scalys-files",
                System.getenv("AWS_ENDPOINT_URL_S3"), System.getenv("AWS_REGION"),
                System.getenv("AWS_ACCESS_KEY_ID"), System.getenv("AWS_SECRET_ACCESS_KEY"));
        try {
            var bytes = new ByteArrayOutputStream();
            ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", bytes);
            var images = new ProductImageService(Path.of("local-product-images").toString(), storage);
            String url = images.store(new MockMultipartFile("file", "image.png", "image/png", bytes.toByteArray()));
            String filename = url.substring(url.lastIndexOf('/') + 1);
            try {
                var file = images.open(filename);
                assertEquals(bytes.size(), file.length());
                try (var input = file.resource().getInputStream()) {
                    assertArrayEquals(bytes.toByteArray(), input.readAllBytes());
                }
                MockMvcBuilders.standaloneSetup(new ProductImageController(images)).build()
                        .perform(get(url))
                        .andExpect(status().isOk())
                        .andExpect(content().bytes(bytes.toByteArray()));
                assertEquals(404, assertThrows(ResponseStatusException.class,
                        () -> images.open(UUID.randomUUID() + ".png")).getStatusCode().value());
            } finally {
                storage.delete("product-images/" + filename);
            }
        } finally {
            storage.close();
        }
    }
}
