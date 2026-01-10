package ko.dh.goot.controller;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import ko.dh.goot.dto.ProductDetail;
import ko.dh.goot.dto.ProductListItem;
import ko.dh.goot.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
@Log4j2
@Controller
@RequiredArgsConstructor
@RequestMapping("product")
public class ProductController {
	
	private final ProductService productService;
	
	// 상품 목록
	@GetMapping("/productList")
	public String productList(@RequestParam Map<String, Object> param, Model model) {
		System.out.println("/productList 맵핑");
	    System.out.println("파라미터: " + param);
	    
	    List<ProductListItem> productList = productService.selectProductList(param);
	    model.addAttribute("productList", productList);
	    
	    System.out.println(productList);
	    System.out.println("productList::");
	    return "product/productList";
	}

	// 상품 상세
    @GetMapping("/detail/{productId}")
    public String selectProductDetail(@PathVariable("productId") int productId, Model model) {
    	System.out.println("productId::");
        System.out.println(productId);
        ProductDetail product = productService.selectProductDetail(productId);

        
        model.addAttribute("product", product);
        return "product/productDetail";
    }
}
