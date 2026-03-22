package ko.dh.goot.product.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ko.dh.goot.product.dto.ProductList;
import ko.dh.goot.product.service.ProductService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/product")
public class ProductApiController {

    private final ProductService productService;

    // Next.js가 호출하는 API
    @GetMapping("/products")
    public List<ProductList> productList(@RequestParam(value = "category", defaultValue = "1") String category) {
        Map<String, Object> param = new HashMap<>();
        param.put("category", category);

        return productService.selectProductList(param);
    }
}