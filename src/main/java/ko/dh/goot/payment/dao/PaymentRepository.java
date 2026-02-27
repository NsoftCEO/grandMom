package ko.dh.goot.payment.dao;

import ko.dh.goot.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    /**
     * 포트원 결제 고유 번호(pgTxId / imp_uid)로 결제 내역 존재 여부 확인
     * (웹훅 중복 수신 시 멱등성 검증용)
     * * @param pgTxId 포트원 결제 고유 번호
     * @return 존재 여부
     */
    boolean existsByPgTxId(String pgTxId);

    /**
     * 포트원 결제 고유 번호(pgTxId / imp_uid)로 결제 내역 단건 조회
     * (결제 취소, 실패 등 상태 동기화 시 사용)
     * * @param pgTxId 포트원 결제 고유 번호
     * @return Payment 엔티티 (Optional)
     */
    Optional<Payment> findByPgTxId(String pgTxId);
}
