/**
 * 学员工作台：/friend/exam/list/active、/finished、detail；/question/detail；我的报名。
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

  function okResult(body) {
    return body && body.code === 1000;
  }

  /** @param {object|null} body 接口 JSON 根对象 */
  function extractDataList(body) {
    if (!body || body.data == null) return [];
    const d = body.data;
    return Array.isArray(d) ? d : [];
  }

  function isGuest() {
    return FriendApi.isGuestMode();
  }

  function isLoggedIn() {
    const s = FriendApi.getSession();
    return !!(s && s.token);
  }

  function withTokenOpt() {
    return { withToken: !isGuest() };
  }

  function guardEntry() {
    if (isLoggedIn()) {
      FriendApi.setGuestMode(false);
      return;
    }
    if (isGuest()) return;
    location.replace("user-login.html");
  }

  function syncApiInput() {
    const el = document.getElementById("friend-api-base");
    if (el) el.value = FriendApi.getApiBase();
  }

  function syncSessionUi() {
    const label = document.getElementById("friend-session-label");
    const sub = document.getElementById("user-sub-badge");
    const linkLogin = document.getElementById("friend-link-login");
    const loggedBlock = document.getElementById("workspace-logged");
    const regBlock = document.getElementById("exam-register-block");
    const hint = document.getElementById("exam-panel-hint");

    if (isGuest()) {
      if (label) label.textContent = "访客模式（未登录）";
      if (sub) sub.textContent = "访客 · 公开竞赛";
      if (linkLogin) linkLogin.classList.remove("hidden");
      if (loggedBlock) loggedBlock.classList.add("hidden");
      if (regBlock) regBlock.classList.add("hidden");
      if (hint) hint.textContent = "未登录：列表与详情走网关白名单；无法报名。";
    } else if (isLoggedIn()) {
      const s = FriendApi.getSession();
      const uid = s.userId != null && s.userId !== "" ? s.userId : "（验证码登录未带 userId）";
      if (label) label.textContent = "已登录 · userId: " + uid;
      if (sub) sub.textContent = "学员中心 · 已登录";
      if (linkLogin) linkLogin.classList.add("hidden");
      if (loggedBlock) loggedBlock.classList.remove("hidden");
      if (regBlock) regBlock.classList.remove("hidden");
      if (hint) hint.textContent =
        "进行中列表 → /friend/exam/list/active；已结束 → /friend/exam/list/finished；点击竞赛 → /friend/exam/detail。";
    }
  }

  function escapeHtml(s) {
    if (s == null) return "";
    return String(s)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  function hasText(s) {
    return s != null && String(s).trim() !== "";
  }

  function nl2brEscaped(text) {
    const t = String(text).replace(/\r\n/g, "\n").replace(/\r/g, "\n");
    return escapeHtml(t).replace(/\n/g, "<br />");
  }

  /**
   * 将 /question/detail 返回的 data 渲染为可读页面（非 JSON）。
   * @param {object} vo QuestionDetailVo
   */
  function renderQuestionDetailHtml(vo) {
    if (!vo || typeof vo !== "object") {
      return '<p class="hint user-q-empty">暂无题目数据</p>';
    }
    const qid = vo.questionId != null ? String(vo.questionId) : "—";
    const title = vo.title || "未命名题目";
    const diffLabel = (vo.difficultyLabel || "unknown").toLowerCase();
    let diffClass = "user-q-diff--unknown";
    if (diffLabel === "easy") diffClass = "user-q-diff--easy";
    else if (diffLabel === "medium") diffClass = "user-q-diff--medium";
    else if (diffLabel === "hard") diffClass = "user-q-diff--hard";

    let chips = "";
    if (vo.timeLimit != null && vo.timeLimit !== "") {
      chips +=
        '<span class="user-q-chip">时间 <strong>' +
        escapeHtml(String(vo.timeLimit)) +
        "</strong> ms</span>";
    }
    if (vo.spaceLimit != null && vo.spaceLimit !== "") {
      chips +=
        '<span class="user-q-chip">空间 <strong>' +
        escapeHtml(String(vo.spaceLimit)) +
        "</strong> KB</span>";
    }

    let html = '<article class="user-q-view">';
    html += '<header class="user-q-view-head">';
    html += '<div class="user-q-view-title-row">';
    html += '<h2 class="user-q-view-title">' + escapeHtml(title) + "</h2>";
    html +=
      '<span class="user-q-diff ' +
      diffClass +
      '">' +
      escapeHtml(vo.difficultyLabel || "unknown") +
      "</span>";
    html += "</div>";
    html +=
      '<p class="user-q-view-meta">题目编号 <span class="user-q-id-tag">' +
      escapeHtml(qid) +
      "</span></p>";
    if (chips) html += '<div class="user-q-chips">' + chips + "</div>";
    html += "</header>";

    if (hasText(vo.content)) {
      html += '<section class="user-q-section">';
      html += '<h3 class="user-q-section-title">题目描述</h3>';
      html += '<div class="user-q-prose">' + nl2brEscaped(vo.content) + "</div>";
      html += "</section>";
    }

    if (hasText(vo.questionCase)) {
      html += '<section class="user-q-section">';
      html += '<h3 class="user-q-section-title">样例与测试说明</h3>';
      html +=
        '<div class="user-q-code-wrap"><pre class="user-q-codeblock"><code>' +
        escapeHtml(vo.questionCase) +
        "</code></pre></div>";
      html += "</section>";
    }

    if (hasText(vo.defaultCode)) {
      html += '<section class="user-q-section">';
      html += '<h3 class="user-q-section-title">默认代码模板</h3>';
      html +=
        '<div class="user-q-code-wrap"><pre class="user-q-codeblock"><code>' +
        escapeHtml(vo.defaultCode) +
        "</code></pre></div>";
      html += "</section>";
    }

    if (hasText(vo.mainMethod)) {
      html += '<section class="user-q-section">';
      html += '<h3 class="user-q-section-title">主方法 / 入口签名</h3>';
      html +=
        '<div class="user-q-code-wrap"><pre class="user-q-codeblock user-q-codeblock--single"><code>' +
        escapeHtml(vo.mainMethod) +
        "</code></pre></div>";
      html += "</section>";
    }

    if (
      !hasText(vo.content) &&
      !hasText(vo.questionCase) &&
      !hasText(vo.defaultCode) &&
      !hasText(vo.mainMethod)
    ) {
      html +=
        '<p class="hint user-q-empty">该题目暂无正文描述，请联系管理员完善题库。</p>';
    }

    html += "</article>";
    return html;
  }

  function renderExamBars(container, exams, emptyText) {
    if (!container) return;
    container.innerHTML = "";
    if (!exams || exams.length === 0) {
      const p = document.createElement("p");
      p.className = "hint user-exam-empty";
      p.textContent = emptyText || "暂无";
      container.appendChild(p);
      return;
    }
    exams.forEach((ex) => {
      const id = ex.examId != null ? String(ex.examId) : "";
      const title = ex.title || "（无标题）";
      const status = ex.status || "";
      const btn = document.createElement("button");
      btn.type = "button";
      btn.className = "user-exam-bar";
      btn.setAttribute("data-exam-id", id);
      btn.innerHTML =
        '<span class="user-exam-bar-title">' +
        escapeHtml(title) +
        "</span>" +
        '<span class="user-exam-bar-meta">#' +
        escapeHtml(id) +
        " · " +
        escapeHtml(status) +
        "</span>";
      container.appendChild(btn);
    });
  }

  function setSelectedExamBars(examId) {
    document.querySelectorAll(".user-exam-bar[data-exam-id]").forEach((b) => {
      const id = b.getAttribute("data-exam-id");
      b.classList.toggle("is-selected", id === examId);
    });
  }

  function renderQuestionBars(questionIds) {
    const ids = Array.isArray(questionIds) ? questionIds : [];
    if (ids.length === 0) {
      return '<p class="hint user-exam-empty">本场竞赛暂无关联题目 id（后端未配置或缓存未建）。</p>';
    }
    return (
      '<div class="user-q-bar-list">' +
      ids
        .map((qid, i) => {
          const idStr = String(qid);
          return (
            '<button type="button" class="user-q-bar" data-question-id="' +
            escapeHtml(idStr) +
            '">' +
            '<span class="user-q-bar-title">题目 ' +
            (i + 1) +
            "</span>" +
            '<span class="user-q-bar-meta">questionId=' +
            escapeHtml(idStr) +
            "</span></button>"
          );
        })
        .join("") +
      "</div>"
    );
  }

  async function loadExamDetail(examId) {
    const panel = document.getElementById("exam-detail-panel");
    const empty = document.getElementById("exam-detail-empty");
    const qWrap = document.getElementById("question-detail-wrap");
    const qRoot = document.getElementById("question-detail-root");
    if (!panel || !empty) return;
    if (!examId) return;
    setSelectedExamBars(examId);
    panel.classList.remove("hidden");
    empty.classList.add("hidden");
    if (qWrap) qWrap.classList.add("hidden");
    if (qRoot) {
      qRoot.innerHTML = '<p class="hint">点击上方题目长条查看题干与代码。</p>';
    }
    panel.innerHTML = '<p class="hint user-exam-detail-loading">正在请求 /friend/exam/detail …</p>';

    const res = await FriendApi.getExamDetail(examId, withTokenOpt());
    const body = res.data;
    if (body == null) {
      panel.innerHTML =
        '<p class="status-line err">无法解析响应（HTTP ' +
        res.status +
        "）。常见原因：网关未启动、路由不到 oj-friend、或返回了非 JSON（如 502 页面）。请检查网关地址 <code>" +
        escapeHtml(FriendApi.getApiBase()) +
        "</code> 与 Network 面板。</p>";
      showToast("详情请求异常 HTTP " + res.status);
      return;
    }
    if (!okResult(body) || body.data == null) {
      const hint =
        (body.msg || "加载失败") +
        (body.code != null ? "（业务 code: " + body.code + "）" : "");
      panel.innerHTML =
        '<p class="status-line err">' +
        escapeHtml(hint) +
        "</p>" +
        '<p class="hint user-exam-err-hint">若 code 为 3003：库中无该 exam；若 3001：登录态失效。清过 Redis 后请刷新列表再点。</p>';
      showToast(body.msg || "加载失败");
      return;
    }
    const d = body.data;
    const qBlock = renderQuestionBars(d.questionIds);
    panel.innerHTML =
      '<div class="user-exam-detail-head">' +
      "<h4>" +
      escapeHtml(d.title || "竞赛") +
      "</h4>" +
      '<span class="user-exam-detail-badge">' +
      escapeHtml(d.status || "") +
      "</span></div>" +
      '<dl class="user-exam-detail-dl">' +
      "<dt>竞赛编号</dt><dd>" +
      escapeHtml(d.examId != null ? String(d.examId) : "—") +
      "</dd>" +
      "<dt>开始时间</dt><dd>" +
      escapeHtml(fmtTime(d.startTime)) +
      "</dd>" +
      "<dt>结束时间</dt><dd>" +
      escapeHtml(fmtTime(d.endTime)) +
      "</dd>" +
      "</dl>" +
      '<div class="user-exam-questions"><h5 class="user-exam-q-head">题目</h5>' +
      qBlock +
      "</div>";
  }

  /**
   * 点击题目长条：请求 GET {网关}/question/detail?questionId= ，再渲染 renderQuestionDetailHtml。
   * @param {string} questionId data-question-id 上的字符串（雪花 id 勿用 Number）
   */
  async function loadQuestionDetail(questionId) {
    const wrap = document.getElementById("question-detail-wrap");
    const root = document.getElementById("question-detail-root");
    if (!root) return;
    if (wrap) {
      wrap.classList.remove("hidden");
      requestAnimationFrame(() => {
        try {
          wrap.scrollIntoView({ behavior: "smooth", block: "nearest" });
        } catch {
          wrap.scrollIntoView();
        }
      });
    }
    root.innerHTML =
      '<div class="user-q-loading card-elevated"><p class="hint">正在加载题目内容…</p><p class="hint user-q-req-hint">GET /question/detail?questionId=' +
      escapeHtml(String(questionId)) +
      "</p></div>";

    const res = await FriendApi.questionDetail(questionId, { withToken: false });
    const body = res.data;
    if (body == null) {
      root.innerHTML =
        '<div class="user-q-error card-elevated"><p class="status-line err">无法解析响应（HTTP ' +
        res.status +
        "）</p></div>";
      showToast("题目请求异常 HTTP " + res.status);
      return;
    }
    if (!okResult(body) || body.data == null) {
      root.innerHTML =
        '<div class="user-q-error card-elevated"><p class="status-line err">' +
        escapeHtml(body.msg || "加载失败") +
        (body.code != null ? "（code: " + body.code + "）" : "") +
        "</p></div>";
      showToast(body.msg || "题目详情请求失败");
      return;
    }
    root.innerHTML = renderQuestionDetailHtml(body.data);
  }

  function fmtTime(v) {
    if (v == null || v === "") return "—";
    if (typeof v === "string") return v;
    try {
      return new Date(v).toLocaleString("zh-CN");
    } catch {
      return String(v);
    }
  }

  function parseObjectKeyFromUrl(url) {
    if (!url) return "";
    try {
      const u = new URL(String(url));
      const p = u.pathname || "";
      return p.startsWith("/") ? p.slice(1) : p;
    } catch {
      return "";
    }
  }

  function fileExt(name) {
    const n = String(name || "");
    const idx = n.lastIndexOf(".");
    if (idx < 0 || idx === n.length - 1) return "";
    const ext = n.slice(idx).toLowerCase();
    if (!/^\.[a-z0-9]{1,10}$/.test(ext)) return "";
    return ext;
  }

  function randomSuffix() {
    return Math.random().toString(36).slice(2, 10);
  }

  function buildObjectKey(prefix, fileName) {
    const base = String(prefix || "").replace(/\/+$/, "") + "/";
    const ext = fileExt(fileName);
    const d = new Date();
    const y = d.getFullYear();
    const m = d.getMonth() + 1;
    const day = d.getDate();
    return base + y + "/" + m + "/" + day + "/" + Date.now() + "_" + randomSuffix() + ext;
  }

  async function uploadToOssWithSts(stsData, file, objectKey) {
    if (!window.OSS) {
      throw new Error("OSS SDK 未加载");
    }
    const client = new window.OSS({
      region: stsData.region,
      bucket: stsData.bucketName,
      endpoint: "https://" + String(stsData.endpoint || "").replace(/^https?:\/\//, ""),
      accessKeyId: stsData.accessKeyId,
      accessKeySecret: stsData.accessKeySecret,
      stsToken: stsData.securityToken,
      secure: true,
      timeout: 120000,
    });
    await client.put(objectKey, file);
    return objectKey;
  }

  function renderAvatarPreview(objectKey, url) {
    const box = document.getElementById("avatar-preview-box");
    const img = document.getElementById("avatar-preview-img");
    const keyText = document.getElementById("avatar-object-key-text");
    const link = document.getElementById("avatar-url-link");
    if (!box || !img || !keyText || !link) return;
    const key = objectKey || "";
    const finalUrl = url || "";
    keyText.textContent = key || "-";
    link.textContent = finalUrl || "无可访问 URL";
    link.href = finalUrl || "#";
    if (finalUrl) {
      img.src = finalUrl;
      img.classList.remove("hidden");
    } else {
      img.removeAttribute("src");
      img.classList.add("hidden");
    }
    box.classList.remove("hidden");
  }

  /** 分别请求 active / finished，避免混用一条接口 */
  async function loadExamLists() {
    const wt = withTokenOpt();
    const activeEl = document.getElementById("exam-bars-active");
    const finishedEl = document.getElementById("exam-bars-finished");

    const activeRes = await FriendApi.listActiveExams(wt);
    const activeBody = activeRes.data;
    const activeList = extractDataList(activeBody);
    if (okResult(activeBody)) {
      renderExamBars(activeEl, activeList, "暂无进行中的竞赛");
    } else {
      renderExamBars(activeEl, [], activeBody?.msg || "加载失败");
      showToast(activeBody?.msg || "GET /friend/exam/list/active 失败");
    }

    const finishedRes = await FriendApi.listFinishedExams(wt);
    const finishedBody = finishedRes.data;
    const finishedList = extractDataList(finishedBody);
    if (okResult(finishedBody)) {
      renderExamBars(finishedEl, finishedList, "暂无已结束竞赛");
    } else {
      renderExamBars(finishedEl, [], finishedBody?.msg || "加载失败");
      showToast(finishedBody?.msg || "GET /friend/exam/list/finished 失败");
    }
  }

  async function loadMyRegistrations() {
    const box = document.getElementById("my-reg-bars");
    if (!box) return;
    box.innerHTML = '<p class="hint user-exam-empty">加载中…</p>';
    const { data: body } = await FriendApi.listMyRegistrations();
    const list = extractDataList(body);
    if (okResult(body)) {
      renderExamBars(box, list, "暂无报名记录");
    } else {
      renderExamBars(box, [], body?.msg || "请求失败");
      showToast(body?.msg || "GET /friend/exam/my/registrations 失败");
    }
  }

  guardEntry();

  document.body.addEventListener("click", (e) => {
    const qBar = e.target.closest(".user-q-bar[data-question-id]");
    if (qBar) {
      const qid = qBar.getAttribute("data-question-id");
      if (qid) loadQuestionDetail(qid);
      return;
    }
    const exBar = e.target.closest(".user-exam-bar[data-exam-id]");
    if (exBar) {
      const eid = exBar.getAttribute("data-exam-id");
      if (eid) loadExamDetail(eid);
    }
  });

  document.getElementById("friend-btn-save-api")?.addEventListener("click", () => {
    const v = document.getElementById("friend-api-base")?.value?.trim();
    if (v) FriendApi.setApiBase(v);
    showToast("已保存网关地址: " + FriendApi.getApiBase());
    loadExamLists();
    if (isLoggedIn()) loadMyRegistrations();
  });

  document.getElementById("friend-btn-ping")?.addEventListener("click", async () => {
    const out = document.getElementById("friend-ping-result");
    const { ok, status, data } = await FriendApi.ping();
    if (out) {
      out.textContent = ok
        ? typeof data === "string"
          ? data
          : JSON.stringify(data)
        : "HTTP " + status + "（网关可能未路由 /ping，可忽略）";
    }
  });

  document.getElementById("friend-btn-refresh-my-reg")?.addEventListener("click", () => {
    loadMyRegistrations();
  });

  document.getElementById("friend-btn-register-exam")?.addEventListener("click", async () => {
    const raw = document.getElementById("exam-register-id")?.value?.trim();
    const hint = document.getElementById("friend-register-hint");
    // 雪花 examId 超过 JS Number 安全整数，禁止 Number(raw)，否则精度丢失 → 后端 selectById 为空 →「资源不存在」
    if (!raw || !/^\d+$/.test(raw)) {
      if (hint) hint.textContent = "请填写竞赛 examId（纯数字字符串）";
      showToast("请填写有效 examId");
      return;
    }
    const { data: body } = await FriendApi.registerExam({ examId: raw });
    if (hint) {
      hint.textContent = okResult(body)
        ? "报名成功，可刷新「我的报名」列表。"
        : body?.msg || "报名失败";
    }
    if (okResult(body)) {
      showToast("报名成功");
      loadMyRegistrations();
    } else showToast(body?.msg || "报名失败");
  });

  document.getElementById("friend-btn-logout")?.addEventListener("click", () => {
    FriendApi.clearSession();
    FriendApi.setGuestMode(false);
    location.href = "user-login.html";
  });

  document.getElementById("friend-btn-upload-avatar")?.addEventListener("click", async () => {
    const hint = document.getElementById("avatar-action-hint");
    const fileInput = document.getElementById("avatar-file-input");
    const dirInput = document.getElementById("avatar-dir-input");
    const file = fileInput && fileInput.files ? fileInput.files[0] : null;
    const dir = dirInput?.value?.trim() || "avatar";
    if (!file) {
      if (hint) hint.textContent = "请先选择图片文件";
      showToast("请先选择图片");
      return;
    }
    if (hint) hint.textContent = "正在申请 STS...";
    const stsRes = await FriendApi.issueMyAvatarSts(dir);
    const stsBody = stsRes.data;
    if (!okResult(stsBody) || !stsBody?.data) {
      const msg = stsBody?.msg || "申请 STS 失败";
      if (hint) hint.textContent = msg;
      showToast(msg);
      return;
    }
    const sts = stsBody.data;
    const objectKey = buildObjectKey(sts.objectKeyPrefix, file.name);
    if (hint) hint.textContent = "正在使用 STS 直传 OSS...";
    try {
      await uploadToOssWithSts(sts, file, objectKey);
    } catch (e) {
      const msg = e && e.message ? e.message : "OSS 上传失败";
      if (hint) hint.textContent = msg;
      showToast(msg);
      return;
    }
    if (hint) hint.textContent = "上传成功，正在回传 objectKey...";
    const saveRes = await FriendApi.saveMyAvatarObjectKey(objectKey);
    const saveBody = saveRes.data;
    if (!okResult(saveBody)) {
      const msg = saveBody?.msg || "保存头像失败";
      if (hint) hint.textContent = msg;
      showToast(msg);
      return;
    }
    const savedKey = saveBody?.data?.objectKey || objectKey;
    renderAvatarPreview(savedKey, "");
    // 再拉一次后端，拿标准拼接 URL。
    const latest = await FriendApi.getMyAvatar();
    if (okResult(latest?.data) && latest.data?.data) {
      renderAvatarPreview(latest.data.data.objectKey || savedKey, latest.data.data.url || "");
    }
    if (hint) hint.textContent = "头像已更新";
    showToast("头像 STS 上传并保存成功");
  });

  document.getElementById("friend-btn-load-avatar")?.addEventListener("click", async () => {
    const hint = document.getElementById("avatar-action-hint");
    if (hint) hint.textContent = "正在加载当前头像...";
    const { data: body } = await FriendApi.getMyAvatar();
    if (!okResult(body) || !body?.data) {
      const msg = body?.msg || "加载头像失败";
      if (hint) hint.textContent = msg;
      showToast(msg);
      return;
    }
    renderAvatarPreview(body.data.objectKey || "", body.data.url || "");
    if (hint) hint.textContent = "头像信息已加载";
    showToast("头像加载成功");
  });

  document.querySelectorAll(".user-feature-card[data-placeholder]").forEach((card) => {
    card.addEventListener("click", () => {
      const name = card.getAttribute("data-placeholder") || "该功能";
      showToast("「" + name + "」尚未对接后端");
    });
  });

  syncApiInput();
  syncSessionUi();
  loadExamLists();
  if (isLoggedIn()) loadMyRegistrations();
})();
