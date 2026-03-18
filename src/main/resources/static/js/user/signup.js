async function signup() {
    const data = {
        name: document.getElementById("name").value,
        email: document.getElementById("email").value,
        password: document.getElementById("password").value,
        phone: document.getElementById("phone").value
    };

    const errorEl = document.getElementById("error");
    errorEl.innerText = "";

    try {
        const response = await fetch("/api/auth/signup", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(data)
        });

        if (!response.ok) {
            const res = await response.json();
            errorEl.innerText = res.message || "회원가입 실패";
            return;
        }

        // 성공 시 로그인 페이지로 이동
        alert("회원가입 성공");
        window.location.href = "/auth/login";

    } catch (e) {
        errorEl.innerText = "서버 오류";
    }
}