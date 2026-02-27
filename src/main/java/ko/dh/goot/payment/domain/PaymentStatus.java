package ko.dh.goot.payment.domain;

public enum PaymentStatus {
    READY, PAID, FAILED, CANCELLED;

    // 포트원 결제 상태 문자열을 도메인 Enum으로 변환
    public static PaymentStatus from(String pgStatus) {
        if (pgStatus == null) return READY;
        
        return switch (pgStatus.toUpperCase()) {
            case "PAID" -> PAID;
            case "FAILED" -> FAILED;
            case "CANCELLED" -> CANCELLED;
            default -> READY;
        };
    }
}