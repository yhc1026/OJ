/**
 * exam-list.js — 竞赛列表页（exam-list.html）
 * 加载进行中 / 已结束竞赛列表，点击跳转独立详情页 exam-detail.html。
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

  function okResult(body) {
    return body && body.code === 1000;
  }

  function extractList(body) {
    if (!body || body.data == null) return [];
    return Array.isArray(body.data) ? body.data : [];
  }

  // ── 渲染 ───────────────────────────────────────────

  function escapeHtml(s) {
    if (s == null) return "";
    return String(s)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  function fmtTime(v) {
    if (v == null || v === "") return "—";
    if (typeof v === "string") return v;
    try { return new Date(v).toLocaleString("zh-CN"); }
    catch { return String(v); }
  }

  function statusDot(status) {
    var s = (status || "").toLowerCase();
    if (s === "未开始") return '<span class="status-dot status-dot--pending"></span>';
    if (s === "进行中") return '<span class="status-dot status-dot--active"></span>';
    if (s === "已结束") return '<span class="status-dot status-dot--finished"></span>';
    return '<span class="status-dot"></span>';
  }

  function renderExamRow(ex) {
    var id = ex.examId != null ? String(ex.examId) : "";
    var title = escapeHtml(ex.title || "（无标题）");
    var status = escapeHtml(ex.status || "");
    return (
      '<a href="exam-detail.html?examId=' + encodeURIComponent(id) + '" class="exam-row">' +
        '<div class="exam-row-left">' +
          '<span class="exam-row-title">' + title + '</span>' +
          '<span class="exam-row-meta">#' + id + '</span>' +
        '</div>' +
        '<div class="exam-row-right">' +
          '<span class="exam-row-status ' + (status.toLowerCase() === "进行中" ? "exam-row-status--active" : status.toLowerCase() === "已结束" ? "exam-row-status--finished" : "exam-row-status--pending") + '">' + statusDot(status) + status + '</span>' +
        '</div>' +
      '</a>'
    );
  }

  function renderList(containerId, exams, emptyText) {
    var el = document.getElementById(containerId);
    if (!el) return;
    el.innerHTML = "";
    if (!exams || exams.length === 0) {
      el.innerHTML = '<p class="hint exam-empty">' + escapeHtml(emptyText || "暂无") + '</p>';
      return;
    }
    el.innerHTML = exams.map(renderExamRow).join("");
  }

  // ── 加载 ───────────────────────────────────────────

  async function loadActive() {
    var el = document.getElementById("list-active");
    if (el) el.innerHTML = '<p class="hint exam-empty">加载中…</p>';
    var res = await FriendApi.listActiveExams();
    var body = res.data;
    if (okResult(body)) {
      renderList("list-active", body.data, "暂无进行中的竞赛");
    } else {
      renderList("list-active", [], body && body.msg ? body.msg : "加载失败");
      if (body && body.msg) showToast(body.msg);
    }
  }

  async function loadFinished() {
    var el = document.getElementById("list-finished");
    if (el) el.innerHTML = '<p class="hint exam-empty">加载中…</p>';
    var res = await FriendApi.listFinishedExams();
    var body = res.data;
    if (okResult(body)) {
      renderList("list-finished", body.data, "暂无已结束竞赛");
    } else {
      renderList("list-finished", [], body && body.msg ? body.msg : "加载失败");
    }
  }

  function loadAll() {
    loadActive();
    loadFinished();
  }

  // ── 状态同步 ───────────────────────────────────────

  function syncSession() {
    var label = document.getElementById("session-label");
    var badge = document.getElementById("page-badge");
    var linkLogin = document.getElementById("link-login");
    var isG = FriendApi.isGuestMode();
    var isL = !!(FriendApi.getSession() && FriendApi.getSession().token);
    if (isG) {
      if (label) label.textContent = "访客模式";
      if (badge) badge.textContent = "竞赛浏览 · 访客";
      if (linkLogin) linkLogin.classList.remove("hidden");
    } else if (isL) {
      var s = FriendApi.getSession();
      var uid = s.userId != null ? s.userId : "";
      if (label) label.textContent = "已登录 · " + uid;
      if (badge) badge.textContent = "竞赛浏览 · 已登录";
      if (linkLogin) linkLogin.classList.add("hidden");
    } else {
      if (label) label.textContent = "未登录";
      if (badge) badge.textContent = "竞赛浏览 · 访客";
      if (linkLogin) linkLogin.classList.remove("hidden");
    }
  }

  function syncApiInput() {
    var el = document.getElementById("friend-api-base");
    if (el) el.value = FriendApi.getApiBase();
  }

  // ── 事件 ───────────────────────────────────────────

  document.getElementById("btn-save-api") && document.getElementById("btn-save-api").addEventListener("click", function () {
    var v = document.getElementById("friend-api-base") && document.getElementById("friend-api-base").value && document.getElementById("friend-api-base").value.trim();
    if (v) FriendApi.setApiBase(v);
    showToast("网关地址已保存: " + FriendApi.getApiBase());
    loadAll();
  });

  document.getElementById("btn-refresh") && document.getElementById("btn-refresh").addEventListener("click", function () {
    loadAll();
  });

  document.getElementById("btn-goto-mine") && document.getElementById("btn-goto-mine").addEventListener("click", function () {
    var s = FriendApi.getSession();
    if (!s || !s.token) {
      showToast("请先登录");
      location.href = "user-login.html";
      return;
    }
    location.href = "my-registration.html";
  });

  document.getElementById("btn-logout") && document.getElementById("btn-logout").addEventListener("click", function () {
    FriendApi.clearSession();
    FriendApi.setGuestMode(false);
    location.href = "user-login.html";
  });

  // ── 初始化 ─────────────────────────────────────────

  syncApiInput();
  syncSession();
  loadAll();
})();
