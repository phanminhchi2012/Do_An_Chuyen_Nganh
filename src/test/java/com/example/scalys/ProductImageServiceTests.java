package com.example.scalys;

import com.example.scalys.services.ProductImageService;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductImageServiceTests {
    @TempDir Path directory;

    @Test
    void storesARealImageWithAnOpaqueNameAndCanFindIt() throws Exception {
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", bytes);
        var service = new ProductImageService(directory.toString());

        String url = service.store(new MockMultipartFile("file", "pet.png", "image/png", bytes.toByteArray()));

        assertTrue(url.matches("/api/product-images/[a-f0-9-]{36}\\.png"));
        assertArrayEquals(bytes.toByteArray(), Files.readAllBytes(service.find(url.substring(url.lastIndexOf('/') + 1))));
    }

    @Test
    void rejectsFakeImagesAndPathTraversal() {
        var service = new ProductImageService(directory.toString());
        var fake = new MockMultipartFile("file", "pet.png", "image/png", "not an image".getBytes());

        assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.store(fake)).getStatusCode().value());
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> service.find("../secret.png")).getStatusCode().value());
    }
}
