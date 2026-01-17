package ko.dh.goot.product.dto;

import java.util.Date;
import java.util.List;

public class Product {
    private Long productId;
	private String productName;
    private Integer price;
    private String category;
    private Integer stock;
    private String description;
    private Date createdAt;
    private Date updatedAt;
    
    private String mainImage;
    private List<ProductImage> images;
    
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

	public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Integer getPrice() { return price; }
    public void setPrice(Integer price) { this.price = price; }
    
    public String getCategory() { return category; }
	public void setCategory(String category) { this.category = category; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    
    public String getMainImage() {
		return mainImage;
	}
	public void setMainImage(String mainImage) {
		this.mainImage = mainImage;
	}
	
	public List<ProductImage> getImages() { return images; }
    public void setImages(List<ProductImage> images) { this.images = images; }
}