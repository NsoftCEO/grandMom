document.addEventListener("DOMContentLoaded", function () {

    const mainImage = document.getElementById("mainProductImage");

    initThumbnailGallery();
    initBuyBtn();

    // ===============================
    // 썸네일 갤러리 초기화
    // ===============================
    function initThumbnailGallery() {
        const thumbnails = document.querySelectorAll(".thumbnail img");
        let currentIndex = 0;

        if (thumbnails.length === 0) return;

        // 자동 이미지 변경 (6초)
        setInterval(() => {
            currentIndex = (currentIndex + 1) % thumbnails.length;
            fadeImageTo(thumbnails[currentIndex].src);
        }, 6000);

        // 수동 클릭
        thumbnails.forEach((thumb, index) => {
            thumb.addEventListener("click", () => {
                currentIndex = index;
                fadeImageTo(thumb.src);
            });
        });

        // 페이드 효과
        function fadeImageTo(newSrc) {
            mainImage.style.transition = "opacity 0.3s";
            mainImage.style.opacity = 0;
            setTimeout(() => {
                mainImage.src = newSrc;
                mainImage.style.opacity = 1;
            }, 300);
        }
    }

    // ===============================
    // 구매 버튼 초기화
    // ===============================
    function initBuyBtn() {
        const buyBtn = document.getElementById("buyBtn");
        if (!buyBtn) return;

        buyBtn.addEventListener("click", function() {
            const productIdInput = document.getElementById("productId");
            const quantityInput = document.querySelector("input[name='quantity']");

            if (!productIdInput || !quantityInput) return;

            const productId = productIdInput.value;
            const quantity = quantityInput.value;

            if (!productId || !quantity) {
                alert("상품 또는 수량 정보가 올바르지 않습니다.");
                return;
            }

            console.log("구매버튼 클릭:", productId, quantity);

            // 주문 페이지로 이동 (쿼리 파라미터)
            const url = `/order/detail?productId=${productId}&quantity=${quantity}`;
            window.location.href = url;
        });
    }

});
