package ko.dh.goot.product.persistence;

import java.time.LocalDateTime;

import ko.dh.goot.product.domain.ProductStatus;

public record ProductRecord(
    Long productId,
    String productName,
    int price,
    int salePrice,
    String description,
    Long categoryId,
    ProductStatus productStatus,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}