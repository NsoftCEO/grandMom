package ko.dh.goot.payment.dao;

import org.apache.ibatis.annotations.Mapper;

import ko.dh.goot.payment.domain.Payment;
import ko.dh.goot.payment.dto.PortOnePaymentResponse;

@Mapper
public interface PaymentMapper {
	int existsByPaymentId(String paymentId);
    void insertPayment(PortOnePaymentResponse pgPayment);
    Payment selectByOrderId(Long orderId);
    void updatePaymentStatus(Payment payment);
	
}