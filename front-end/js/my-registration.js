/**
 * my-registration.js — 我的报名页（my-registration.html）
 * 展示当前用户已报名的竞赛列表，点击进入详情。
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

  function okResult(body) { return body && body.code === 1000; }

  function escapeHtml(s) {
    if (s == null) return "";
    return String(s).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
  }

  function fmtTime(v) {
    if (v == null || v === "") return "—";
    if (typeof v === "string") return v;
    try { return new Date(v).toLocaleString("zh-CN"); }
    catch { return String(v); }
  }

  function statusBadge(status) {
    var s = (status || "").toLowerCase();
    if (s === "进行中") return '<span class="badge badge--active">进行中</span>';
    if (s === "未开始") return '<span class="badge badge--pending">未开始</span>';
    if (s === "已结束") return '<span class="badge badge--finished">已结束</span>';
    return '<span class="badge">' + escapeHtml(status || "") + '</span>';
  }

  function renderExamRow(ex) {
    var id = ex.examId != null ? String(ex.examId) : "";
    var title = escapeHtml(ex.title || "（无标题）");
    var status = escapeHtml(ex.status || "");
    return (
      '<a href="exam-detail.html?examId=' + encodeURIComponent(id) + '" class="exam-row">' +
        '<div class="exam-row-left">' +
          '<span class="exam-row-title">' + title + '</span>' +
          '<span class="exam-row-meta">#' + id + ' · ' + fmtTime(ex.startTime) + ' ~ ' + fmtTime(ex.endTime) + '</span>' +
        '</div>' +
        '<div class="exam-row-right">' +
          statusBadge(status) +
        '</div>' +
      '</a>'
    );
  }

  function renderList(list) {
    var el = document.getElementById("my-reg-list");
    if (!el) return;
    if (!list || list.length === 0) {
      el.innerHTML = '<div class="list-empty">' +
        '<p class="list-empty-icon">📋</p>' +
        '<p class="list-empty-title">暂无报名记录</p>' +
        '<p class="hint">去竞赛列表逛逛吧</p>' +
        '<a href="exam-list.html" class="btn btn-sm btn-secondary">浏览竞赛</a>' +
      '</div>';
      return;
    }
    el.innerHTML = list.map(renderExamRow).join("");
  }

  function loadRegistrations() {
    var el = document.getElementById("my-reg-list");
    if (el) el.innerHTML = '<p class="hint exam-empty">加载中…</p>';
    FriendApi.listMyRegistrations().then(function (res) {
      var body = res.data;
      if (!body) {
        renderList([]);
        showToast("网络错误");
        return;
      }
      if (!okResult(body)) {
        renderList([]);
        showToast((body && body.msg) || "加载失败");
        return;
      }
      renderList(body.data && Array.isArray(body.data) ? body.data : []);
    }).catch(function () {
      renderList([]);
      showToast("请求失败");
    });
  }

  // ── 权限检查 ─────────────────────────────────────
  function checkAuth() {
    var s = FriendApi.getSession();
    if (s && s.token) {
      document.getElementById("state-unauth") && document.getElementById("state-unauth").classList.add("hidden");
      if (document.getElementById("state-content")) document.getElementById("state-content").classList.remove("hidden");
      if (document.getElementById("link-login")) document.getElementById("link-login").classList.add("hidden");
      if (document.getElementById("btn-logout")) document.getElementById("btn-logout").classList.remove("hidden");
      loadRegistrations();
    } else {
      document.getElementById("state-unauth") && document.getElementById("state-unauth").classList.remove("hidden");
      if (document.getElementById("state-content")) document.getElementById("state-content").classList.add("hidden");
      if (document.getElementById("link-login")) document.getElementById("link-login").classList.remove("hidden");
      if (document.getElementById("btn-logout")) document.getElementById("btn-logout").classList.add("hidden");
    }
  }

  document.getElementById("btn-refresh") && document.getElementById("btn-refresh").addEventListener("click", loadRegistrations);
  document.getElementById("btn-logout") && document.getElementById("btn-logout").addEventListener("click", function () {
    FriendApi.clearSession();
    FriendApi.setGuestMode(false);
    location.href = "user-login.html";
  });
  checkAuth();
})();
