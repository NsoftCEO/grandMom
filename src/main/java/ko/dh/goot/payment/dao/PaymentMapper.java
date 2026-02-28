package ko.dh.goot.payment.dao;

import org.apache.ibatis.annotations.Mapper;

import ko.dh.goot.payment.domain.Payment;
import ko.dh.goot.payment.dto.PortOnePaymentResponse;
import ko.dh.goot.payment.persistence.PaymentRecord;

@Mapper
public interface PaymentMapper {
	int existsByPaymentId(String paymentId);
    void insertPayment(PortOnePaymentResponse pgPayment);
    PaymentRecord selectByOrderId(Long orderId);
    void updatePaymentStatus(PaymentRecord payment);
	
}