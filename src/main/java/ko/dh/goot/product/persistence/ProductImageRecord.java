package ko.dh.goot.product.persistence;

import java.time.LocalDateTime;

public record ProductImageRecord(
	    Long imageId,
	    Long productId,
	    String fileName,
	    String imageType,
	    int sortOrder,
	    String useYn,
	    LocalDateTime createdAt
	) {}
