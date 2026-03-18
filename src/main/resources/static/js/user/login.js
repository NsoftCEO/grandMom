async function login() {
    const email = document.getElementById("email").value;
    const password = document.getElementById("password").value;

    const errorEl = document.getElementById("error");
    errorEl.innerText = "";

	alert(password);
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

		const data = await response.json();
		
		console.log("data::");
		console.log(data);
		
		
        if (!response.ok) {          
            errorEl.innerText = data.message || "로그인 실패";
            return;
        }
		
        localStorage.setItem("accessToken", data.accessToken);

        window.location.href = "/";
    } catch (e) {
        errorEl.innerText = "서버 오류";
    }
}