package ko.dh.goot.product.persistence;

import java.time.LocalDateTime;

public record ProductCategoryRecord(
	    Long categoryId,
	    String categoryName,
	    String useYn, // 'Y' or 'N'
	    LocalDateTime createdAt,
	    LocalDateTime updatedAt
	) {}