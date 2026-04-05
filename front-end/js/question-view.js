/**
 * question-view.js — 题目详情页（question-view.html）
 * 从 URL ?questionId=xxx&examId=xxx 加载题目内容（题干、样例、代码模板）。
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

  function hasText(s) { return s != null && String(s).trim() !== ""; }

  function nl2brEscaped(text) {
    return escapeHtml(String(text).replace(/\r\n/g, "\n").replace(/\r/g, "\n")).replace(/\n/g, "<br />");
  }

  function difficultyClass(label) {
    var s = (label || "unknown").toLowerCase();
    if (s === "easy") return "q-diff q-diff--easy";
    if (s === "medium") return "q-diff q-diff--medium";
    if (s === "hard") return "q-diff q-diff--hard";
    return "q-diff q-diff--unknown";
  }

  function renderQuestionDetail(vo) {
    var qid = vo.questionId != null ? String(vo.questionId) : "—";
    var title = escapeHtml(vo.title || "未命名题目");
    var diffLabel = vo.difficultyLabel || "unknown";
    var chips = "";
    if (hasText(vo.timeLimit)) chips += '<span class="q-chip">⏱ 时间 <strong>' + escapeHtml(String(vo.timeLimit)) + '</strong> ms</span>';
    if (hasText(vo.spaceLimit)) chips += '<span class="q-chip">💾 空间 <strong>' + escapeHtml(String(vo.spaceLimit)) + '</strong> KB</span>';

    var html = '<div class="q-view">';
    html += '<header class="q-view-head">';
    html += '<div class="q-view-title-row">';
    html += '<h2 class="q-view-title">' + title + '</h2>';
    html += '<span class="' + difficultyClass(diffLabel) + '">' + escapeHtml(diffLabel) + '</span>';
    html += '</div>';
    html += '<p class="q-view-meta">题目编号 <span class="q-id-tag">#' + qid + '</span></p>';
    if (chips) html += '<div class="q-chips">' + chips + '</div>';
    html += '</header>';

    if (hasText(vo.content)) {
      html += '<section class="q-section">';
      html += '<h3 class="q-section-title">📋 题目描述</h3>';
      html += '<div class="q-prose">' + nl2brEscaped(vo.content) + '</div>';
      html += '</section>';
    }
    if (hasText(vo.questionCase)) {
      html += '<section class="q-section">';
      html += '<h3 class="q-section-title">📝 样例与测试说明</h3>';
      html += '<pre class="q-codeblock"><code>' + escapeHtml(vo.questionCase) + '</code></pre>';
      html += '</section>';
    }
    if (hasText(vo.defaultCode)) {
      html += '<section class="q-section">';
      html += '<h3 class="q-section-title">💻 默认代码模板</h3>';
      html += '<pre class="q-codeblock q-codeblock--single"><code>' + escapeHtml(vo.defaultCode) + '</code></pre>';
      html += '</section>';
    }
    if (hasText(vo.mainMethod)) {
      html += '<section class="q-section">';
      html += '<h3 class="q-section-title">⚙️ 主方法 / 入口签名</h3>';
      html += '<pre class="q-codeblock q-codeblock--single"><code>' + escapeHtml(vo.mainMethod) + '</code></pre>';
      html += '</section>';
    }
    if (!hasText(vo.content) && !hasText(vo.questionCase) && !hasText(vo.defaultCode) && !hasText(vo.mainMethod)) {
      html += '<p class="hint q-empty">该题目暂无正文描述，请联系管理员完善题库。</p>';
    }
    html += '</div>';
    return html;
  }

  // ── 状态切换 ─────────────────────────────────────
  function showState(name) {
    ["loading", "content", "error"].forEach(function (s) {
      var el = document.getElementById("state-" + s);
      if (el) el.classList.toggle("hidden", s !== name);
    });
  }

  // ── URL 参数 ────────────────────────────────────
  var params = new URLSearchParams(location.search);
  var examId = params.get("examId");
  var questionId = params.get("questionId");

  // 返回链接
  var backLink = document.getElementById("back-link");
  var footerBack = document.getElementById("footer-back");
  var backHref = examId ? "exam-detail.html?examId=" + encodeURIComponent(examId) : "exam-list.html";
  if (backLink) backLink.href = backHref;
  if (footerBack) footerBack.href = backHref;

  if (!questionId) {
    var errEl = document.getElementById("error-msg");
    if (errEl) errEl.textContent = "缺少 questionId 参数";
    showState("error");
  } else {
    var reqHint = document.getElementById("req-hint");
    if (reqHint) reqHint.textContent = "/question/detail?questionId=" + questionId;
    showState("loading");

    FriendApi.questionDetail(questionId).then(function (res) {
      var body = res.data;
      if (!body || body.code !== 1000 || !body.data) {
        var errEl = document.getElementById("error-msg");
        if (errEl) errEl.textContent = (body && body.msg) || "加载失败";
        showState("error");
        return;
      }
      var root = document.getElementById("q-view-root");
      if (root) root.innerHTML = renderQuestionDetail(body.data);
      var titleEl = document.getElementById("q-title");
      if (titleEl) titleEl.textContent = body.data.title || "题目详情";
      var badgeEl = document.getElementById("page-badge");
      if (badgeEl) badgeEl.textContent = "题目 " + questionId;
      showState("content");
    }).catch(function () {
      var errEl = document.getElementById("error-msg");
      if (errEl) errEl.textContent = "网络错误，请检查网关地址";
      showState("error");
    });
  }

  // ── 退出 ────────────────────────────────────────
  document.getElementById("btn-logout") && document.getElementById("btn-logout").addEventListener("click", function () {
    FriendApi.clearSession();
    FriendApi.setGuestMode(false);
    location.href = "user-login.html";
  });

  // ── 状态同步 ────────────────────────────────────
  function syncSession() {
    var label = document.getElementById("session-label");
    var isG = FriendApi.isGuestMode();
    var isL = !!(FriendApi.getSession() && FriendApi.getSession().token);
    if (isG) {
      if (label) label.textContent = "访客模式";
      if (document.getElementById("link-login")) document.getElementById("link-login").classList.remove("hidden");
      if (document.getElementById("btn-logout")) document.getElementById("btn-logout").classList.add("hidden");
    } else if (isL) {
      var s = FriendApi.getSession();
      if (label) label.textContent = "已登录 · " + (s.userId || "");
      if (document.getElementById("link-login")) document.getElementById("link-login").classList.add("hidden");
      if (document.getElementById("btn-logout")) document.getElementById("btn-logout").classList.remove("hidden");
    } else {
      if (label) label.textContent = "未登录";
      if (document.getElementById("link-login")) document.getElementById("link-login").classList.remove("hidden");
      if (document.getElementById("btn-logout")) document.getElementById("btn-logout").classList.add("hidden");
    }
  }
  syncSession();
})();
