package ko.dh.goot.payment.domain;

import lombok.extern.log4j.Log4j2;

@Log4j2
public enum RefundStatus {
    NONE,
    REQUESTED,
    PARTIAL,
    REFUNDED,
    FAILED,
    UNKNOWN;

	// RefundStatusTypeHandler에서 호출, 만약 enum값에 없는 값 또는 null이 db에서 조회된다면 UNKOWN으로 처리하고 로그남김 
	public static RefundStatus from(String value) {

        if (value == null) {
            return UNKNOWN;
        }

        try {
            return RefundStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            log.warn("DB RefundStatus 값 맵핑 실패, DB 값: {}", value);
            return UNKNOWN;
        }
    }
}