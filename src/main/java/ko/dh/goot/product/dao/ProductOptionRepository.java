package ko.dh.goot.product.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import ko.dh.goot.product.domain.ProductOption;

public interface ProductOptionRepository extends JpaRepository<ProductOption, Long>{

}
