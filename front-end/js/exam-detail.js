/**
 * exam-detail.js — 竞赛详情页（exam-detail.html）
 * 从 URL ?examId=xxx 加载竞赛详情、题目列表；点击题目跳转 question-view.html。
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

  // ── 状态切换 ─────────────────────────────────────
  function showState(name) {
    ["loading", "detail", "error"].forEach(function (s) {
      var el = document.getElementById("state-" + s);
      if (el) el.classList.toggle("hidden", s !== name);
    });
  }

  // ── 渲染题目列表 ─────────────────────────────────
  var currentExamId = null;

  function renderQuestionCard(qid, index) {
    var idStr = String(qid || "");
    return (
      '<a href="question-view.html?questionId=' + encodeURIComponent(idStr) + '&examId=' + encodeURIComponent(String(currentExamId)) + '" class="question-card card-elevated">' +
        '<div class="question-card-icon">📄</div>' +
        '<div class="question-card-info">' +
          '<span class="question-card-title">题目 ' + (index + 1) + '</span>' +
          '<span class="question-card-meta">ID: ' + escapeHtml(idStr) + '</span>' +
        '</div>' +
        '<div class="question-card-arrow">→</div>' +
      '</a>'
    );
  }

  function renderQuestionList(containerId, questionIds) {
    var el = document.getElementById(containerId);
    if (!el) return;
    if (!questionIds || questionIds.length === 0) {
      el.innerHTML = '<p class="hint exam-empty">本场竞赛暂无关联题目</p>';
      return;
    }
    el.innerHTML = questionIds.map(function (qid, i) { return renderQuestionCard(qid, i); }).join("");
  }

  // ── URL 参数 ────────────────────────────────────
  var examId = (function () {
    var params = new URLSearchParams(location.search);
    return params.get("examId");
  })();

  var backLink = document.getElementById("back-link");
  if (backLink) backLink.href = examId ? "exam-list.html" : "user-dashboard.html";

  if (!examId) {
    var errEl = document.getElementById("error-msg");
    if (errEl) errEl.textContent = "缺少 examId 参数";
    showState("error");
  } else {
    currentExamId = examId;
    showState("loading");

    FriendApi.getExamDetail(examId).then(function (res) {
      var body = res.data;
      if (!body || body.code !== 1000 || !body.data) {
        var errEl = document.getElementById("error-msg");
        if (errEl) errEl.textContent = (body && body.msg) || "加载失败";
        showState("error");
        return;
      }
      var d = body.data;
      document.getElementById("detail-title").textContent = d.title || "（无标题）";
      document.getElementById("detail-status").innerHTML = statusBadge(d.status);
      document.getElementById("detail-id").textContent = String(d.examId || "—");
      document.getElementById("detail-start").textContent = fmtTime(d.startTime);
      document.getElementById("detail-end").textContent = fmtTime(d.endTime);

      // 登录后显示报名区
      var s = FriendApi.getSession();
      if (s && s.token) {
        document.getElementById("reg-exam-id").value = String(examId);
        var rz = document.getElementById("register-zone");
        if (rz) rz.classList.remove("hidden");
      }

      renderQuestionList("question-list", d.questionIds);
      showState("detail");
    }).catch(function () {
      var errEl = document.getElementById("error-msg");
      if (errEl) errEl.textContent = "网络错误，请检查网关地址";
      showState("error");
    });
  }

  // ── 报名 ────────────────────────────────────────
  document.getElementById("btn-register-exam") && document.getElementById("btn-register-exam").addEventListener("click", function () {
    var s = FriendApi.getSession();
    if (!s || !s.token) {
      showToast("请先登录");
      location.href = "user-login.html?redirect=" + encodeURIComponent(location.href);
      return;
    }
    var hint = document.getElementById("register-hint");
    if (hint) hint.textContent = "报名中…";
    FriendApi.registerExam({ examId: String(examId) }).then(function (res) {
      var body = res.data;
      if (okResult(body)) {
        if (hint) hint.textContent = "报名成功！";
        showToast("报名成功");
      } else {
        if (hint) hint.textContent = (body && body.msg) || "报名失败";
        showToast((body && body.msg) || "报名失败");
      }
    }).catch(function () {
      if (hint) hint.textContent = "网络错误";
      showToast("网络错误");
    });
  });

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
