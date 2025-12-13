
const PORTONE_STORE_ID = document.body.dataset.storeId;
const KAKAO_CHANNEL_KEY = document.body.dataset.kakaoKey;

// 💡 2. 전역 상태 변수
let serverOrderId = null;
let expectedAmount = null;

// 💡 3. PortOne SDK에 필요한 상수 (실제 PortOne 라이브러리에서 정의되어야 합니다.)
const CURRENCY_KRW = "KRW";
const EASY_PAY = "EASY_PAY"; // 또는 'card', 'vbank' 등

document.addEventListener('DOMContentLoaded', () => {
    const payBtn = document.getElementById('payBtn');
    if (payBtn) {
        payBtn.addEventListener('click', handlePayment);
    }
});


/**
 * 사용자에게 메시지를 표시하고, 버튼 상태를 복구합니다.
 * HTML에 <div id="paymentMessageBox"></div> 가 있다고 가정합니다.
 * @param {string} message 표시할 메시지
 * @param {boolean} isError 에러 여부
 */
function displayPaymentMessage(message, isError = false) {
    const messageBox = document.getElementById('paymentMessageBox');
    const payBtn = document.getElementById('payBtn');
    
    // 🚨 alert 대체 로직: 메시지 박스 또는 버튼 텍스트 업데이트
    if (messageBox) {
        messageBox.innerText = message;
        messageBox.style.color = isError ? 'red' : 'green';
        messageBox.style.display = 'block';
    } else {
        console.log(`[Message ${isError ? 'ERROR' : 'SUCCESS'}] ${message}`);
        if (payBtn) {
            // 버튼 텍스트에 오류 요약 표시 (최종 finally에서 원본 복구됨)
            payBtn.innerText = isError ? `오류 ${message.substring(0, 15)}...` : message; 
        }
    }
}

async function handlePayment() {
    const orderForm = document.getElementById('orderForm');
    const payBtn = document.getElementById('payBtn');
    
    // payBtn이 없으면 함수 실행 중단
    if (!payBtn) return; 

    const originalBtnText = payBtn.innerText;
    
    // 메시지 박스 초기화
    const messageBox = document.getElementById('paymentMessageBox');
    if (messageBox) { messageBox.style.display = 'none'; }

    if (!orderForm || !orderForm.checkValidity()) {
        displayPaymentMessage("배송 정보를 입력해주세요.", true); // 🚨 alert 대체
        return;
    }

    payBtn.disabled = true;
    payBtn.innerText = '결제 요청 중...';

    try {
        // 1단계: 주문 정보 서버에 전송 및 orderId, 금액 확정
        const prepareResponse = await prepareOrder();
        serverOrderId = prepareResponse.orderId;
        expectedAmount = prepareResponse.expectedAmount;

        payBtn.innerText = '결제 창 호출 중...';

        // 2단계: PortOne 결제 요청
        const portoneResponse = await requestPortOnePayment(serverOrderId, expectedAmount);
        
        console.log("PortOne 응답:", portoneResponse);
        
        // PortOne 결제 실패 처리 (사용자가 취소하거나 오류 발생)
        if (portoneResponse.code !== undefined) {
            // code가 있으면 실패
            displayPaymentMessage("결제가 취소되었거나 실패했습니다. 메시지: " + portoneResponse.message, true); // 🚨 alert 대체
            // ⚠️ TODO: 서버에 주문 상태 정리 (PENDING 주문을 FAILED로) 요청 추가 가능
            return;
        }

        // -----------------------------------------------------------------------------
        // 💡 웹훅 전환: 3단계 최종 검증 요청을 클라이언트에서 제거합니다.
        // 이 검증 단계는 이제 PG사에서 서버로 직접 호출하는 Webhook이 담당합니다.
        // -----------------------------------------------------------------------------
        // await verifyPayment(portoneResponse.paymentId, serverOrderId); // 👈 이 호출을 제거했습니다.
        
        // ✅ 최종 성공: 주문 완료 페이지로 이동 (서버에서 웹훅 처리 중임을 알림)
        displayPaymentMessage("결제 승인 완료. 주문 정보를 서버에서 최종 처리 중입니다.", false); // 🚨 메시지 변경
        // 💡 실무: 여기서 주문 완료/대기 페이지로 이동하여 웹훅 처리가 완료되기를 기다립니다.
        window.location.href = `ordercomplete?orderId=${serverOrderId}`; 

    } catch (error) {
        console.error("결제 처리 중 최종 오류", error);
        displayPaymentMessage(error.message || "결제 처리 중 알 수 없는 오류가 발생했습니다. 고객센터에 문의해주세요.", true); // 🚨 alert 대체
    } finally {
        // 💡 UX안정성 보완 2: 최종적으로 버튼 상태 복구
        payBtn.disabled = false;
        payBtn.innerText = originalBtnText;
    }
}


/**
 * 1단계: 주문 데이터를 서버에 전송하고 orderId와 확정 금액을 받아옴.
 */
async function prepareOrder() {
    const formData = new FormData(document.getElementById('orderForm'));
    const orderData = Object.fromEntries(formData.entries());

    // HTML 요소에서 주문/상품 정보 가져오기
    orderData.orderName = document.getElementById('productName').dataset.productname;
    // 클라이언트 금액은 참고용으로만 보냄 (서버에서 반드시 재계산해야 함)
    const priceElement = document.getElementById('price');
    if (priceElement && priceElement.dataset.price) {
        // 쉼표(,) 제거 후 숫자로 변환
        orderData.clientTotalAmount = parseInt(priceElement.dataset.price.replace(/,/g, '')); 
    } else {
        throw new Error("상품 가격 정보를 찾을 수 없습니다.");
    }

    const prepareOrderResponse = await fetch("/order/prepareOrder", {
        method: 'POST',
        headers: { 
            'Content-Type': 'application/json' 
        },
        body: JSON.stringify(orderData),
    });

    if (!prepareOrderResponse.ok) {
        const error = await prepareOrderResponse.json();
        // 💡 실무 보완: 서버에서 전달한 구체적인 에러 메시지 사용
        throw new Error("주문 생성 실패: " + (error.message || prepareOrderResponse.statusText));
    }
    
    return prepareOrderResponse.json();
}


/**
 * 2단계: PortOne SDK를 호출하여 결제 창을 띄웁니다.
 */
async function requestPortOnePayment(orderId, totalAmount) {
    const paymentId = `payment-${crypto.randomUUID()}`;

    const response = await PortOne.requestPayment({
        storeId: PORTONE_STORE_ID,
        channelKey: KAKAO_CHANNEL_KEY,
        paymentId: paymentId,
        orderName: document.getElementById('productName').dataset.productname,
        totalAmount: totalAmount, // ✅ 서버 확정 금액 사용
        currency: CURRENCY_KRW,
        payMethod: EASY_PAY,
        isTestChannel: true,
        redirectUrl: "http://localhost:8080/payment/redirect", 
        customData: {
            orderId: orderId 
        }
    });
    
    console.log("PortOne requestPayment 응답:", response);
    return response;
}
