/**
 * avatar.js — 头像管理页（avatar.html）
 * 查看当前头像、上传新头像（STS 直传阿里云 OSS）。
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

  function fileExt(name) {
    var n = String(name || "");
    var idx = n.lastIndexOf(".");
    if (idx < 0 || idx === n.length - 1) return "";
    var ext = n.slice(idx).toLowerCase();
    return /^\.[a-z0-9]{1,10}$/.test(ext) ? ext : "";
  }

  function randomSuffix() {
    return Math.random().toString(36).slice(2, 10);
  }

  function buildObjectKey(prefix, fileName) {
    var base = String(prefix || "").replace(/\/+$/, "") + "/";
    var ext = fileExt(fileName);
    var d = new Date();
    return base + d.getFullYear() + "/" + (d.getMonth() + 1) + "/" + d.getDate() + "/" + Date.now() + "_" + randomSuffix() + ext;
  }

  function uploadToOssWithSts(stsData, file, objectKey) {
    if (!window.OSS) throw new Error("OSS SDK 未加载");
    var client = new window.OSS({
      region: stsData.region,
      bucket: stsData.bucketName,
      endpoint: "https://" + String(stsData.endpoint || "").replace(/^https?:\/\//, ""),
      accessKeyId: stsData.accessKeyId,
      accessKeySecret: stsData.accessKeySecret,
      stsToken: stsData.securityToken,
      secure: true,
      timeout: 120000,
    });
    return client.put(objectKey, file);
  }

  function renderAvatarPreview(objectKey, url) {
    var placeholder = document.getElementById("avatar-placeholder");
    var imgWrap = document.getElementById("avatar-img-wrap");
    var img = document.getElementById("avatar-display");
    var meta = document.getElementById("avatar-meta");
    var keyEl = document.getElementById("meta-key");
    var urlEl = document.getElementById("meta-url");

    if (placeholder) placeholder.classList.add("hidden");
    if (imgWrap) imgWrap.classList.remove("hidden");
    if (img) {
      img.src = url || "";
      if (url) img.classList.remove("hidden");
      else img.classList.add("hidden");
    }
    if (meta) meta.classList.remove("hidden");
    if (keyEl) keyEl.textContent = objectKey || "-";
    if (urlEl) {
      urlEl.textContent = url || "无可访问 URL";
      urlEl.href = url || "#";
    }
  }

  function resetAvatarPreview() {
    var placeholder = document.getElementById("avatar-placeholder");
    var imgWrap = document.getElementById("avatar-img-wrap");
    var meta = document.getElementById("avatar-meta");
    if (placeholder) placeholder.classList.remove("hidden");
    if (imgWrap) imgWrap.classList.add("hidden");
    if (meta) meta.classList.add("hidden");
  }

  // ── 查看头像 ────────────────────────────────────
  document.getElementById("btn-load-avatar") && document.getElementById("btn-load-avatar").addEventListener("click", function () {
    FriendApi.getMyAvatar().then(function (res) {
      var body = res.data;
      if (!okResult(body) || !body.data) {
        resetAvatarPreview();
        showToast((body && body.msg) || "加载头像失败");
        return;
      }
      renderAvatarPreview(body.data.objectKey || "", body.data.url || "");
      showToast("头像加载成功");
    }).catch(function () {
      resetAvatarPreview();
      showToast("网络错误");
    });
  });

  // ── 上传头像 ────────────────────────────────────
  document.getElementById("btn-upload-avatar") && document.getElementById("btn-upload-avatar").addEventListener("click", async function () {
    var fileInput = document.getElementById("avatar-file");
    var dirInput = document.getElementById("avatar-dir");
    var hint = document.getElementById("upload-hint");
    var file = fileInput && fileInput.files ? fileInput.files[0] : null;
    var dir = dirInput && dirInput.value ? dirInput.value.trim() : "avatar";

    if (!file) {
      if (hint) hint.textContent = "请先选择图片文件";
      showToast("请先选择图片");
      return;
    }

    if (hint) hint.textContent = "正在申请 STS 凭证…";
    var stsRes = await FriendApi.issueMyAvatarSts(dir);
    var stsBody = stsRes.data;
    if (!okResult(stsBody) || !stsBody.data) {
      if (hint) hint.textContent = (stsBody && stsBody.msg) || "申请 STS 失败";
      showToast((stsBody && stsBody.msg) || "申请 STS 失败");
      return;
    }

    var sts = stsBody.data;
    var objectKey = buildObjectKey(sts.objectKeyPrefix, file.name);

    if (hint) hint.textContent = "正在上传到 OSS…";
    try {
      await uploadToOssWithSts(sts, file, objectKey);
    } catch (e) {
      if (hint) hint.textContent = (e && e.message) || "OSS 上传失败";
      showToast((e && e.message) || "OSS 上传失败");
      return;
    }

    if (hint) hint.textContent = "正在保存头像…";
    var saveRes = await FriendApi.saveMyAvatarObjectKey(objectKey);
    var saveBody = saveRes.data;
    if (!okResult(saveBody)) {
      if (hint) hint.textContent = (saveBody && saveBody.msg) || "保存头像失败";
      showToast((saveBody && saveBody.msg) || "保存头像失败");
      return;
    }

    // 再拉一次拿标准 URL
    var latest = await FriendApi.getMyAvatar();
    if (okResult(latest && latest.data) && latest.data.data) {
      renderAvatarPreview(latest.data.data.objectKey || objectKey, latest.data.data.url || "");
    } else {
      renderAvatarPreview(objectKey, "");
    }

    if (hint) hint.textContent = "头像更新成功";
    showToast("头像上传并保存成功");
  });

  // ── 权限检查 ────────────────────────────────────
  function checkAuth() {
    var s = FriendApi.getSession();
    if (s && s.token) {
      document.getElementById("state-unauth") && document.getElementById("state-unauth").classList.add("hidden");
      if (document.getElementById("state-content")) document.getElementById("state-content").classList.remove("hidden");
      if (document.getElementById("link-login")) document.getElementById("link-login").classList.add("hidden");
      if (document.getElementById("btn-logout")) document.getElementById("btn-logout").classList.remove("hidden");
    } else {
      document.getElementById("state-unauth") && document.getElementById("state-unauth").classList.remove("hidden");
      if (document.getElementById("state-content")) document.getElementById("state-content").classList.add("hidden");
      if (document.getElementById("link-login")) document.getElementById("link-login").classList.remove("hidden");
      if (document.getElementById("btn-logout")) document.getElementById("btn-logout").classList.add("hidden");
    }
  }

  document.getElementById("btn-logout") && document.getElementById("btn-logout").addEventListener("click", function () {
    FriendApi.clearSession();
    FriendApi.setGuestMode(false);
    location.href = "user-login.html";
  });

  checkAuth();
})();
