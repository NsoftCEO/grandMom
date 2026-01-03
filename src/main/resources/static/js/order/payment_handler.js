const PORTONE_STORE_ID = document.body.dataset.storeId;
const KAKAO_CHANNEL_KEY = document.body.dataset.kakaoKey;

// 💡 전역 상태
let serverOrderId = null;

document.addEventListener('DOMContentLoaded', () => {
    const payBtn = document.getElementById('payBtn');
    if (payBtn) {
        payBtn.addEventListener('click', handlePayment);
    }
});

/**
 * 사용자 메시지 표시
 */
function displayPaymentMessage(message, isError = false) {
    const messageBox = document.getElementById('paymentMessageBox');
    if (!messageBox) return;

    messageBox.innerText = message;
    messageBox.style.color = isError ? 'red' : 'green';
    messageBox.style.display = 'block';
}

/**
 * 결제 전체 플로우
 */
async function handlePayment() {
    const orderForm = document.getElementById('orderForm');
    const payBtn = document.getElementById('payBtn');
    if (!payBtn) return;

    const originalBtnText = payBtn.innerText;

    if (!orderForm || !orderForm.checkValidity()) {
        displayPaymentMessage("배송 정보를 입력해주세요.", true);
        return;
    }

    payBtn.disabled = true;
    payBtn.innerText = '결제 요청 중...';

    try {
        /* ===============================
         * 1️⃣ 주문 생성 (금액 서버 확정)
         * =============================== */
        const prepareResponse = await prepareOrder();
        serverOrderId = prepareResponse.orderId;

        payBtn.innerText = '결제 창 호출 중...';

        /* ===============================
         * 2️⃣ 결제 파라미터 서버 요청
         * =============================== */
        const paymentParams = await requestPaymentParams(serverOrderId);

		console.log("paymentParams：：：：");
		console.log(paymentParams);
        /* ===============================
         * 3️⃣ PortOne 결제 UI 호출
         * =============================== */
        const portoneResponse = await PortOne.requestPayment(paymentParams);

        if (portoneResponse.code) {
            displayPaymentMessage(portoneResponse.message || "결제가 취소되었습니다.", true);
            return;
        }

        displayPaymentMessage("결제 승인 완료. 처리 중입니다.");
       // window.location.href = `ordercomplete?orderId=${serverOrderId}`;

    } catch (error) {
        console.error(error);
        displayPaymentMessage(
            error.message || "결제 처리 중 오류가 발생했습니다.",
            true
        );
    } finally {
        payBtn.disabled = false;
        payBtn.innerText = originalBtnText;
    }
}

/**
 * 1️⃣ 주문 생성
 */
async function prepareOrder() {

    /* ===============================
     * 기본 검증
     * =============================== */
    const $form = $('#orderForm');

    if (!$form.find('[name="receiver"]').val()) {
        alert('받는 분 이름을 입력하세요.');
        return;
    }

    /* ===============================
     * 주문 데이터 구성 (중요)
     * =============================== */
    const orderData = {
        productId: Number($form.find('[name="productId"]').val()),
        quantity: Number($form.find('[name="quantity"]').val()),
        receiver: $form.find('[name="receiver"]').val(),
        phone: $form.find('[name="phone"]').val(),
        address: $form.find('[name="address"]').val(),
        memo: $form.find('[name="memo"]').val(),
        orderName: $('#productName').data('productname'),
        clientTotalAmount: Number($('#price').data('price')),

        // ✅ 옵션 스냅샷 (JSON)
        optionInfo: {
            color: $('#optionColor').val(),
            size: $('#optionSize').val()
        }
    };

    try {
        const res = await fetch('/order/prepareOrder', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(orderData)
        });

        if (!res.ok) {
            const error = await res.json();
            throw new Error(error.message || '주문 생성 실패');
        }

        return await res.json(); // { orderId, totalAmount }     
          
    } catch (err) {
        console.error(err);
        alert(err.message);
        throw err;
    }
}

/**
 * 2️. 서버에서 결제 파라미터 생성 요청
 * 👉 totalAmount 포함 (서버 결정)
 */
async function requestPaymentParams(orderId) {
    const res = await fetch("/order/createPaymentParams", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ orderId })
    });

    if (!res.ok) {
        const error = await res.json();
        throw new Error(error.message || "결제 요청 생성 실패");
    }

    return await res.json();
}
