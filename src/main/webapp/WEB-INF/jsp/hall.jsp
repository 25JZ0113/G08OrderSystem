<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%-- H-01 席状況画面(ホール端末・ログインなし) --%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="ja">
<head>
<%@ include file="common/head.jspf" %>
<%-- 30 秒ごとの自動再表示は、ダイアログを開いている間は止めたいので下の script で行う --%>
<title>席状況 | 喫茶 生成り</title>
</head>
<body>
<header class="app-header">
  <p class="app-title">
    <span class="app-shop">喫茶 生成り</span>
    <span class="app-screen">席状況</span>
  </p>
  <div class="app-header-row">
    <p class="table-info">ホール端末</p>
    <div class="header-actions">
      <%@ include file="common/adminMenuButton.jspf" %>
      <%-- 管理者メニューから開いたときは【管理者メニュー】で戻るので、入口ページへのボタンは出さない --%>
      <c:if test="${!(sessionScope.loginUser.admin && sessionScope.loginTerminal == 'admin')}">
        <a class="btn btn-secondary btn-sm" href="${ctx}/">入口ページに戻る</a>
      </c:if>
      <a class="btn btn-secondary btn-sm" href="${ctx}/serving">提供待ち一覧</a>
    </div>
  </div>
</header>

<main class="status-page kitchen-page">
  <div class="kitchen-heading">
    <h1 class="section-title">テーブルの状況</h1>
    <a class="btn btn-secondary btn-sm" href="${ctx}/hall">最新の状態にする</a>
  </div>

  <c:if test="${not empty successMessage}">
    <p class="alert alert-success"><c:out value="${successMessage}" /></p>
  </c:if>
  <c:if test="${not empty errorMessage}">
    <p class="alert alert-danger"><c:out value="${errorMessage}" /></p>
  </c:if>

  <p class="hall-guide">利用中のテーブルは【人数を変更する】で人数を変更できます。清掃が終わったら、準備中のテーブルの【清掃完了にする】を押してください。</p>

  <ul class="seat-grid">
    <c:forEach var="s" items="${seats}">
      <li>
        <c:choose>
          <%-- 準備中:清掃完了 → 空席(確認してから POST) --%>
          <c:when test="${s.preparing}">
            <div class="seat seat-preparing">
              <span class="seat-no">テーブル${s.tableNo}</span>
              <span class="seat-state">準備中</span>
              <%-- 押すと下の確認ダイアログを開く(送信はダイアログの【空席にする】で行う) --%>
              <button type="button" class="btn btn-primary btn-sm seat-btn seat-clean"
                      data-table-id="${s.tableId}" data-table-no="${s.tableNo}">清掃完了にする</button>
            </div>
          </c:when>
          <%-- 利用中:H-02 人数変更画面へ --%>
          <c:when test="${s.inUse}">
            <div class="seat seat-in-use">
              <span class="seat-no">テーブル${s.tableNo}</span>
              <span class="seat-state">利用中<c:if test="${s.guestCount != null}"> ${s.guestCount}人</c:if></span>
              <a class="btn btn-secondary btn-sm seat-btn" href="${ctx}/guestChange?tableId=${s.tableId}">人数を変更する</a>
            </div>
          </c:when>
          <%-- 空席:押せない --%>
          <c:otherwise>
            <div class="seat seat-vacant">
              <span class="seat-no">テーブル${s.tableNo}</span>
              <span class="seat-state"><c:out value="${s.status}" /></span>
            </div>
          </c:otherwise>
        </c:choose>
      </li>
    </c:forEach>
  </ul>

  <div class="seat-legend">
    <span class="legend-title">凡例</span>
    <span class="legend-item"><span class="legend-swatch seat-vacant"></span>空席</span>
    <span class="legend-item"><span class="legend-swatch seat-in-use"></span>利用中</span>
    <span class="legend-item"><span class="legend-swatch seat-preparing"></span>準備中(清掃待ち)</span>
  </div>

  <p class="kitchen-note">※この画面は 30 秒ごとに自動で最新になります。</p>
</main>

<%-- 清掃完了の確認ダイアログ(準備中 → 空席) --%>
<dialog id="cleanDialog" class="confirm-dialog" aria-labelledby="cleanDialogTitle">
  <form method="post" action="${ctx}/hall">
    <input type="hidden" name="tableId" id="cleanTableId">
    <input type="hidden" name="tableNo" id="cleanTableNo">

    <h2 id="cleanDialogTitle" class="confirm-title">
      <span class="confirm-table">テーブル<span id="cleanTableLabel"></span></span>を空席にしますか?
    </h2>
    <div class="confirm-change">
      <span class="tag confirm-from">準備中</span>
      <span class="confirm-arrow" aria-hidden="true">→</span>
      <span class="tag confirm-to">空席</span>
    </div>
    <ul class="confirm-points">
      <li>テーブルの<strong>清掃が終わっている</strong>ことを確認してください。</li>
      <li>空席にすると、テーブル端末が<strong>人数入力画面</strong>に切り替わり、次のお客様が使えるようになります。</li>
    </ul>

    <div class="confirm-actions">
      <button type="button" class="btn btn-secondary" id="cleanCancel">キャンセル</button>
      <button type="submit" class="btn btn-primary">空席にする</button>
    </div>
  </form>
</dialog>

<script>
(function () {
  const dialog = document.getElementById('cleanDialog');

  // 【清掃完了にする】→ 押されたテーブルの番号を入れてダイアログを開く
  document.querySelectorAll('.seat-clean').forEach(function (btn) {
    btn.addEventListener('click', function () {
      document.getElementById('cleanTableId').value = btn.dataset.tableId;
      document.getElementById('cleanTableNo').value = btn.dataset.tableNo;
      document.getElementById('cleanTableLabel').textContent = btn.dataset.tableNo;
      dialog.showModal();
    });
  });

  document.getElementById('cleanCancel').addEventListener('click', function () {
    dialog.close();
  });

  // 30 秒ごとに最新にする。ダイアログを開いている間は待つ(途中で画面が切り替わらないように)
  setInterval(function () {
    if (!dialog.open) {
      location.href = '${ctx}/hall';
    }
  }, 30000);
})();
</script>
</body>
</html>
