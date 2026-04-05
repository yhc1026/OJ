/**
 * user-dashboard.js — 学员仪表盘（user-dashboard.html）
 * 登录状态同步、网关保存。
 */
(function () {
  var toastEl = document.getElementById("toast");
  var toastTimer = null;

  function showToast(msg) {
    if (!toastEl) return;
    toastEl.textContent = msg;
    toastEl.classList.remove("hidden");
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () { toastEl.classList.add("hidden"); }, 3000);
  }

  // ── 工具 ─────────────────────────────────────────

  function syncApiInput() {
    var el = document.getElementById("friend-api-base");
    if (el) el.value = FriendApi.getApiBase();
  }

  // ── 登录状态 ───────────────────────────────────

  function syncSessionUi() {
    var label = document.getElementById("session-label");
    var badge = document.getElementById("page-badge");
    var linkLogin = document.getElementById("link-login");
    var btnLogout = document.getElementById("btn-logout");
    var guestTip = document.getElementById("guest-tip");
    var heroUserInfo = document.getElementById("hero-user-info");
    var heroTextWrap = document.getElementById("hero-title");
    var heroSub = document.getElementById("hero-sub");
    var heroUserName = document.getElementById("hero-user-name");
    var heroUserId = document.getElementById("hero-user-id");
    var cardMyReg = document.getElementById("card-my-reg");
    var cardAvatar = document.getElementById("card-avatar");

    var isG = FriendApi.isGuestMode();
    var session = FriendApi.getSession();
    var isL = !!(session && session.token);

    if (isL) {
      var uid = session.userId != null ? String(session.userId) : "（验证码登录无 userId）";
      if (label) label.textContent = "已登录 · " + uid;
      if (badge) badge.textContent = "学员中心 · 已登录";
      if (linkLogin) linkLogin.classList.add("hidden");
      if (btnLogout) btnLogout.classList.remove("hidden");
      if (guestTip) guestTip.classList.add("hidden");
      if (heroUserInfo) heroUserInfo.classList.remove("hidden");
      if (heroTextWrap) heroTextWrap.textContent = "欢迎回来";
      if (heroSub) heroSub.textContent = "浏览竞赛、查看题目、在线刷题；登录后可报名竞赛、管理头像。";
      if (heroUserName) heroUserName.textContent = session.nickName || "学员";
      if (heroUserId) heroUserId.textContent = "ID: " + uid;
      if (cardMyReg) { cardMyReg.classList.remove("nav-card--disabled"); cardMyReg.removeAttribute("title"); }
      if (cardAvatar) { cardAvatar.classList.remove("nav-card--disabled"); cardAvatar.removeAttribute("title"); }
    } else {
      var display = isG ? "访客模式（未登录）" : "未登录";
      if (label) label.textContent = display;
      if (badge) badge.textContent = "学员中心";
      if (linkLogin) linkLogin.classList.remove("hidden");
      if (btnLogout) btnLogout.classList.add("hidden");
      if (guestTip) guestTip.classList.remove("hidden");
      if (heroUserInfo) heroUserInfo.classList.add("hidden");
      if (heroTextWrap) heroTextWrap.textContent = "欢迎使用 OJ 练习平台";
      if (heroSub) heroSub.textContent = "浏览竞赛与题目无需登录；报名竞赛、管理头像需先登录。";
      if (heroUserName) heroUserName.textContent = "访客";
      if (heroUserId) heroUserId.textContent = "ID: —";
      if (cardMyReg) {
        cardMyReg.classList.add("nav-card--disabled");
        cardMyReg.setAttribute("title", "请先登录");
        cardMyReg.addEventListener("click", function (e) {
          e.preventDefault();
          showToast("请先登录");
          location.href = "user-login.html";
        });
      }
      if (cardAvatar) {
        cardAvatar.classList.add("nav-card--disabled");
        cardAvatar.setAttribute("title", "请先登录");
        cardAvatar.addEventListener("click", function (e) {
          e.preventDefault();
          showToast("请先登录");
          location.href = "user-login.html";
        });
      }
    }
  }

  // ── 事件绑定 ───────────────────────────────────

  document.getElementById("btn-save-api") && document.getElementById("btn-save-api").addEventListener("click", function () {
    var v = document.getElementById("friend-api-base") && document.getElementById("friend-api-base").value && document.getElementById("friend-api-base").value.trim();
    if (v) FriendApi.setApiBase(v);
    showToast("网关地址已保存: " + FriendApi.getApiBase());
  });

  document.getElementById("btn-logout") && document.getElementById("btn-logout").addEventListener("click", function () {
    FriendApi.clearSession();
    FriendApi.setGuestMode(false);
    location.href = "user-login.html";
  });

  // ── 初始化 ─────────────────────────────────────

  syncApiInput();
  syncSessionUi();
})();
