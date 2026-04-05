/**
 * friend-api.js — 学员端 HTTP 封装
 * 所有请求经网关 http://localhost:9000，API 基址存于 localStorage oj_api_base。
 * 需登录接口自动携带 header: token；访客模式（sessionStorage oj_friend_mode=guest）不带 token。
 */
(function (global) {
  var STORAGE_API = "oj_api_base";
  var STORAGE_SESSION = "oj_friend_session";
  var STORAGE_GUEST = "oj_friend_mode";

  // ── 存储 ───────────────────────────────────────────────

  function getApiBase() {
    var v = localStorage.getItem(STORAGE_API);
    return (v && v.trim()) || "http://localhost:9000";
  }

  function setApiBase(url) {
    if (url && String(url).trim()) localStorage.setItem(STORAGE_API, String(url).trim());
  }

  function getSession() {
    try {
      var raw = sessionStorage.getItem(STORAGE_SESSION);
      if (!raw) return null;
      var s = JSON.parse(raw);
      if (s && typeof s === "object") return s;
    } catch (e) { /* ignore */ }
    return null;
  }

  function setSession(obj) {
    if (obj) sessionStorage.setItem(STORAGE_SESSION, JSON.stringify(obj));
    else sessionStorage.removeItem(STORAGE_SESSION);
  }

  function clearSession() {
    sessionStorage.removeItem(STORAGE_SESSION);
  }

  function isGuestMode() {
    return sessionStorage.getItem(STORAGE_GUEST) === "guest";
  }

  function setGuestMode(on) {
    if (on) sessionStorage.setItem(STORAGE_GUEST, "guest");
    else sessionStorage.removeItem(STORAGE_GUEST);
  }

  // ── 核心请求 ───────────────────────────────────────────

  function request(path, opts) {
    opts = opts || {};
    var method = opts.method || "GET";
    var headers = { "Content-Type": "application/json" };
    var wantToken = opts.withToken !== false;
    if (wantToken) {
      var s = getSession();
      if (s && s.token) headers.token = s.token;
    }
    var init = { method: method, headers: headers };
    if (opts.body !== undefined && opts.body !== null) {
      init.body = JSON.stringify(opts.body);
    }
    var url = getApiBase().replace(/\/$/, "") + path;
    return fetch(url, init).then(function (res) {
      return res.text().then(function (text) {
        var data = text ? JSON.parse(text) : null;
        return { ok: res.ok, status: res.status, data: data };
      }).catch(function () {
        return { ok: res.ok, status: res.status, data: null };
      });
    });
  }

  // multipart/form-data（文件上传）
  function requestForm(path, opts) {
    opts = opts || {};
    var headers = {};
    var wantToken = opts.withToken !== false;
    if (wantToken) {
      var s = getSession();
      if (s && s.token) headers.token = s.token;
    }
    var url = getApiBase().replace(/\/$/, "") + path;
    return fetch(url, { method: "POST", headers: headers, body: opts.formData })
      .then(function (res) {
        return res.text().then(function (text) {
          var data = text ? JSON.parse(text) : null;
          return { ok: res.ok, status: res.status, data: data };
        }).catch(function () {
          return { ok: res.ok, status: res.status, data: null };
        });
      });
  }

  // ── API 列表 ──────────────────────────────────────────

  var FriendApi = {
    // 存储
    getApiBase: getApiBase,
    setApiBase: setApiBase,
    getSession: getSession,
    setSession: setSession,
    clearSession: clearSession,
    isGuestMode: isGuestMode,
    setGuestMode: setGuestMode,

    // 请求
    request: request,
    requestForm: requestForm,

    // 健康检查
    ping: function () {
      return request("/ping", { withToken: false });
    },

    // ── 用户 ──────────────────────────────────────────
    register: function (body) {
      return request("/friend/user/register", { method: "POST", body: body, withToken: false });
    },
    loginByPhonePassword: function (body) {
      return request("/friend/user/loginByPhonePassword", { method: "POST", body: body, withToken: false });
    },
    loginByEmailPassword: function (body) {
      return request("/friend/user/loginByEmailPassword", { method: "POST", body: body, withToken: false });
    },
    sendEmailCode: function (body) {
      return request("/friend/user/sendEmailCode", { method: "POST", body: body, withToken: false });
    },
    loginByEmailCode: function (body) {
      return request("/friend/user/loginByEmailCode", { method: "POST", body: body, withToken: false });
    },
    logout: function (body) {
      return request("/friend/user/logout", { method: "POST", body: body, withToken: true });
    },
    getUserDetail: function (userId) {
      return request("/friend/user/detail?userId=" + encodeURIComponent(String(userId)), { withToken: true });
    },
    getCurrentUserDetail: function (opts) {
      // opts: { token, gatewayUserId } — 优先 token，gatewayUserId 兜底
      return request("/friend/user/me/detail", { withToken: false });
    },

    // ── 竞赛 ──────────────────────────────────────────
    listActiveExams: function () {
      return request("/friend/exam/list/active", { withToken: false });
    },
    listFinishedExams: function () {
      return request("/friend/exam/list/finished", { withToken: false });
    },
    getExamDetail: function (examId) {
      return request("/friend/exam/detail?examId=" + encodeURIComponent(String(examId)), { withToken: false });
    },
    listMyRegistrations: function () {
      return request("/friend/exam/my/registrations", { withToken: true });
    },
    registerExam: function (body) {
      return request("/friend/exam/registerExam", { method: "POST", body: body, withToken: true });
    },

    // ── 题目 ──────────────────────────────────────────
    questionList: function () {
      return request("/question/list", { withToken: false });
    },
    questionDetail: function (questionId) {
      return request("/question/detail?questionId=" + encodeURIComponent(String(questionId)), { withToken: false });
    },

    // ── 头像 ──────────────────────────────────────────
    issueMyAvatarSts: function (dir) {
      return request("/friend/user/avatar/sts", {
        method: "POST",
        body: { dir: dir || "" },
        withToken: true
      });
    },
    saveMyAvatarObjectKey: function (objectKey) {
      return request("/friend/user/avatar/object-key", {
        method: "PUT",
        body: { objectKey: String(objectKey || "").trim() },
        withToken: true
      });
    },
    getMyAvatar: function () {
      return request("/friend/user/avatar", { withToken: true });
    }
  };

  global.FriendApi = FriendApi;
})(typeof window !== "undefined" ? window : globalThis);
