/**
 * 学员端登录页：注册 / 登录 / 访客进入 user.html
 */
(function () {
  const toastEl = document.getElementById("user-toast");
  let toastTimer = null;

  function showToast(msg) {
    if (!toastEl) return;
    toastEl.textContent = msg;
    toastEl.classList.remove("hidden");
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => toastEl.classList.add("hidden"), 3200);
  }

  function okResult(data) {
    return data && data.code === 1000;
  }

  function setStatusLine(text, isErr) {
    const st = document.getElementById("login-status-line");
    if (!st) return;
    st.textContent = text || "";
    st.className = isErr ? "status-line err" : "status-line muted";
  }

  function goWorkspace() {
    FriendApi.setGuestMode(false);
    location.href = "user-dashboard.html";
  }

  function applyLoginPayload(data) {
    if (!okResult(data) || data.data == null) return false;
    const d = data.data;
    if (typeof d === "string") {
      FriendApi.setSession({
        token: d,
        userId: null,
        nickName: "",
        email: "",
      });
      showToast("登录成功");
      goWorkspace();
      return true;
    }
    FriendApi.setSession({
      token: d.token,
      userId: d.userId != null ? String(d.userId) : null,
      nickName: d.nickName || "",
      email: d.email || "",
    });
    showToast("登录成功");
    goWorkspace();
    return true;
  }

  function syncApiInput() {
    const el = document.getElementById("friend-api-base");
    if (el) el.value = FriendApi.getApiBase();
  }

  document.getElementById("friend-btn-save-api")?.addEventListener("click", () => {
    const v = document.getElementById("friend-api-base")?.value?.trim();
    if (v) FriendApi.setApiBase(v);
    showToast("已保存网关: " + FriendApi.getApiBase());
  });

  document.querySelectorAll(".user-tab").forEach((btn) => {
    btn.addEventListener("click", () => {
      const name = btn.getAttribute("data-tab");
      document.querySelectorAll(".user-tab").forEach((b) => b.classList.remove("active"));
      btn.classList.add("active");
      document.querySelectorAll(".user-tab-panel").forEach((p) => p.classList.add("hidden"));
      const panel = document.getElementById("panel-" + name);
      if (panel) panel.classList.remove("hidden");
    });
  });

  document.getElementById("friend-btn-register")?.addEventListener("click", async () => {
    setStatusLine("");
    const body = {
      nickName: document.getElementById("reg-nick")?.value?.trim(),
      gender: Number(document.getElementById("reg-gender")?.value),
      phone: document.getElementById("reg-phone")?.value?.trim(),
      password: document.getElementById("reg-pwd")?.value,
      email: document.getElementById("reg-email")?.value?.trim(),
    };
    const { data } = await FriendApi.register(body);
    if (okResult(data)) {
      showToast("注册成功 userId=" + data.data);
      setStatusLine("注册成功，请切换到登录方式进入学员中心。");
    } else {
      setStatusLine(data?.msg || "注册失败", true);
      showToast(data?.msg || "注册失败");
    }
  });

  document.getElementById("friend-btn-login-phone")?.addEventListener("click", async () => {
    setStatusLine("登录中…");
    const body = {
      phone: document.getElementById("login-phone")?.value?.trim(),
      password: document.getElementById("login-phone-pwd")?.value,
    };
    const { data } = await FriendApi.loginByPhonePassword(body);
    if (applyLoginPayload(data)) return;
    setStatusLine(data?.msg || "登录失败", true);
    showToast(data?.msg || "登录失败");
  });

  document.getElementById("friend-btn-login-email")?.addEventListener("click", async () => {
    setStatusLine("登录中…");
    const body = {
      email: document.getElementById("login-email")?.value?.trim(),
      password: document.getElementById("login-email-pwd")?.value,
    };
    const { data } = await FriendApi.loginByEmailPassword(body);
    if (applyLoginPayload(data)) return;
    setStatusLine(data?.msg || "登录失败", true);
    showToast(data?.msg || "登录失败");
  });

  document.getElementById("friend-btn-send-code")?.addEventListener("click", async () => {
    const email = document.getElementById("code-email")?.value?.trim();
    const { data } = await FriendApi.sendEmailCode({ email });
    if (okResult(data)) showToast("验证码已发送（若邮箱存在）");
    else showToast(data?.msg || "发送失败");
  });

  document.getElementById("friend-btn-login-code")?.addEventListener("click", async () => {
    setStatusLine("登录中…");
    const body = {
      email: document.getElementById("code-email")?.value?.trim(),
      code: document.getElementById("code-value")?.value?.trim(),
    };
    const { data } = await FriendApi.loginByEmailCode(body);
    if (applyLoginPayload(data)) return;
    setStatusLine(data?.msg || "登录失败", true);
    showToast(data?.msg || "登录失败");
  });

  document.getElementById("btn-guest-enter")?.addEventListener("click", () => {
    FriendApi.clearSession();
    FriendApi.setGuestMode(true);
    location.href = "user.html";
  });

  document.getElementById("link-back-user")?.addEventListener("click", (e) => {
    const s = FriendApi.getSession();
    if (s && s.token) {
      FriendApi.setGuestMode(false);
      return;
    }
    if (FriendApi.isGuestMode()) return;
    e.preventDefault();
    showToast("请先登录或使用访客浏览");
  });

  syncApiInput();
})();
