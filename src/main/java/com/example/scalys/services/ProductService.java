package com.example.scalys.services;

import com.example.scalys.dtos.ProductRequest;
import com.example.scalys.dtos.ProductResponse;
import com.example.scalys.dtos.ProductStockRequest;
import com.example.scalys.dtos.ProductUpdateRequest;
import com.example.scalys.models.Product;
import com.example.scalys.models.ProductStatus;
import com.example.scalys.models.ProductKind;
import com.example.scalys.models.OrderStatus;
import com.example.scalys.models.SpeciesGroup;
import com.example.scalys.repositories.ProductRepository;
import com.example.scalys.repositories.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public ProductService(ProductRepository productRepository, OrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    public List<ProductResponse> getProducts() {
        return productRepository.findByStatusInOrderByIdAsc(List.of(ProductStatus.AVAILABLE))
                .stream()
                .filter(this::isPubliclyAvailable)
                .map(ProductResponse::from)
                .toList();
    }

    public ProductResponse getProduct(Long id) {
        Product product = findProduct(id);
        if (!isPubliclyAvailable(product)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        }
        return ProductResponse.from(product);
    }

    private boolean isPubliclyAvailable(Product product) {
        return product.getStatus() == ProductStatus.AVAILABLE
                && (product.getKind() == ProductKind.SERVICE
                    || product.getStockQuantity() != null && product.getStockQuantity() > 0);
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAllByOrderByIdAsc().stream()
                .map(ProductResponse::from)
                .toList();
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        validateIndividualCode(request.individualCode(), null);
        List<String> images = imageUrls(request);
        Product product = new Product(request.name().trim(), blankToNull(request.species()), request.price(),
                blankToNull(request.description()), images.isEmpty() ? null : images.getFirst(), request.status());
        product.updateCatalog(request.kind() == null ? ProductKind.PET : request.kind(), request.category(), request.stockQuantity());
        updateDetails(product, request, images);
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductUpdateRequest update) {
        Product product = findProductForUpdate(id);
        verifySnapshot(product, update.expectedKind(), update.expectedStatus(), update.expectedStockQuantity());
        ProductRequest request = update.product();
        ProductKind requestedKind = request.kind() == null ? ProductKind.PET : request.kind();
        boolean changingKind = product.getKind() != requestedKind;
        if (changingKind && orderRepository.existsByProductId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể đổi loại sản phẩm đã có đơn hàng.");
        }
        guardActivePetReservation(product, request.status());
        validateIndividualCode(request.individualCode(), id);
        updateFields(product, request, changingKind);
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse updateStock(Long id, ProductStockRequest request) {
        Product product = findProductForUpdate(id);
        verifySnapshot(product, request.expectedKind(), request.expectedStatus(), request.expectedStockQuantity());
        if (product.getKind() == ProductKind.SERVICE)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dịch vụ không có tồn kho.");
        product.setStockQuantity(request.stockQuantity());
        if (product.getKind() == ProductKind.PET && product.getStatus() == ProductStatus.HELD
                && request.stockQuantity() > 0) product.setStatus(ProductStatus.AVAILABLE);
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse updateStatus(Long id, ProductStatus status) {
        Product product = findProductForUpdate(id);
        guardActivePetReservation(product, status);
        if (product.getKind() != ProductKind.PET && status == ProductStatus.HELD) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ pet một cá thể mới có trạng thái đang giữ.");
        }
        product.setStatus(status);
        return ProductResponse.from(product);
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    private Product findProductForUpdate(Long id) {
        return productRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    private void verifySnapshot(Product product, ProductKind kind, ProductStatus status, Integer stock) {
        if (product.getKind() != kind || product.getStatus() != status
                || !Objects.equals(product.getStockQuantity(), stock))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Sản phẩm hoặc tồn kho đã thay đổi. Hãy tải lại dữ liệu trước khi lưu.");
    }

    private void guardActivePetReservation(Product product, ProductStatus targetStatus) {
        if (product.getKind() == ProductKind.PET && product.getStatus() == ProductStatus.HELD
                && targetStatus != ProductStatus.HELD
                && orderRepository.existsByProductIdAndStatus(product.getId(), OrderStatus.NEW)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Pet đang giữ cho đơn mới. Hãy xử lý đơn hàng trước khi đổi trạng thái.");
        }
    }

    private void updateFields(Product product, ProductRequest request, boolean changingKind) {
        List<String> images = imageUrls(request);
        product.update(request.name().trim(), blankToNull(request.species()), request.price(),
                blankToNull(request.description()), images.isEmpty() ? null : images.getFirst(),
                changingKind ? request.status() : product.getStatus());
        product.updateCatalog(request.kind() == null ? ProductKind.PET : request.kind(), request.category(),
                changingKind ? request.stockQuantity() : product.getStockQuantity());
        updateDetails(product, request, images);
    }

    private void updateDetails(Product product, ProductRequest request, List<String> images) {
        boolean pet = product.getKind() == ProductKind.PET;
        product.updateDetails(pet ? blankToNull(request.scientificName()) : null,
                request.speciesGroup() == null ? SpeciesGroup.OTHER : request.speciesGroup(),
                pet ? request.gender() : null, pet ? request.birthDate() : null,
                pet ? blankToNull(request.approximateAge()) : null,
                pet ? blankToNull(request.currentSize()) : null, pet ? blankToNull(request.adultSize()) : null,
                pet ? request.origin() : null, pet ? request.careDifficulty() : null,
                pet ? blankToNull(request.individualCode()) : null,
                blankToNull(request.videoUrl()), images);
    }

    private List<String> imageUrls(ProductRequest request) {
        if (request.imageUrls() != null) {
            return request.imageUrls().stream().map(String::trim).toList();
        }
        String oldImage = blankToNull(request.imageUrl());
        return oldImage == null ? List.of() : List.of(oldImage);
    }

    private void validateIndividualCode(String value, Long id) {
        String code = blankToNull(value);
        if (code != null && (id == null ? productRepository.existsByIndividualCode(code)
                : productRepository.existsByIndividualCodeAndIdNot(code, id))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã cá thể đã tồn tại");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
