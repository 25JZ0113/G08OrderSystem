<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%-- H-02 人数変更画面(ホール端末・ログインなし) --%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="ja">
<head>
<%@ include file="common/head.jspf" %>
<title>人数変更 | 喫茶 生成り</title>
</head>
<body>
<header class="app-header">
  <p class="app-title">
    <span class="app-shop">喫茶 生成り</span>
    <span class="app-screen">人数変更</span>
  </p>
  <div class="app-header-row">
    <p class="table-info">ホール端末</p>
    <div class="header-actions">
      <%@ include file="common/adminMenuButton.jspf" %>
    </div>
  </div>
</header>

<main class="status-page kitchen-page">
  <div class="kitchen-heading">
    <div class="heading-left">
      <%-- 変更せずに H-01 席状況画面へ戻る --%>
      <a class="btn btn-secondary btn-sm" href="${ctx}/hall">席状況に戻る</a>
    </div>
  </div>

  <c:if test="${not empty errorMessage}">
    <p class="alert alert-danger"><c:out value="${errorMessage}" /></p>
  </c:if>

  <c:if test="${not empty seat}">
    <form class="guest-form guest-change" action="${ctx}/guestChange" method="post">
      <input type="hidden" name="orderId" value="${order.id}">
      <input type="hidden" name="tableNo" value="${seat.tableNo}">

      <p class="guest-change-current">
        <span>テーブル番号:<strong>${seat.tableNo}</strong></span>
        <span>現在の人数:<strong>${order.guestCount}人</strong></span>
      </p>

      <p class="welcome-lead" id="label-guests">変更後の人数を入力してください</p>
      <div class="guest-qty" role="group" aria-labelledby="label-guests">
        <button type="button" class="btn btn-secondary guest-btn" data-step="-1" aria-label="1人減らす">−</button>
        <input type="number" name="guestCount" id="guestCount" class="guest-input"
               value="${order.guestCount}" min="1" max="${maxGuests}" required aria-labelledby="label-guests">
        <button type="button" class="btn btn-secondary guest-btn" data-step="1" aria-label="1人増やす">＋</button>
        <span class="guest-unit">人</span>
      </div>

      <button type="submit" class="btn btn-primary btn-lg">変更する</button>
      <p class="welcome-note">※利用中のテーブルの人数だけを変更します(テーブル状態とテーブル端末の画面は変わりません)</p>
    </form>
  </c:if>
</main>

<script>
// −/＋ボタンで人数を 1〜上限 の範囲で増減する(T-08 人数入力と同じ)
(function () {
  const input = document.getElementById('guestCount');
  if (!input) return;
  const max = Number(input.max);
  document.querySelectorAll('.guest-btn').forEach(function (b) {
    b.addEventListener('click', function () {
      const n = (Number(input.value) || 1) + Number(b.dataset.step);
      input.value = Math.min(max, Math.max(1, n));
    });
  });
})();
</script>
</body>
</html>
