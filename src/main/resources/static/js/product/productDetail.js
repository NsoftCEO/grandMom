$(function () {

    const $color = $("#colorSelect");
    const $size = $("#sizeSelect");
    const $optionId = $("#optionId");

    let selectedColor = null;
    let selectedSize = null;

    // ===============================
    // 색상 초기 세팅
    // ===============================
    const colors = [...new Set(optionList.map(o => o.color))];

    colors.forEach(color => {
        $color.append(`<option value="${color}">${color}</option>`);
    });

    // ===============================
    // 색상 선택
    // ===============================
    $color.on("change", function () {
        selectedColor = $(this).val();
        selectedSize = null;
        $optionId.val("");

        $size.prop("disabled", true);
        $size.empty().append(`<option value="">사이즈 선택</option>`);

        if (!selectedColor) return;

        const sizes = optionList
            .filter(o => o.color === selectedColor && o.stockQuantity > 0)
            .map(o => o.size);

        [...new Set(sizes)].forEach(size => {
            $size.append(`<option value="${size}">${size}</option>`);
        });

        $size.prop("disabled", false);
    });

    // ===============================
    // 사이즈 선택
    // ===============================
    $size.on("change", function () {
        selectedSize = $(this).val();

        const option = optionList.find(o =>
            o.color === selectedColor && o.size === selectedSize
        );

        if (!option) {
            $optionId.val("");
            return;
        }

        $optionId.val(option.optionId);
    });

    // ===============================
    // 이미지 변경 (기존 로직 유지)
    // ===============================
    const $mainImage = $("#mainProductImage");
    const $thumbs = $(".thumb-img");
    let currentIndex = 0;

    $thumbs.on("click", function () {
        currentIndex = $thumbs.index(this);
        changeImage($(this).attr("src"));
    });

    if ($thumbs.length > 1) {
        setInterval(() => {
            currentIndex = (currentIndex + 1) % $thumbs.length;
            changeImage($thumbs.eq(currentIndex).attr("src"));
        }, 6000);
    }

    function changeImage(src) {
        $mainImage.fadeOut(200, function () {
            $(this).attr("src", src).fadeIn(200);
        });
    }

    // ===============================
    // 구매
    // ===============================
    $("#buyBtn").on("click", function () {
        const optionId = $optionId.val();
        const qty = $("#quantity").val();

        if (!optionId) {
            alert("색상과 사이즈를 선택해주세요.");
            return;
        }
		
		// 수정해야됨
		const url = `/order/detail?optionId=${optionId}&quantity=${qty}`;
		//const url = `/order/detail?productId=${productId}&quantity=${qty}`;
		window.location.href = url;
    });
});
