package com.example.scalys.services;

import com.example.scalys.dtos.WarrantyClaimPage;
import com.example.scalys.dtos.WarrantyClaimResponse;
import com.example.scalys.models.WarrantyClaim;
import com.example.scalys.repositories.OrderRepository;
import com.example.scalys.repositories.WarrantyClaimRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WarrantyClaimService {
    private static final long MAX_VIDEO_BYTES = 100L * 1024 * 1024;
    private final WarrantyClaimRepository claims;
    private final OrderRepository orders;
    private final WarrantySubmissionLimiter limiter;
    private final Path storageDirectory;
    private final ObjectStorageService objectStorage;

    public WarrantyClaimService(WarrantyClaimRepository claims, OrderRepository orders,
                                WarrantySubmissionLimiter limiter,
                                @Value("${warranty.storage-dir}") String storageDirectory,
                                ObjectStorageService objectStorage) {
        this.claims = claims;
        this.orders = orders;
        this.limiter = limiter;
        this.storageDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();
        this.objectStorage = objectStorage;
    }

    @Transactional
    public Long submit(String orderCode, String name, String phone, String email, String description,
                       MultipartFile video, String remoteAddress) {
        String cleanCode = orderCode == null ? "" : orderCode.trim().toUpperCase(Locale.ROOT);
        String cleanName = name == null ? "" : name.trim();
        String cleanPhone = phone == null ? "" : phone.replaceAll("[\\s.-]", "");
        String cleanEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        String cleanDescription = description == null ? "" : description.trim();
        if (!cleanCode.matches("[A-Z2-9]{10}") || cleanName.isBlank() || cleanName.length() > 150
                || !cleanPhone.matches("(?:0|\\+84)[35789][0-9]{8}")
                || cleanEmail.length() > 254 || !cleanEmail.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
                || cleanDescription.length() < 10 || cleanDescription.length() > 2000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng kiểm tra thông tin yêu cầu bảo hành.");
        if (video == null || video.isEmpty() || video.getSize() > MAX_VIDEO_BYTES)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Video phải có dung lượng từ 1 byte đến 100 MB.");
        String type = video.getContentType();
        if (!"video/mp4".equals(type) && !"video/webm".equals(type) && !"video/quicktime".equals(type))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ nhận video MP4, WebM hoặc MOV.");
        limiter.checkAndRecord(remoteAddress);
        try (InputStream source = video.getInputStream()) {
            byte[] header = source.readNBytes(12);
            boolean mp4 = header.length >= 8 && header[4] == 'f' && header[5] == 't'
                    && header[6] == 'y' && header[7] == 'p';
            boolean webm = header.length >= 4 && (header[0] & 0xff) == 0x1a
                    && (header[1] & 0xff) == 0x45 && (header[2] & 0xff) == 0xdf
                    && (header[3] & 0xff) == 0xa3;
            if (!(webm && "video/webm".equals(type)) && !(mp4 && !"video/webm".equals(type)))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tệp không phải video MP4, WebM hoặc MOV hợp lệ.");
        } catch (IOException failure) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không đọc được video.");
        }
        var order = orders.findByPublicCodeAndPhone(cleanCode, cleanPhone).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng với mã và số điện thoại đã nhập."));
        String fileName = UUID.randomUUID() + ("video/webm".equals(type) ? ".webm" : "video/quicktime".equals(type) ? ".mov" : ".mp4");
        Path destination = storageDirectory.resolve(fileName);
        boolean remote = objectStorage.enabled();
        try {
            if (remote) {
                objectStorage.put("warranty-videos/" + fileName, video, type);
            } else {
                Files.createDirectories(storageDirectory);
                try (InputStream source = video.getInputStream()) {
                    Files.copy(source, destination);
                }
            }
        } catch (IOException | RuntimeException failure) {
            if (!remote) {
                try { Files.deleteIfExists(destination); } catch (IOException ignored) { }
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không lưu được video. Vui lòng thử lại.");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    try {
                        if (remote) objectStorage.delete("warranty-videos/" + fileName);
                        else Files.deleteIfExists(destination);
                    } catch (IOException | RuntimeException ignored) { }
                }
            }
        });
        return claims.saveAndFlush(new WarrantyClaim(order.getId(), order.getPublicCode(),
                order.getProduct().getName(), cleanName, cleanPhone, cleanEmail, cleanDescription,
                fileName, type)).getId();
    }

    @Transactional(readOnly = true)
    public WarrantyClaimPage list(WarrantyClaim.Status status, int page) {
        if (page < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trang không hợp lệ.");
        Page<WarrantyClaim> found = status == null
                ? claims.findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(page, 10))
                : claims.findByStatusOrderByCreatedAtDescIdDesc(status, PageRequest.of(page, 10));
        return new WarrantyClaimPage(found.getContent().stream().map(WarrantyClaimResponse::from).toList(),
                found.getTotalElements(), found.getTotalPages(), page);
    }

    @Transactional(readOnly = true)
    public long newCount() { return claims.countByStatus(WarrantyClaim.Status.NEW); }

    @Transactional
    public WarrantyClaimResponse updateStatus(Long id, WarrantyClaim.Status status, String actor) {
        WarrantyClaim claim = claims.findByIdForUpdate(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu bảo hành."));
        claim.setStatus(status, actor);
        return WarrantyClaimResponse.from(claim);
    }

    @Transactional(readOnly = true)
    public VideoFile video(Long id) {
        WarrantyClaim claim = claims.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu bảo hành."));
        String filename = claim.getVideoFile();
        if (!filename.matches("[a-f0-9-]{36}\\.(mp4|webm|mov)"))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy video.");
        if (objectStorage.enabled()) {
            var stored = objectStorage.get("warranty-videos/" + filename);
            return new VideoFile(stored.resource(), claim.getVideoContentType(), stored.length());
        }
        Path file = storageDirectory.resolve(filename).normalize();
        if (!file.startsWith(storageDirectory) || !Files.isRegularFile(file))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy video.");
        try {
            return new VideoFile(new FileSystemResource(file), claim.getVideoContentType(), Files.size(file));
        } catch (IOException failure) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không đọc được video.", failure);
        }
    }

    public record VideoFile(Resource resource, String contentType, long length) { }
}
