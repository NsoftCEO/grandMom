package ko.dh.goot.dao;

import org.apache.ibatis.annotations.Mapper;

import ko.dh.goot.dto.Payment;

@Mapper
public interface PaymentMapper {
	int existsByPaymentId(String paymentId);
    void insertPayment(Payment payment);
    Payment selectByOrderId(Long orderId);
    void updatePaymentStatus(Payment payment);
	
}