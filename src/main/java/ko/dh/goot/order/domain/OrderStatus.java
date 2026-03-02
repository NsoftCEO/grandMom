package ko.dh.goot.order.domain;

public enum OrderStatus {
    PAYMENT_READY,  // 결제 대기
    PAID,           // 결제 완료
    CANCELLED,      // 취소
    PAYMENT_FAILED, // 결제 실패
    SHIPPED,        // 배송 시작
    DELIVERED,      // 배송 완료
    REFUNDED		// 환불
}
