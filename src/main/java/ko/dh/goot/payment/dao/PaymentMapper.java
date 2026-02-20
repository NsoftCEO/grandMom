package ko.dh.goot.payment.dao;

import org.apache.ibatis.annotations.Mapper;

import ko.dh.goot.payment.dto.PortOnePaymentResponse;
import ko.dh.goot.payment.entity.Payment;

@Mapper
public interface PaymentMapper {
	int existsByPaymentId(String paymentId);
    void insertPayment(PortOnePaymentResponse pgPayment);
    Payment selectByOrderId(Long orderId);
    void updatePaymentStatus(Payment payment);
	
}