package ko.dh.goot.product.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import ko.dh.goot.product.domain.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

}