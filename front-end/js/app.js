/**
 * OJ admin frontend script for SysUser and Question modules.
 */
(function () {
  const STORAGE_API = "oj_api_base";
  const STORAGE_SESSION = "oj_admin_session";

  const $ = (id) => document.getElementById(id);

  const defaultApiBase = () =>
    localStorage.getItem(STORAGE_API) || "http://localhost:9000";

  let session = null;
  let listPage = { current: 1, pages: 1, total: 0 };
  let questionPage = { current: 1, pages: 1, total: 0 };
  let examPage = { current: 1, pages: 1, total: 0 };
  let pendingDelete = null;

  /** Panel meta info */
  const PANEL_INFO = {
    contest: {
      title: "\u6bd4\u8d5b\u7ba1\u7406",
      desc: "\u7ade\u8d5b\u5217\u8868\u3001\u6309ID/\u540d\u79f0\u67e5\u8be2\u3001\u65b0\u589e\u4e0e\u5220\u9664\u5df2\u63a5\u5165\u3002",
      sub: "\u7ade\u8d5b \u00b7 \u5df2\u63a5\u5165",
    },
    problem: {
      title: "\u9898\u76ee\u7ba1\u7406",
      desc: "\u9898\u76ee\u5217\u8868\u3001\u6309\u6807\u9898\u6a21\u7cca\u67e5\u8be2\u3001\u65b0\u589e\u4e0e\u5220\u9664\u5df2\u63a5\u5165\u3002",
      sub: "\u9898\u5e93 \u00b7 \u5df2\u63a5\u5165",
    },
    ojuser: {
      title: "OJ \u7528\u6237\u7ba1\u7406",
      desc: "\u7528\u4e8e\u7ba1\u7406\u666e\u901a OJ \u7528\u6237\uff0c\u5f53\u524d\u4e3b\u8981\u5c55\u793a\u9875\u9762\u6846\u67b6\u3002",
      sub: "\u7528\u6237\u4e2d\u5fc3 \u00b7 \u89c4\u5212\u4e2d",
    },
    sysuser: {
      title: "\u7cfb\u7edf\u7528\u6237\u7ba1\u7406",
      desc: "\u7ba1\u7406\u540e\u53f0\u767b\u5f55\u8d26\u53f7\uff0c\u652f\u6301\u5206\u9875\u67e5\u8be2\u3001\u6309\u6761\u4ef6\u67e5\u8be2\u4e0e\u65b0\u589e\u3002",
      sub: "\u7cfb\u7edf\u7528\u6237 \u00b7 \u5df2\u63a5\u5165",
    },
  };

  function apiBase() {
    const v = $("apiBase").value.trim();
    if (v) localStorage.setItem(STORAGE_API, v);
    return v || defaultApiBase();
  }

  function saveSession(s) {
    session = s;
    if (s) {
      sessionStorage.setItem(STORAGE_SESSION, JSON.stringify(s));
    } else {
      sessionStorage.removeItem(STORAGE_SESSION);
    }
  }

  function loadStoredSession() {
    try {
      const raw = sessionStorage.getItem(STORAGE_SESSION);
      if (!raw) return null;
      const s = JSON.parse(raw);
      if (s && s.token) return s;
    } catch {
      /* ignore */
    }
    return null;
  }

  function authHeaders() {
    const h = { "Content-Type": "application/json" };
    if (session && session.token) h.token = session.token;
    return h;
  }

  function isKickedOrAuthLost(status, data) {
    if (status === 401) return true;
    if (!data || typeof data !== "object") return false;
    if (data.code === 3001) return true;
    const msg = String(data.msg || "");
    if (
      msg.includes("\u767b\u5f55\u5931\u6548") ||
      msg.includes("\u672a\u767b\u5f55") ||
      msg.includes("token\u5931\u6548") ||
      msg.includes("token\u8fc7\u671f") ||
      msg.includes("\u8bf7\u91cd\u65b0\u767b\u5f55") ||
      msg.includes("\u8ba4\u8bc1\u5931\u8d25")
    ) {
      return true;
    }
    return false;
  }

  function showKickModal(message) {
    const overlay = $("modal-overlay");
    const title = $("modal-title");
    const text = $("modal-message");
    const isKick =
      message &&
      (message.includes("\u88ab\u9876\u4e0b\u7ebf") || message.includes("\u767b\u5f55\u5931\u6548"));
    title.textContent = isKick ? "\u8d26\u53f7\u5df2\u4e0b\u7ebf" : "\u767b\u5f55\u63d0\u9192";
    text.textContent =
      message ||
      (isKick
        ? "\u68c0\u6d4b\u5230\u8d26\u53f7\u5728\u5176\u5b83\u8bbe\u5907\u767b\u5f55\uff0c\u5f53\u524d\u4f1a\u8bdd\u5df2\u5931\u6548\uff0c\u8bf7\u91cd\u65b0\u767b\u5f55\u3002"
        : "\u767b\u5f55\u72b6\u6001\u5df2\u5931\u6548\uff0c\u8bf7\u91cd\u65b0\u767b\u5f55\u3002");
    overlay.classList.remove("hidden");
    overlay.setAttribute("aria-hidden", "false");
  }

  function hideKickModal() {
    $("modal-overlay").classList.add("hidden");
    $("modal-overlay").setAttribute("aria-hidden", "true");
  }

  function showConfirmDelete(message, onConfirm) {
    pendingDelete = onConfirm;
    $("confirm-message").textContent = message;
    $("confirm-overlay").classList.remove("hidden");
    $("confirm-overlay").setAttribute("aria-hidden", "false");
  }

  function hideConfirm() {
    pendingDelete = null;
    $("confirm-overlay").classList.add("hidden");
    $("confirm-overlay").setAttribute("aria-hidden", "true");
  }

  function clearSessionAndShowLogin() {
    saveSession(null);
    $("view-login").classList.remove("hidden");
    $("view-workspace").classList.add("hidden");
    $("user-bar").classList.add("hidden");
    $("login-status").textContent = "";
    $("login-status").className = "status-line muted";
    // Clear login form after logout to avoid stale credentials.
    const accIn = $("login-account");
    const pwdIn = $("login-password");
    if (accIn) accIn.value = "";
    if (pwdIn) pwdIn.value = "";
    resetAdminNavToSysUser();
  }

  /** Reset admin navigation to SysUser panel */
  function resetAdminNavToSysUser() {
    document.querySelectorAll(".sidebar-nav .nav-item").forEach((btn) => {
      btn.classList.toggle("active", btn.getAttribute("data-panel") === "sysuser");
    });
    document.querySelectorAll(".admin-panel").forEach((el) => {
      el.classList.toggle("hidden", el.id !== "panel-sysuser");
    });
    const sub = $("header-subtitle");
    if (sub) sub.textContent = "\u7cfb\u7edf\u7528\u6237";
  }

  function switchAdminPanel(key) {
    const info = PANEL_INFO[key];
    if (!info) return;

    document.querySelectorAll(".admin-panel").forEach((el) => {
      el.classList.toggle("hidden", el.id !== `panel-${key}`);
    });
    document.querySelectorAll(".sidebar-nav .nav-item").forEach((btn) => {
      btn.classList.toggle("active", btn.getAttribute("data-panel") === key);
    });

    const titleEl = $("admin-content-title");
    const descEl = $("admin-content-desc");
    if (titleEl) titleEl.textContent = info.title;
    if (descEl) descEl.textContent = info.desc;

    const sub = $("header-subtitle");
    if (sub) sub.textContent = info.sub;

    const protoFooter = $("footer-proto-note");
    const sysFooter = $("footer-sysuser-note");
    if (protoFooter && sysFooter) {
      if (key === "sysuser") {
        protoFooter.classList.add("hidden");
        sysFooter.classList.remove("hidden");
      } else {
        protoFooter.classList.remove("hidden");
        sysFooter.classList.add("hidden");
      }
    }

    if (key === "sysuser" && session && session.token) {
      loadUserList();
    }
    if (key === "contest" && session && session.token) {
      loadExamList();
    }
    if (key === "problem") {
      loadQuestionList();
    }
  }

  async function apiFetch(path, options = {}) {
    const method = options.method || "GET";
    const opts = {
      method,
      headers: authHeaders(),
    };
    if (options.body !== undefined) {
      opts.body =
        typeof options.body === "string"
          ? options.body
          : JSON.stringify(options.body);
    }
    const res = await fetch(`${apiBase()}${path}`, opts);
    const text = await res.text();
    let data;
    try {
      data = JSON.parse(text);
    } catch {
      data = { msg: text, code: -1, parseError: true };
    }
    return { ok: res.ok, status: res.status, data };
  }

  async function requestWithAuth(path, options) {
    const result = await apiFetch(path, options || { method: "GET" });
    const { ok, status, data } = result;
    if (session && session.token && isKickedOrAuthLost(status, data)) {
      const msg = data.msg || (status === 401 ? "\u8ba4\u8bc1\u5931\u8d25\uff0c\u8bf7\u91cd\u65b0\u767b\u5f55" : "");
      clearSessionAndShowLogin();
      showKickModal(msg);
      return { ...result, kicked: true };
    }
    return { ...result, kicked: false };
  }

  function escapeHtml(s) {
    if (!s) return "";
    return String(s)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  function showWorkspace() {
    $("view-login").classList.add("hidden");
    $("view-workspace").classList.remove("hidden");
    $("user-bar").classList.remove("hidden");
    const nick = session.nickName || session.userAccount || "\u7528\u6237";
    $("user-bar-text").textContent = nick;
    const av = (nick && nick[0]) || "U";
    $("user-avatar").textContent = av;
  }

  async function doLogin() {
    const acc = $("login-account").value.trim();
    const pwd = $("login-password").value;
    const st = $("login-status");
    if (!acc || !pwd) {
      st.textContent = "\u8bf7\u8f93\u5165\u8d26\u53f7\u548c\u5bc6\u7801";
      st.className = "status-line err";
      return;
    }
    st.textContent = "\u767b\u5f55\u4e2d...";
    st.className = "status-line muted";
    const path = `/sysUser/login?userAccount=${encodeURIComponent(
      acc
    )}&pwd=${encodeURIComponent(pwd)}`;
    const { ok, status, data } = await apiFetch(path, { method: "GET" });
    if (ok && data.code === 1000 && data.data && data.data.token) {
      saveSession({
        token: data.data.token,
        userId: data.data.userId,
        nickName: data.data.nickName,
        userAccount: acc,
      });
      st.textContent = "";
      listPage.current = 1;
      showWorkspace();
      switchAdminPanel("sysuser");
    } else {
      st.textContent = data.msg || `\u767b\u5f55\u5931\u8d25\uff08HTTP ${status}\uff09`;
      st.className = "status-line err";
    }
  }

  function doLogout() {
    clearSessionAndShowLogin();
    $("user-table-wrap").innerHTML = "";
    $("list-status").textContent = "";
    $("insert-status").textContent = "";
    $("query-status").textContent = "";
    $("query-result").classList.add("hidden");
    $("query-result").innerHTML = "";
    $("question-table-wrap").innerHTML = "";
    $("question-list-status").textContent = "";
    $("question-query-status").textContent = "";
    $("question-query-result").classList.add("hidden");
    $("question-query-result").innerHTML = "";
    $("question-insert-status").textContent = "";
    $("exam-table-wrap").innerHTML = "";
    $("exam-list-status").textContent = "";
    $("exam-query-status").textContent = "";
    $("exam-query-result").classList.add("hidden");
    $("exam-query-result").innerHTML = "";
    $("exam-insert-status").textContent = "";
    $("exam-question-status").textContent = "";
    $("exam-question-result").classList.add("hidden");
    $("exam-question-result").innerHTML = "";
    $("exam-question-detail-status").textContent = "";
    $("exam-question-detail-result").classList.add("hidden");
    $("exam-question-detail-result").innerHTML = "";
  }

  function updatePagerUi() {
    const { current, pages, total } = listPage;
    $("page-indicator").textContent = `\u7b2c ${current} / ${Math.max(pages, 1)} \u9875\uff0c\u5171 ${total} \u6761`;
    $("btn-page-prev").disabled = current <= 1;
    $("btn-page-next").disabled = current >= pages || pages < 1;
  }

  async function loadUserList() {
    if (!session || !session.token) return;
    const wrap = $("user-table-wrap");
    const st = $("list-status");
    st.textContent = "\u52a0\u8f7d\u4e2d...";
    st.className = "status-line muted";
    const page = Math.max(1, listPage.current);
    const { ok, data, kicked } = await requestWithAuth(
      `/sysUser/sys-users?page=${page}`
    );
    if (kicked) return;
    if (!ok || data.code !== 1000) {
      st.textContent = data.msg || "\u52a0\u8f7d\u5931\u8d25";
      st.className = "status-line err";
      wrap.innerHTML = "";
      return;
    }
    st.textContent = "";
    const pg = data.data || {};
    const records = pg.records || [];
    listPage.current = pg.current || page;
    listPage.pages = pg.pages || 1;
    listPage.total = pg.total != null ? pg.total : records.length;
    updatePagerUi();

    if (records.length === 0) {
      wrap.innerHTML = '<div class="empty-state">\u6682\u65e0\u6570\u636e</div>';
      return;
    }
    const selfId = session.userId != null ? String(session.userId) : "";
    let html =
      '<table class="data-table"><thead><tr><th>\u7528\u6237ID</th><th>\u8d26\u53f7</th><th>\u6635\u79f0</th><th class="col-actions">\u64cd\u4f5c</th></tr></thead><tbody>';
    for (const u of records) {
      const uid = String(u.userId);
      const isSelf = selfId && uid === selfId;
      html += `<tr data-user-id="${escapeHtml(uid)}" data-user-account="${escapeHtml(
        u.userAccount || ""
      )}">
        <td><code class="cell-mono">${escapeHtml(uid)}</code></td>
        <td>${escapeHtml(u.userAccount || "")}</td>
        <td>${escapeHtml(u.nickName || "")}</td>
        <td class="col-actions">
          <button type="button" class="btn btn-text-danger btn-delete-row" ${
            isSelf ? "disabled title=\"\u4e0d\u80fd\u5220\u9664\u5f53\u524d\u767b\u5f55\u8d26\u53f7\"" : ""
          }>\u5220\u9664</button>
        </td>
      </tr>`;
    }
    html += "</tbody></table>";
    wrap.innerHTML = html;
  }

  function updateQuestionPagerUi() {
    const { current, pages, total } = questionPage;
    $("question-page-indicator").textContent = `\u7b2c ${current} / ${Math.max(
      pages,
      1
    )} \u9875\uff0c\u5171 ${total} \u6761`;
    $("btn-question-page-prev").disabled = current <= 1;
    $("btn-question-page-next").disabled = current >= pages || pages < 1;
  }

  function renderQuestionDifficultyLabel(q) {
    return q.difficultyLabel || (q.difficulty != null ? String(q.difficulty) : "unknown");
  }

  function renderQuestionDetail(q) {
    return `
      <dl class="detail-dl">
        <div class="detail-row"><dt>\u9898\u76ee ID</dt><dd><code>${escapeHtml(
          String(q.questionId ?? "")
        )}</code></dd></div>
        <div class="detail-row"><dt>\u6807\u9898</dt><dd>${escapeHtml(q.title || "")}</dd></div>
        <div class="detail-row"><dt>\u96be\u5ea6</dt><dd>${escapeHtml(
          renderQuestionDifficultyLabel(q)
        )}</dd></div>
        <div class="detail-row"><dt>\u65f6\u95f4\u9650\u5236</dt><dd>${escapeHtml(
          String(q.timeLimit ?? "\u2014")
        )}</dd></div>
        <div class="detail-row"><dt>\u7a7a\u95f4\u9650\u5236</dt><dd>${escapeHtml(
          String(q.spaceLimit ?? "\u2014")
        )}</dd></div>
        <div class="detail-row"><dt>\u9898\u76ee\u5185\u5bb9</dt><dd>${escapeHtml(
          q.content || "\u2014"
        )}</dd></div>
        <div class="detail-row"><dt>\u6d4b\u8bd5\u7528\u4f8b</dt><dd>${escapeHtml(
          q.questionCase || "\u2014"
        )}</dd></div>
        <div class="detail-row"><dt>\u9ed8\u8ba4\u4ee3\u7801</dt><dd>${escapeHtml(
          q.defaultCode || "\u2014"
        )}</dd></div>
        <div class="detail-row"><dt>Main \u65b9\u6cd5</dt><dd>${escapeHtml(
          q.mainMethod || "\u2014"
        )}</dd></div>
      </dl>`;
  }

  function renderQuestionTable(records) {
    if (!records || records.length === 0) {
      return '<div class="empty-state">\u6682\u65e0\u6570\u636e</div>';
    }
    let html =
      '<table class="data-table"><thead><tr><th>\u9898\u76eeID</th><th>\u6807\u9898</th><th>\u96be\u5ea6</th><th>\u65f6\u95f4\u9650\u5236</th><th>\u7a7a\u95f4\u9650\u5236</th><th class="col-actions">\u64cd\u4f5c</th></tr></thead><tbody>';
    for (const q of records) {
      html += `<tr class="row-clickable" data-question-id="${escapeHtml(String(q.questionId ?? ""))}" data-question-title="${escapeHtml(
        q.title || ""
      )}">
        <td><code class="cell-mono">${escapeHtml(String(q.questionId ?? ""))}</code></td>
        <td>${escapeHtml(q.title || "")}</td>
        <td>${escapeHtml(renderQuestionDifficultyLabel(q))}</td>
        <td>${escapeHtml(String(q.timeLimit ?? "\u2014"))}</td>
        <td>${escapeHtml(String(q.spaceLimit ?? "\u2014"))}</td>
        <td class="col-actions">
          <button type="button" class="btn btn-text-danger btn-delete-question-row">\u5220\u9664</button>
        </td>
      </tr>`;
    }
    html += "</tbody></table>";
    return html;
  }

  async function loadQuestionList() {
    const wrap = $("question-table-wrap");
    const st = $("question-list-status");
    st.textContent = "\u52a0\u8f7d\u4e2d...";
    st.className = "status-line muted";
    const page = Math.max(1, questionPage.current);
    const { ok, status, data } = await apiFetch(`/question/page?page=${page}`);
    if (!ok || data.code !== 1000) {
      st.textContent = data.msg || `\u52a0\u8f7d\u5931\u8d25\uff08HTTP ${status}\uff09`;
      st.className = "status-line err";
      wrap.innerHTML = "";
      return;
    }
    const pg = data.data || {};
    const records = pg.records || [];
    questionPage.current = pg.current || page;
    questionPage.pages = pg.pages || 1;
    questionPage.total = pg.total != null ? pg.total : records.length;
    updateQuestionPagerUi();
    wrap.innerHTML = renderQuestionTable(records);
    st.textContent = "";
  }

  async function queryQuestionBriefById() {
    const raw = $("question-query-id").value.trim();
    const id = parseInt(raw, 10);
    const st = $("question-query-status");
    const box = $("question-query-result");
    if (!raw || Number.isNaN(id)) {
      st.textContent = "\u8bf7\u8f93\u5165\u5408\u6cd5\u9898\u76ee ID";
      st.className = "status-line err";
      box.classList.add("hidden");
      return;
    }
    st.textContent = "\u67e5\u8be2\u4e2d...";
    st.className = "status-line muted";
    const { ok, data } = await apiFetch(
      `/question/brief?questionId=${encodeURIComponent(id)}`
    );
    if (!ok || data.code !== 1000 || !data.data) {
      st.textContent = data.msg || "\u67e5\u8be2\u5931\u8d25";
      st.className = "status-line err";
      box.classList.add("hidden");
      return;
    }
    st.textContent = "\u67e5\u8be2\u6210\u529f";
    st.className = "status-line ok";
    box.innerHTML = renderQuestionDetail(data.data);
    box.classList.remove("hidden");
  }

  async function queryQuestionDetailById() {
    const raw = $("question-query-id").value.trim();
    const id = parseInt(raw, 10);
    const st = $("question-query-status");
    const box = $("question-query-result");
    if (!raw || Number.isNaN(id)) {
      st.textContent = "\u8bf7\u8f93\u5165\u5408\u6cd5\u9898\u76ee ID";
      st.className = "status-line err";
      box.classList.add("hidden");
      return;
    }
    st.textContent = "\u67e5\u8be2\u4e2d...";
    st.className = "status-line muted";
    const { ok, data } = await apiFetch(
      `/question/detail?questionId=${encodeURIComponent(id)}`
    );
    if (!ok || data.code !== 1000 || !data.data) {
      st.textContent = data.msg || "\u67e5\u8be2\u5931\u8d25";
      st.className = "status-line err";
      box.classList.add("hidden");
      return;
    }
    st.textContent = "\u67e5\u8be2\u6210\u529f";
    st.className = "status-line ok";
    box.innerHTML = renderQuestionDetail(data.data);
    box.classList.remove("hidden");
  }

  async function queryQuestionBriefByTitle() {
    const title = $("question-query-title").value.trim();
    const st = $("question-query-status");
    const box = $("question-query-result");
    if (!title) {
      st.textContent = "\u8bf7\u8f93\u5165\u6807\u9898\u5173\u952e\u5b57";
      st.className = "status-line err";
      box.classList.add("hidden");
      return;
    }
    st.textContent = "\u67e5\u8be2\u4e2d...";
    st.className = "status-line muted";
    const { ok, data } = await apiFetch(
      `/question/brief-by-title?title=${encodeURIComponent(title)}`
    );
    if (!ok || data.code !== 1000 || !data.data) {
      st.textContent = data.msg || "\u67e5\u8be2\u5931\u8d25";
      st.className = "status-line err";
      box.classList.add("hidden");
      return;
    }
    st.textContent = "\u67e5\u8be2\u6210\u529f";
    st.className = "status-line ok";
    box.innerHTML = renderQuestionDetail(data.data);
    box.classList.remove("hidden");
  }

  async function queryQuestionDetailByTitle() {
    const title = $("question-query-title").value.trim();
    const st = $("question-query-status");
    const box = $("question-query-result");
    if (!title) {
      st.textContent = "\u8bf7\u8f93\u5165\u9898\u76ee\u6807\u9898";
      st.className = "status-line err";
      box.classList.add("hidden");
      return;
    }
    st.textContent = "\u67e5\u8be2\u4e2d...";
    st.className = "status-line muted";
    const { ok, data } = await apiFetch(
      `/question/detail-by-title?title=${encodeURIComponent(title)}`
    );
    if (!ok || data.code !== 1000 || !data.data) {
      st.textContent = data.msg || "\u67e5\u8be2\u5931\u8d25";
      st.className = "status-line err";
      box.classList.add("hidden");
      return;
    }
    st.textContent = "\u67e5\u8be2\u6210\u529f";
    st.className = "status-line ok";
    box.innerHTML = renderQuestionDetail(data.data);
    box.classList.remove("hidden");
  }

  async function queryQuestionListByDifficulty() {
    const diff = $("question-query-difficulty").value;
    const st = $("question-query-status");
    const box = $("question-query-result");
    st.textContent = "\u67e5\u8be2\u4e2d...";
    st.className = "status-line muted";
    const { ok, data } = await apiFetch(
      `/question/list-by-difficulty?difficulty=${encodeURIComponent(diff)}`
    );
    if (!ok || data.code !== 1000 || !Array.isArray(data.data)) {
      st.textContent = data.msg || "\u67e5\u8be2\u5931\u8d25";
      st.className = "status-line err";
      box.classList.add("hidden");
      return;
    }
    const list = data.data;
    st.textContent = `\u6309\u96be\u5ea6\u67e5\u8be2\u5230 ${list.length} \u6761`;
    st.className = "status-line ok";
    box.innerHTML = renderQuestionTable(list);
    box.classList.remove("hidden");
  }

  async function queryQuestionListByTitleLike() {
    const title = $("question-query-title").value.trim();
    const st = $("question-query-status");
    const box = $("question-query-result");
    if (!title) {
      st.textContent = "\u8bf7\u8f93\u5165\u6807\u9898\u5173\u952e\u5b57";
      st.className = "status-line err";
      box.classList.add("hidden");
      return;
    }
    st.textContent = "\u67e5\u8be2\u4e2d...";
    st.className = "status-line muted";
    const { ok, data } = await apiFetch(
      `/question/list-by-title-like?title=${encodeURIComponent(title)}`
    );
    if (!ok || data.code !== 1000 || !Array.isArray(data.data)) {
      st.textContent = data.msg || "\u67e5\u8be2\u5931\u8d25";
      st.className = "status-line err";
      box.classList.add("hidden");
      return;
    }
    const list = data.data;
    st.textContent = `\u6a21\u7cca\u67e5\u8be2\u5230 ${list.length} \u6761`;
    st.className = "status-line ok";
    box.innerHTML = renderQuestionTable(list);
    box.classList.remove("hidden");
  }

  function buildQuestionPayload() {
    const title = $("question-new-title").value.trim();
    const difficulty = parseInt($("question-new-difficulty").value, 10);
    const content = $("question-new-content").value.trim();
    const questionCase = $("question-new-case").value.trim();
    const defaultCode = $("question-new-default-code").value;
    const mainMethod = $("question-new-main-method").value;
    const tRaw = $("question-new-time-limit").value.trim();
    const sRaw = $("question-new-space-limit").value.trim();
    return {
      title,
      difficulty,
      timeLimit: tRaw === "" ? null : parseInt(tRaw, 10),
      spaceLimit: sRaw === "" ? null : parseInt(sRaw, 10),
      content,
      questionCase,
      defaultCode,
      mainMethod,
    };
  }

  async function insertQuestion() {
    if (!session || !session.token) {
      $("question-insert-status").textContent = "\u8bf7\u5148\u767b\u5f55\u518d\u65b0\u589e\u9898\u76ee";
      $("question-insert-status").className = "status-line err";
      return;
    }
    const st = $("question-insert-status");
    const body = buildQuestionPayload();
    if (
      !body.title ||
      Number.isNaN(body.difficulty) ||
      !body.content ||
      !body.questionCase ||
      !body.defaultCode ||
      !body.mainMethod
    ) {
      st.textContent = "\u8bf7\u5b8c\u6574\u586b\u5199\u5fc5\u586b\u5b57\u6bb5";
      st.className = "status-line err";
      return;
    }
    st.textContent = "\u63d0\u4ea4\u4e2d...";
    st.className = "status-line muted";
    const { ok, data, kicked } = await requestWithAuth("/question/addQuestion", {
      method: "POST",
      body,
    });
    if (kicked) return;
    if (ok && data.code === 1000) {
      st.textContent = `\u65b0\u589e\u6210\u529f\uff0c\u9898\u76eeID\uff1a${data.data}`;
      st.className = "status-line ok";
      $("question-new-title").value = "";
      $("question-new-content").value = "";
      $("question-new-case").value = "";
      $("question-new-default-code").value = "";
      $("question-new-main-method").value = "";
      $("question-new-time-limit").value = "";
      $("question-new-space-limit").value = "";
      questionPage.current = 1;
      loadQuestionList();
    } else {
      st.textContent = data.msg || "\u65b0\u589e\u5931\u8d25";
      st.className = "status-line err";
    }
  }

  async function deleteQuestionById(id, title) {
    const { ok, data, kicked } = await requestWithAuth(
      `/question/by-id?questionId=${encodeURIComponent(id)}`,
      { method: "DELETE" }
    );
    if (kicked) return;
    if (ok && data.code === 1000) {
      $("question-list-status").textContent = `\u5220\u9664\u6210\u529f\uff1a${title || id}`;
      $("question-list-status").className = "status-line ok";
      loadQuestionList();
    } else {
      $("question-list-status").textContent = data.msg || "\u5220\u9664\u5931\u8d25";
      $("question-list-status").className = "status-line err";
    }
  }

  function onQuestionTableClick(e) {
    const btn = e.target.closest(".btn-delete-question-row");
    if (btn) {
      const tr = btn.closest("tr");
      if (!tr) return;
      const id = tr.getAttribute("data-question-id");
      const title = tr.getAttribute("data-question-title") || "";
      if (!id) return;
      showConfirmDelete(
        `\u786e\u5b9a\u5220\u9664\u9898\u76ee\u300c${title || id}\u300d\u5417\uff1f\u6b64\u64cd\u4f5c\u4e0d\u53ef\u6062\u590d\u3002`,
        () => deleteQuestionById(id, title)
      );
      return;
    }
    const row = e.target.closest("tr[data-question-id]");
    if (!row) return;
    markSelectedQuestionRow(row);
    loadQuestionDetailByRow(row);
  }

  function onQuestionQueryResultClick(e) {
    const row = e.target.closest("tr[data-question-id]");
    if (!row) return;
    markSelectedQuestionRow(row);
    loadQuestionDetailByRow(row);
  }

  function markSelectedQuestionRow(activeRow) {
    document
      .querySelectorAll("#question-table-wrap tr.row-clickable, #question-query-result tr.row-clickable")
      .forEach((tr) => tr.classList.remove("row-selected"));
    activeRow.classList.add("row-selected");
  }

  async function loadQuestionDetailById(questionId) {
    const st = $("question-detail-status");
    const box = $("question-detail-result");
    const listSt = $("question-list-status");
    st.textContent = "\u8be6\u60c5\u52a0\u8f7d\u4e2d...";
    st.className = "status-line muted";
    listSt.textContent = `\u6b63\u5728\u52a0\u8f7d\u9898\u76ee ${questionId} \u7684\u8be6\u60c5`;
    listSt.className = "status-line muted";
    const { ok, data } = await apiFetch(
      `/question/detail?questionId=${encodeURIComponent(questionId)}`
    );
    if (!ok || data.code !== 1000 || !data.data) {
      st.textContent = data.msg || "\u8be6\u60c5\u52a0\u8f7d\u5931\u8d25";
      st.className = "status-line err";
      listSt.textContent = st.textContent;
      listSt.className = "status-line err";
      box.classList.add("hidden");
      return;
    }
    const q = data.data;
    st.textContent = "\u8be6\u60c5\u52a0\u8f7d\u6210\u529f";
    st.className = "status-line ok";
    box.innerHTML = `
      <div class="question-detail-block">
        <h4>\u9898\u76ee\u5185\u5bb9</h4>
        <pre class="question-detail-pre">${escapeHtml(q.content || "\u2014")}</pre>
      </div>
      <div class="question-detail-block">
        <h4>\u6d4b\u8bd5\u7528\u4f8b</h4>
        <pre class="question-detail-pre">${escapeHtml(q.questionCase || "\u2014")}</pre>
      </div>
      <div class="question-detail-block">
        <h4>\u9ed8\u8ba4\u4ee3\u7801</h4>
        <pre class="question-detail-pre">${escapeHtml(q.defaultCode || "\u2014")}</pre>
      </div>
      <div class="question-detail-block">
        <h4>Main \u65b9\u6cd5</h4>
        <pre class="question-detail-pre">${escapeHtml(q.mainMethod || "\u2014")}</pre>
      </div>
    `;
    box.classList.remove("hidden");
    box.scrollIntoView({ behavior: "smooth", block: "start" });
    listSt.textContent = `\u9898\u76ee ${questionId} \u8be6\u60c5\u5df2\u52a0\u8f7d`;
    listSt.className = "status-line ok";
  }

  async function loadQuestionDetailByRow(row) {
    const qid = row.getAttribute("data-question-id");
    if (!qid) {
      const st = $("question-detail-status");
      st.textContent = "\u7f3a\u5c11\u9898\u76eeID\uff0c\u65e0\u6cd5\u52a0\u8f7d\u8be6\u60c5";
      st.className = "status-line err";
      return;
    }
    await loadQuestionDetailById(qid);
  }

  function updateExamPagerUi() {
    const { current, pages, total } = examPage;
    $("exam-page-indicator").textContent = `\u7b2c ${current} / ${Math.max(
      pages,
      1
    )} \u9875\uff0c\u5171 ${total} \u6761`;
    $("btn-exam-page-prev").disabled = current <= 1;
    $("btn-exam-page-next").disabled = current >= pages || pages < 1;
  }

  function renderExamStatusLabel(status) {
    if (status === 0) return "\u672a\u5f00\u59cb";
    if (status === 1) return "\u8fdb\u884c\u4e2d";
    if (status === 2) return "\u5df2\u7ed3\u675f";
    return String(status ?? "\u2014");
  }

  function renderExamTimeRange(e) {
    const s = e.startTime || "\u2014";
    const n = e.endTime || "\u2014";
    return `${s} ~ ${n}`;
  }

  function renderExamDetail(e) {
    return `
      <dl class="detail-dl">
        <div class="detail-row"><dt>\u7ade\u8d5b ID</dt><dd><code>${escapeHtml(String(e.examId || ""))}</code></dd></div>
        <div class="detail-row"><dt>\u540d\u79f0</dt><dd>${escapeHtml(e.title || "")}</dd></div>
        <div class="detail-row"><dt>\u72b6\u6001</dt><dd>${escapeHtml(renderExamStatusLabel(e.status))}</dd></div>
        <div class="detail-row"><dt>\u5f00\u59cb\u65f6\u95f4</dt><dd>${escapeHtml(String(e.startTime || "\u2014"))}</dd></div>
        <div class="detail-row"><dt>\u7ed3\u675f\u65f6\u95f4</dt><dd>${escapeHtml(String(e.endTime || "\u2014"))}</dd></div>
      </dl>`;
  }

  function renderExamTable(records) {
    if (!records || records.length === 0) {
      return '<div class="empty-state">\u6682\u65e0\u6570\u636e</div>';
    }
    let html =
      '<table class="data-table"><thead><tr><th>\u7ade\u8d5bID</th><th>\u540d\u79f0</th><th>\u65f6\u95f4</th><th>\u72b6\u6001</th><th class="col-actions">\u64cd\u4f5c</th></tr></thead><tbody>';
    for (const e of records) {
      html += `<tr class="row-clickable" data-exam-id="${escapeHtml(String(
        e.examId || ""
      ))}" data-exam-title="${escapeHtml(e.title || "")}">
        <td><code class="cell-mono">${escapeHtml(String(e.examId || ""))}</code></td>
        <td>${escapeHtml(e.title || "")}</td>
        <td>${escapeHtml(renderExamTimeRange(e))}</td>
        <td>${escapeHtml(renderExamStatusLabel(e.status))}</td>
        <td class="col-actions">
          <button type="button" class="btn btn-text-danger btn-delete-exam-row">\u5220\u9664</button>
        </td>
      </tr>`;
    }
    html += "</tbody></table>";
    return html;
  }

  function renderQuestionReadonlyTable(records) {
    if (!records || records.length === 0) {
      return '<div class="empty-state">\u6682\u65e0\u9898\u76ee</div>';
    }
    let html =
      '<table class="data-table"><thead><tr><th>\u9898\u76eeID</th><th>\u6807\u9898</th><th>\u96be\u5ea6</th><th>\u65f6\u95f4\u9650\u5236</th><th>\u7a7a\u95f4\u9650\u5236</th></tr></thead><tbody>';
    for (const q of records) {
      html += `<tr class="row-clickable" data-question-id="${escapeHtml(String(
        q.questionId || ""
      ))}">
        <td><code class="cell-mono">${escapeHtml(String(q.questionId || ""))}</code></td>
        <td>${escapeHtml(q.title || "")}</td>
        <td>${escapeHtml(renderQuestionDifficultyLabel(q))}</td>
        <td>${escapeHtml(String(q.timeLimit ?? "\u2014"))}</td>
        <td>${escapeHtml(String(q.spaceLimit ?? "\u2014"))}</td>
      </tr>`;
    }
    html += "</tbody></table>";
    return html;
  }

  async function loadExamList() {
    if (!session || !session.token) return;
    const wrap = $("exam-table-wrap");
    const st = $("exam-list-status");
    st.textContent = "\u52a0\u8f7d\u4e2d...";
    st.className = "status-line muted";
    const page = Math.max(1, examPage.current);
    const { ok, data, kicked } = await requestWithAuth(`/exam/list?page=${page}`);
    if (kicked) return;
    if (!ok || data.code !== 1000) {
      st.textContent = data.msg || "\u52a0\u8f7d\u5931\u8d25";
      st.className = "status-line err";
      wrap.innerHTML = "";
      return;
    }
    const pg = data.data || {};
    const records = pg.records || [];
    examPage.current = pg.current || page;
    examPage.pages = pg.pages || 1;
    examPage.total = pg.total != null ? pg.total : records.length;
    updateExamPagerUi();
    wrap.innerHTML = renderExamTable(records);
    st.textContent = "";
  }

  async function queryExamById() {
    if (!session || !session.token) return;
    const id = $("exam-query-id").value.trim();
    const st = $("exam-query-status");
    const box = $("exam-query-result");
    if (!id || !/^\d+$/.test(id)) {
      st.textContent = "\u8bf7\u8f93\u5165\u5408\u6cd5\u7ade\u8d5bID";
      st.className = "status-line err";
      box.classList.add("hidden");
      return;
    }
    st.textContent = "\u67e5\u8be2\u4e2d...";
    st.className = "status-line muted";
    const { ok, data, kicked } = await requestWithAuth(
      `/exam/getExamById?examId=${encodeURIComponent(id)}`
    );
    if (kicked) return;
    if (!ok || data.code !== 1000 || !data.data) {
      st.textContent = data.msg || "\u67e5\u8be2\u5931\u8d25";
      st.className = "status-line err";
      box.classList.add("hidden");
      return;
    }
    st.textContent = "\u67e5\u8be2\u6210\u529f";
    st.className = "status-line ok";
    const exam = data.data;
    box.innerHTML = renderExamDetail(exam);
    box.classList.remove("hidden");
    if (exam && exam.examId) {
      await loadQuestionsFromExam(exam.examId, exam.title || "");
    }
  }

  async function queryExamByName() {
    if (!session || !session.token) return;
    const title = $("exam-query-name").value.trim();
    const st = $("exam-query-status");
    const box = $("exam-query-result");
    if (!title) {
      st.textContent = "\u8bf7\u8f93\u5165\u7ade\u8d5b\u540d\u79f0";
      st.className = "status-line err";
      box.classList.add("hidden");
      return;
    }
    st.textContent = "\u67e5\u8be2\u4e2d...";
    st.className = "status-line muted";
    const { ok, data, kicked } = await requestWithAuth(
      `/exam/getExamByName?title=${encodeURIComponent(title)}`
    );
    if (kicked) return;
    if (!ok || data.code !== 1000 || !data.data) {
      st.textContent = data.msg || "\u67e5\u8be2\u5931\u8d25";
      st.className = "status-line err";
      box.classList.add("hidden");
      return;
    }
    st.textContent = "\u67e5\u8be2\u6210\u529f";
    st.className = "status-line ok";
    const exam = data.data;
    box.innerHTML = renderExamDetail(exam);
    box.classList.remove("hidden");
    if (exam && exam.examId) {
      await loadQuestionsFromExam(exam.examId, exam.title || "");
    }
  }

  function parseQuestionIdList(raw) {
    const tokens = String(raw || "")
      .split(/[\n,\s]+/)
      .map((x) => x.trim())
      .filter((x) => x.length > 0);
    const seen = new Set();
    const ids = [];
    for (const t of tokens) {
      if (!/^\d+$/.test(t)) continue;
      if (!seen.has(t)) {
        seen.add(t);
        // Keep long id as string to avoid JS Number precision loss.
        ids.push(t);
      }
    }
    return ids;
  }

  function buildExamCreateBody() {
    const title = $("exam-new-title").value.trim();
    const startTimeRaw = $("exam-new-start-time").value;
    const endTimeRaw = $("exam-new-end-time").value;
    const status = parseInt($("exam-new-status").value, 10);
    const qids = parseQuestionIdList($("exam-new-question-ids").value);
    const startTime = startTimeRaw ? `${startTimeRaw}:00` : null;
    const endTime = endTimeRaw ? `${endTimeRaw}:00` : null;
    return {
      exam: {
        title,
        startTime,
        endTime,
        status,
      },
      questionIdList: qids,
    };
  }

  async function insertExam() {
    if (!session || !session.token) return;
    const st = $("exam-insert-status");
    const body = buildExamCreateBody();
    if (
      !body.exam.title ||
      Number.isNaN(body.exam.status) ||
      !body.exam.startTime ||
      !body.exam.endTime ||
      !Array.isArray(body.questionIdList) ||
      body.questionIdList.length === 0
    ) {
      st.textContent = "\u8bf7\u586b\u5199\u7ade\u8d5b\u57fa\u672c\u4fe1\u606f\u5e76\u8f93\u5165\u81f3\u5c11\u4e00\u4e2a\u9898\u76eeID";
      st.className = "status-line err";
      return;
    }
    st.textContent = "\u63d0\u4ea4\u4e2d...";
    st.className = "status-line muted";
    const { ok, data, kicked } = await requestWithAuth("/exam/addExam", {
      method: "POST",
      body,
    });
    if (kicked) return;
    if (!ok || data.code !== 1000) {
      st.textContent = data.msg || "\u65b0\u589e\u5931\u8d25";
      st.className = "status-line err";
      return;
    }
    st.textContent = `\u65b0\u589e\u6210\u529f\uff0c\u7ade\u8d5bID\uff1a${data.data}`;
    st.className = "status-line ok";
    $("exam-new-title").value = "";
    $("exam-new-start-time").value = "";
    $("exam-new-end-time").value = "";
    $("exam-new-status").value = "0";
    $("exam-new-question-ids").value = "";
    examPage.current = 1;
    await loadExamList();
  }

  async function deleteExamById(examId, title) {
    const { ok, data, kicked } = await requestWithAuth(
      `/exam/deleteExamById?examId=${encodeURIComponent(examId)}`,
      { method: "DELETE" }
    );
    if (kicked) return;
    if (!ok || data.code !== 1000) {
      $("exam-list-status").textContent = data.msg || "\u5220\u9664\u5931\u8d25";
      $("exam-list-status").className = "status-line err";
      return;
    }
    $("exam-list-status").textContent = `\u5220\u9664\u6210\u529f\uff1a${title || examId}`;
    $("exam-list-status").className = "status-line ok";
    await loadExamList();
  }

  async function loadQuestionsFromExam(examId, examTitle) {
    const st = $("exam-question-status");
    const box = $("exam-question-result");
    st.textContent = `\u6b63\u5728\u52a0\u8f7d\u300c${examTitle || examId}\u300d\u9898\u76ee...`;
    st.className = "status-line muted";
    const { ok, data, kicked } = await requestWithAuth(
      `/question/getQuestionFromExam?examId=${encodeURIComponent(examId)}`
    );
    if (kicked) return;
    if (!ok || data.code !== 1000 || !Array.isArray(data.data)) {
      st.textContent = data.msg || "\u52a0\u8f7d\u7ade\u8d5b\u9898\u76ee\u5931\u8d25";
      st.className = "status-line err";
      box.classList.add("hidden");
      return;
    }
    const list = data.data;
    st.textContent = `\u5171 ${list.length} \u9053\u9898`;
    st.className = "status-line ok";
    box.innerHTML = renderQuestionReadonlyTable(list);
    box.classList.remove("hidden");
    const dSt = $("exam-question-detail-status");
    const dBox = $("exam-question-detail-result");
    dSt.textContent = "\u8bf7\u70b9\u51fb\u4e0a\u65b9\u9898\u76ee\u884c\u67e5\u770b\u8be6\u60c5";
    dSt.className = "status-line muted";
    dBox.classList.add("hidden");
    dBox.innerHTML = "";
  }

  async function loadExamQuestionDetailById(questionId) {
    const st = $("exam-question-detail-status");
    const box = $("exam-question-detail-result");
    st.textContent = "\u8be6\u60c5\u52a0\u8f7d\u4e2d...";
    st.className = "status-line muted";
    const { ok, data, kicked } = await requestWithAuth(
      `/question/detail?questionId=${encodeURIComponent(questionId)}`
    );
    if (kicked) return;
    if (!ok || data.code !== 1000 || !data.data) {
      st.textContent = data.msg || "\u8be6\u60c5\u52a0\u8f7d\u5931\u8d25";
      st.className = "status-line err";
      box.classList.add("hidden");
      return;
    }
    const q = data.data;
    st.textContent = `\u9898\u76ee ${questionId} \u8be6\u60c5\u5df2\u52a0\u8f7d`;
    st.className = "status-line ok";
    box.innerHTML = `
      <div class="question-detail-block">
        <h4>\u9898\u76ee\u5185\u5bb9</h4>
        <pre class="question-detail-pre">${escapeHtml(q.content || "\u2014")}</pre>
      </div>
      <div class="question-detail-block">
        <h4>\u6d4b\u8bd5\u7528\u4f8b</h4>
        <pre class="question-detail-pre">${escapeHtml(q.questionCase || "\u2014")}</pre>
      </div>
      <div class="question-detail-block">
        <h4>\u9ed8\u8ba4\u4ee3\u7801</h4>
        <pre class="question-detail-pre">${escapeHtml(q.defaultCode || "\u2014")}</pre>
      </div>
      <div class="question-detail-block">
        <h4>Main \u65b9\u6cd5</h4>
        <pre class="question-detail-pre">${escapeHtml(q.mainMethod || "\u2014")}</pre>
      </div>
    `;
    box.classList.remove("hidden");
    box.scrollIntoView({ behavior: "smooth", block: "start" });
  }

  function onExamQuestionResultClick(e) {
    const row = e.target.closest("tr[data-question-id]");
    if (!row) return;
    document
      .querySelectorAll("#exam-question-result tr.row-clickable")
      .forEach((tr) => tr.classList.remove("row-selected"));
    row.classList.add("row-selected");
    const qid = row.getAttribute("data-question-id");
    if (!qid) return;
    loadExamQuestionDetailById(qid);
  }

  function onExamTableClick(e) {
    const btn = e.target.closest(".btn-delete-exam-row");
    if (btn) {
      const tr = btn.closest("tr");
      if (!tr) return;
      const examId = tr.getAttribute("data-exam-id");
      const title = tr.getAttribute("data-exam-title") || "";
      if (!examId) return;
      showConfirmDelete(
        `\u786e\u5b9a\u5220\u9664\u7ade\u8d5b\u300c${title || examId}\u300d\u5417\uff1f\u8be5\u7ade\u8d5b\u5173\u8054\u9898\u76ee\u5173\u7cfb\u4e5f\u4f1a\u88ab\u5220\u9664\u3002`,
        () => deleteExamById(examId, title)
      );
      return;
    }
    const row = e.target.closest("tr[data-exam-id]");
    if (!row) return;
    const examId = row.getAttribute("data-exam-id");
    const title = row.getAttribute("data-exam-title") || "";
    if (!examId) return;
    loadQuestionsFromExam(examId, title);
  }

  function initExamQueryTabs() {
    const tabs = document.querySelectorAll("#panel-contest .query-tabs .tab");
    tabs.forEach((tab) => {
      tab.addEventListener("click", () => {
        tabs.forEach((t) => t.classList.remove("active"));
        tab.classList.add("active");
        const key = tab.getAttribute("data-tab");
        $("exam-query-pane-id").classList.toggle("hidden", key !== "exam-by-id");
        $("exam-query-pane-name").classList.toggle("hidden", key !== "exam-by-name");
        $("exam-query-status").textContent = "";
      });
    });
  }

  function renderUserDetail(u) {
    const pwd =
      u.password != null && String(u.password).length > 0
        ? "********\uff08\u540e\u7aef\u5df2\u8131\u654f\uff09"
        : "\u2014";
    return `
      <dl class="detail-dl">
        <div class="detail-row"><dt>\u7528\u6237 ID</dt><dd><code>${escapeHtml(
          String(u.userId ?? "")
        )}</code></dd></div>
        <div class="detail-row"><dt>\u8d26\u53f7</dt><dd>${escapeHtml(
          u.userAccount || ""
        )}</dd></div>
        <div class="detail-row"><dt>\u6635\u79f0</dt><dd>${escapeHtml(
          u.nickName || ""
        )}</dd></div>
        <div class="detail-row"><dt>\u5bc6\u7801</dt><dd class="muted">${pwd}</dd></div>
        <div class="detail-row"><dt>\u521b\u5efa\u65f6\u95f4</dt><dd>${escapeHtml(
          String(u.createTime ?? "\u2014")
        )}</dd></div>
        <div class="detail-row"><dt>\u66f4\u65b0\u65f6\u95f4</dt><dd>${escapeHtml(
          String(u.updateTime ?? "\u2014")
        )}</dd></div>
      </dl>`;
  }

  async function queryById() {
    if (!session || !session.token) return;
    const st = $("query-status");
    const raw = $("query-by-id").value.trim();
    const box = $("query-result");
    // Keep ID as string to avoid JS Number precision loss.
    if (!raw || !/^\d+$/.test(raw)) {
      st.textContent = "\u8bf7\u8f93\u5165\u5408\u6cd5\u7528\u6237 ID";
      st.className = "status-line err";
      box.classList.add("hidden");
      return;
    }
    st.textContent = "\u67e5\u8be2\u4e2d...";
    st.className = "status-line muted";
    const { ok, data, kicked } = await requestWithAuth(
      `/sysUser/findUserById?id=${encodeURIComponent(raw)}`
    );
    if (kicked) return;
    if (!ok || data.code !== 1000 || !data.data) {
      st.textContent = data.msg || "\u67e5\u8be2\u5931\u8d25";
      st.className = "status-line err";
      box.classList.add("hidden");
      return;
    }
    st.textContent = "\u67e5\u8be2\u6210\u529f";
    st.className = "status-line ok";
    box.innerHTML = renderUserDetail(data.data);
    box.classList.remove("hidden");
  }

  async function queryByAccount() {
    if (!session || !session.token) return;
    const st = $("query-status");
    const acc = $("query-by-account").value.trim();
    const box = $("query-result");
    if (!acc) {
      st.textContent = "\u8bf7\u8f93\u5165\u7528\u6237\u8d26\u53f7";
      st.className = "status-line err";
      box.classList.add("hidden");
      return;
    }
    st.textContent = "\u67e5\u8be2\u4e2d...";
    st.className = "status-line muted";
    const { ok, data, kicked } = await requestWithAuth(
      `/sysUser/findUserByUserAccount?userAccount=${encodeURIComponent(acc)}`
    );
    if (kicked) return;
    if (!ok || data.code !== 1000 || !data.data) {
      st.textContent = data.msg || "\u67e5\u8be2\u5931\u8d25";
      st.className = "status-line err";
      box.classList.add("hidden");
      return;
    }
    st.textContent = "\u67e5\u8be2\u6210\u529f";
    st.className = "status-line ok";
    box.innerHTML = renderUserDetail(data.data);
    box.classList.remove("hidden");
  }

  async function insertUser() {
    if (!session || !session.token) return;
    const st = $("insert-status");
    const acc = $("new-account").value.trim();
    const pwd = $("new-password").value;
    const nick = $("new-nickname").value.trim();
    if (!acc || !pwd) {
      st.textContent = "\u8d26\u53f7\u548c\u5bc6\u7801\u4e0d\u80fd\u4e3a\u7a7a";
      st.className = "status-line err";
      return;
    }
    st.textContent = "\u63d0\u4ea4\u4e2d...";
    st.className = "status-line muted";
    const body = {
      userAccount: acc,
      password: pwd,
      nickName: nick || acc,
    };
    const { ok, data, kicked } = await requestWithAuth("/sysUser/insertUser", {
      method: "POST",
      body,
    });
    if (kicked) return;
    if (ok && data.code === 1000) {
      st.textContent = "\u65b0\u589e\u6210\u529f";
      st.className = "status-line ok";
      $("new-account").value = "";
      $("new-password").value = "";
      $("new-nickname").value = "";
      listPage.current = 1;
      loadUserList();
    } else {
      st.textContent = data.msg || "\u65b0\u589e\u5931\u8d25";
      st.className = "status-line err";
    }
  }

  async function deleteUserById(id, userAccount) {
    const { ok, data, kicked } = await requestWithAuth(
      `/sysUser/deleteUserById?id=${encodeURIComponent(id)}`,
      { method: "DELETE" }
    );
    if (kicked) return;
    if (ok && data.code === 1000) {
      $("list-status").textContent = `\u5220\u9664\u6210\u529f\uff1a${userAccount || id}`;
      $("list-status").className = "status-line ok";
      loadUserList();
    } else {
      $("list-status").textContent = data.msg || "\u5220\u9664\u5931\u8d25";
      $("list-status").className = "status-line err";
    }
  }

  function onTableClick(e) {
    const btn = e.target.closest(".btn-delete-row");
    if (!btn || btn.disabled) return;
    const tr = btn.closest("tr");
    if (!tr) return;
    const id = tr.getAttribute("data-user-id");
    const acc = tr.getAttribute("data-user-account") || "";
    if (!id) return;
    if (session.userId != null && String(session.userId) === id) {
      $("list-status").textContent = "\u4e0d\u80fd\u5220\u9664\u5f53\u524d\u767b\u5f55\u8d26\u53f7";
      $("list-status").className = "status-line err";
      return;
    }
    showConfirmDelete(
      `\u786e\u5b9a\u5220\u9664\u7528\u6237\u300c${acc || id}\u300d\u5417\uff1f\u6b64\u64cd\u4f5c\u4e0d\u53ef\u6062\u590d\u3002`,
      () => deleteUserById(id, acc)
    );
  }

  function initQueryTabs() {
    const tabs = document.querySelectorAll("#panel-sysuser .query-tabs .tab");
    tabs.forEach((tab) => {
      tab.addEventListener("click", () => {
        tabs.forEach((t) => t.classList.remove("active"));
        tab.classList.add("active");
        const key = tab.getAttribute("data-tab");
        $("query-pane-id").classList.toggle("hidden", key !== "by-id");
        $("query-pane-account").classList.toggle("hidden", key !== "by-account");
        $("query-status").textContent = "";
        $("query-status").className = "status-line muted";
      });
    });
  }

  function initProblemQueryTabs() {}

  function init() {
    $("apiBase").value = defaultApiBase();
    $("apiBase").addEventListener("change", () => apiBase());

    $("btn-login").addEventListener("click", doLogin);
    $("login-password").addEventListener("keydown", (e) => {
      if (e.key === "Enter") doLogin();
    });

    $("btn-logout").addEventListener("click", doLogout);
    $("btn-insert-user").addEventListener("click", insertUser);
    $("btn-refresh-users").addEventListener("click", () => loadUserList());
    $("btn-page-prev").addEventListener("click", () => {
      if (listPage.current > 1) {
        listPage.current -= 1;
        loadUserList();
      }
    });
    $("btn-page-next").addEventListener("click", () => {
      if (listPage.current < listPage.pages) {
        listPage.current += 1;
        loadUserList();
      }
    });

    $("btn-query-id").addEventListener("click", queryById);
    $("query-by-id").addEventListener("keydown", (e) => {
      if (e.key === "Enter") queryById();
    });
    $("btn-query-account").addEventListener("click", queryByAccount);
    $("query-by-account").addEventListener("keydown", (e) => {
      if (e.key === "Enter") queryByAccount();
    });

    $("user-table-wrap").addEventListener("click", onTableClick);
    $("question-table-wrap").addEventListener("click", onQuestionTableClick);
    $("question-query-result").addEventListener("click", onQuestionQueryResultClick);
    $("btn-refresh-questions").addEventListener("click", () => loadQuestionList());
    $("btn-question-page-prev").addEventListener("click", () => {
      if (questionPage.current > 1) {
        questionPage.current -= 1;
        loadQuestionList();
      }
    });
    $("btn-question-page-next").addEventListener("click", () => {
      if (questionPage.current < questionPage.pages) {
        questionPage.current += 1;
        loadQuestionList();
      }
    });
    $("btn-question-like-by-title").addEventListener(
      "click",
      queryQuestionListByTitleLike
    );
    $("btn-question-insert").addEventListener("click", insertQuestion);

    $("exam-table-wrap").addEventListener("click", onExamTableClick);
    $("exam-question-result").addEventListener("click", onExamQuestionResultClick);
    $("btn-refresh-exams").addEventListener("click", () => loadExamList());
    $("btn-exam-page-prev").addEventListener("click", () => {
      if (examPage.current > 1) {
        examPage.current -= 1;
        loadExamList();
      }
    });
    $("btn-exam-page-next").addEventListener("click", () => {
      if (examPage.current < examPage.pages) {
        examPage.current += 1;
        loadExamList();
      }
    });
    $("btn-exam-query-id").addEventListener("click", queryExamById);
    $("exam-query-id").addEventListener("keydown", (e) => {
      if (e.key === "Enter") queryExamById();
    });
    $("btn-exam-query-name").addEventListener("click", queryExamByName);
    $("exam-query-name").addEventListener("keydown", (e) => {
      if (e.key === "Enter") queryExamByName();
    });
    $("btn-exam-insert").addEventListener("click", insertExam);

    $("modal-ok").addEventListener("click", hideKickModal);
    $("modal-overlay").addEventListener("click", (e) => {
      if (e.target === $("modal-overlay")) hideKickModal();
    });

    $("confirm-cancel").addEventListener("click", hideConfirm);
    $("confirm-overlay").addEventListener("click", (e) => {
      if (e.target === $("confirm-overlay")) hideConfirm();
    });
    $("confirm-ok").addEventListener("click", async () => {
      const fn = pendingDelete;
      hideConfirm();
      if (typeof fn === "function") await fn();
    });

    initQueryTabs();
    initProblemQueryTabs();
    initExamQueryTabs();

    document.querySelectorAll(".sidebar-nav .nav-item").forEach((btn) => {
      btn.addEventListener("click", () => {
        const key = btn.getAttribute("data-panel");
        if (key) switchAdminPanel(key);
      });
    });

    const stored = loadStoredSession();
    if (stored) {
      saveSession(stored);
      showWorkspace();
      switchAdminPanel("sysuser");
    }
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();
