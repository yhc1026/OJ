/**
 * question-list.js — 题目列表页（question-list.html）
 * 请求 GET /question/list?page=1，展示题目列表，点击跳转详情页。
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
    return String(s)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  function difficultyChip(label) {
    var s = (label || "unknown").toLowerCase();
    if (s === "easy")   return '<span class="qlist-diff qlist-diff--easy">简单</span>';
    if (s === "medium") return '<span class="qlist-diff qlist-diff--medium">中等</span>';
    if (s === "hard")   return '<span class="qlist-diff qlist-diff--hard">困难</span>';
    return '<span class="qlist-diff qlist-diff--unknown">' + escapeHtml(label || "未知") + '</span>';
  }

  function fmtTime(v) {
    if (v == null || v === "") return "—";
    if (typeof v === "string") return v;
    try { return new Date(v).toLocaleString("zh-CN"); }
    catch { return String(v); }
  }

  function renderQuestionRow(q) {
    var qid = q.questionId != null ? String(q.questionId) : "";
    var title = escapeHtml(q.title || "（无标题）");
    var diffLabel = q.difficultyLabel || "unknown";
    var createTime = fmtTime(q.createTime);
    return (
      '<a href="question-view.html?questionId=' + encodeURIComponent(qid) + '" class="qlist-row">' +
        '<div class="qlist-row-left">' +
          '<span class="qlist-row-title">' + title + '</span>' +
          '<span class="qlist-row-meta">#' + escapeHtml(qid) + ' · ' + createTime + '</span>' +
        '</div>' +
        '<div class="qlist-row-right">' +
          difficultyChip(diffLabel) +
          '<span class="qlist-row-arrow">→</span>' +
        '</div>' +
      '</a>'
    );
  }

  function renderList(list, total) {
    var el = document.getElementById("qlist-body");
    var countEl = document.getElementById("qlist-count");
    if (!el) return;
    if (!list || list.length === 0) {
      el.innerHTML = '<p class="hint qlist-empty">暂无题目数据</p>';
      if (countEl) countEl.textContent = "";
      return;
    }
    if (countEl) countEl.textContent = "共 " + (total || list.length) + " 道";
    el.innerHTML = list.map(renderQuestionRow).join("");
  }

  // ── 加载（带 page 参数） ──────────────────────────

  async function loadQuestionList() {
    var el = document.getElementById("qlist-body");
    if (el) el.innerHTML = '<p class="hint qlist-empty">加载中…</p>';
    var res = await FriendApi.request("/question/list?page=1", { withToken: false });
    var body = res.data;
    console.log("[题目列表] 响应:", body);
    if (okResult(body) && body.data && body.data.records) {
      renderList(body.data.records, body.data.total);
    } else if (okResult(body) && Array.isArray(body.data)) {
      renderList(body.data);
    } else {
      renderList([]);
      showToast((body && body.msg) || "题目列表加载失败");
    }
  }

  // ── 状态同步 ─────────────────────────────────────

  function syncSession() {
    var label = document.getElementById("session-label");
    var isL = !!(FriendApi.getSession() && FriendApi.getSession().token);
    if (label) label.textContent = isL ? "已登录" : "未登录";
    if (document.getElementById("link-login")) document.getElementById("link-login").classList.toggle("hidden", isL);
    if (document.getElementById("btn-logout")) document.getElementById("btn-logout").classList.toggle("hidden", !isL);
  }

  function syncApiInput() {
    var el = document.getElementById("friend-api-base");
    if (el) el.value = FriendApi.getApiBase();
  }

  // ── 事件 ─────────────────────────────────────────

  document.getElementById("btn-save-api") && document.getElementById("btn-save-api").addEventListener("click", function () {
    var v = (document.getElementById("friend-api-base") || {}).value || "";
    if (v.trim()) FriendApi.setApiBase(v.trim());
    showToast("网关地址已保存: " + FriendApi.getApiBase());
    loadQuestionList();
  });

  document.getElementById("btn-refresh") && document.getElementById("btn-refresh").addEventListener("click", loadQuestionList);

  document.getElementById("btn-logout") && document.getElementById("btn-logout").addEventListener("click", function () {
    FriendApi.clearSession();
    FriendApi.setGuestMode(false);
    location.href = "user-login.html";
  });

  // ── 初始化 ─────────────────────────────────────

  syncApiInput();
  syncSession();
  loadQuestionList();
})();
