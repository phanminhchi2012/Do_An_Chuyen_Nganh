package com.example.scalys.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductImageService {
    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final long MAX_PIXELS = 24_000_000;
    private final Path storageDirectory;
    private final ObjectStorageService objectStorage;

    @Autowired
    public ProductImageService(@Value("${product-image.storage-dir}") String storageDirectory,
                               ObjectStorageService objectStorage) {
        this.storageDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();
        this.objectStorage = objectStorage;
    }

    public ProductImageService(String storageDirectory) {
        this(storageDirectory, null);
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ảnh phải có dung lượng từ 1 byte đến 10 MB.");
        }
        String extension = imageExtension(file);
        String filename = UUID.randomUUID() + "." + extension;
        if (objectStorage != null && objectStorage.enabled()) {
            try {
                objectStorage.put("product-images/" + filename, file,
                        "png".equals(extension) ? "image/png" : "image/jpeg");
            } catch (IOException | RuntimeException cause) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không lưu được ảnh sản phẩm.", cause);
            }
            return "/api/product-images/" + filename;
        }
        Path destination = storageDirectory.resolve(filename);
        try {
            Files.createDirectories(storageDirectory);
            Files.copy(file.getInputStream(), destination);
        } catch (IOException cause) {
            try { Files.deleteIfExists(destination); } catch (IOException ignored) { }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không lưu được ảnh sản phẩm.", cause);
        }
        return "/api/product-images/" + filename;
    }

    public Path find(String filename) {
        validateFilename(filename);
        Path image = storageDirectory.resolve(filename);
        if (!Files.isRegularFile(image, LinkOption.NOFOLLOW_LINKS)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy ảnh.");
        }
        return image;
    }

    public ObjectStorageService.StoredObject open(String filename) throws IOException {
        validateFilename(filename);
        if (objectStorage != null && objectStorage.enabled())
            return objectStorage.get("product-images/" + filename);
        Path path = find(filename);
        return new ObjectStorageService.StoredObject(new FileSystemResource(path), Files.size(path));
    }

    private void validateFilename(String filename) {
        if (!filename.matches("[a-f0-9-]{36}\\.(png|jpg)"))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy ảnh.");
    }

    private String imageExtension(MultipartFile file) {
        try (ImageInputStream input = ImageIO.createImageInputStream(file.getInputStream())) {
            if (input == null) throw invalidImage();
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw invalidImage();
            ImageReader reader = readers.next();
            try {
                reader.setInput(input);
                String format = reader.getFormatName();
                if (!"PNG".equalsIgnoreCase(format) && !"JPEG".equalsIgnoreCase(format)) {
                    throw invalidImage();
                }
                long width = reader.getWidth(0);
                long height = reader.getHeight(0);
                if (width < 1 || height < 1 || width * height > MAX_PIXELS) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ảnh vượt quá 24 triệu pixel.");
                }
                if (reader.read(0) == null) throw invalidImage();
                return "PNG".equalsIgnoreCase(format) ? "png" : "jpg";
            } finally {
                reader.dispose();
            }
        } catch (IOException cause) {
            throw invalidImage();
        }
    }

    private ResponseStatusException invalidImage() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ nhận ảnh PNG hoặc JPEG hợp lệ.");
    }
}
