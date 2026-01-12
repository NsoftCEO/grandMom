document.addEventListener("DOMContentLoaded", function () {
    const mainImage = document.getElementById("mainProductImage");
    const thumbnails = document.querySelectorAll(".thumbnail img");
	
	//todo 함수로 나누기 ex) 	initThumbnailGallery(); initBuyBtn();
	
   // if (thumbnails.length === 0) return; // 여기 때문에 이미지없으면 버튼이벤트 안먹힘

    let currentIndex = 0;

    // ✅ 3초마다 자동 이미지 변경
    setInterval(() => {
        currentIndex = (currentIndex + 1) % thumbnails.length;
        fadeImageTo(thumbnails[currentIndex].src);
    }, 6000);

    // ✅ 썸네일 클릭 시 수동 변경
    thumbnails.forEach((thumb, index) => {
        thumb.addEventListener("click", () => {
            currentIndex = index;
            fadeImageTo(thumb.src);
        });
    });

    // ✅ 부드럽게 전환 (fade 효과)
    function fadeImageTo(newSrc) {
        mainImage.style.opacity = 0;
        setTimeout(() => {
            mainImage.src = newSrc;
            mainImage.style.opacity = 1;
        }, 300);
    }
	
	const buyBtn = document.getElementById("buyBtn");

	  buyBtn.addEventListener("click", function() {
		console.log("구매버튼 클릭");
	    const productId = $("#productId").val();
	    const quantity = $("input[name='quantity']").val();
		console.log("url::");
		console.log(productId);
		console.log(quantity);
	    // 주문 페이지로 상품 정보 전달 (쿼리 파라미터 방식)
	    const url = `/order/detail?productId=${productId}&quantity=${quantity}`;
	    window.location.href = url;
	  });
	  
	  
	/*document.getElementById("payBtn").addEventListener("click", async function () {
	  const paymentId = `payment-${crypto.randomUUID()}`; // 유니크 결제 ID 생성

	  const response = await PortOne.requestPayment({
	    storeId: "store-3e1256db-ab78-402b-b63f-03cf01111360", // ⚙️ 포트원 콘솔의 Store ID
	    channelKey: "channel-key-f29c6dac-a226-4aab-9035-be90baa437e2", // ⚙️ 카카오페이 채널 키
	    paymentId: paymentId,
	    orderName: "테스트 상품",
	    totalAmount: 1000,
	    currency: "CURRENCY_KRW",
		isTestChannel : true,
	    payMethod: "EASY_PAY", // 카카오페이 = 간편결제
	    redirectUrl: "http://localhost:8080/payment/redirect", // 모바일용 redirect
	  });
	
	  console.log("response::");
	  console.log(response);
	  
	  if (response.code !== undefined) {
	    alert("결제 실패: " + response.message);
	    return;
	  }

	  // ✅ 결제 완료 시 서버에 검증 요청
	  await fetch("/payment/complete", {
	    method: "POST",
	    headers: { "Content-Type": "application/json" },
	    body: JSON.stringify({ paymentId: paymentId }),
	  });

	  alert("결제 요청 완료! 서버 검증 중입니다.");
	});*/
});