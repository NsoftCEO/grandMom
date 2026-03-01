package ko.dh.goot.product.persistence;

import java.time.LocalDateTime;

public record ProductOptionRecord(
    Long optionId,
    Long productId,
    String color,
    String size,
    int stockQuantity,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}