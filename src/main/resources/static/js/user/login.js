async function login() {
    const email = document.getElementById("email").value;
    const password = document.getElementById("password").value;

    const errorEl = document.getElementById("error");
    errorEl.innerText = "";

    try {
        const response = await fetch("/api/auth/login", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                email: email,
                password: password
            })
        });

        if (!response.ok) {
            const data = await response.json();
            errorEl.innerText = data.message || "로그인 실패";
            return;
        }

        // ✅ JWT 쓰면 여기서 저장
        // const data = await response.json();
        // localStorage.setItem("accessToken", data.accessToken);

        // 성공 시 이동
        window.location.href = "/";
    } catch (e) {
        errorEl.innerText = "서버 오류";
    }
}