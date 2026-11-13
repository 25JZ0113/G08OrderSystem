<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%-- T-08 人数入力画面 --%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="screenTitle" value="人数入力" />
<c:set var="showActions" value="${false}" />
<!DOCTYPE html>
<html lang="ja">
<head>
<%@ include file="common/head.jspf" %>
<title>いらっしゃいませ | 喫茶 生成り</title>
</head>
<body>
<%@ include file="common/tableHeader.jspf" %>

<main class="welcome-page">
  <c:if test="${not empty errorMessage}">
    <p class="alert alert-danger">エラー:<c:out value="${errorMessage}" /></p>
  </c:if>

  <c:choose>
    <%-- 端末にテーブルが設定されていない → S-01 テーブル番号確認画面から設定する --%>
    <c:when test="${needSetup}">
      <h2 class="welcome-title">テーブル番号が設定されていません</h2>
      <p class="welcome-lead">この端末のテーブル番号を設定してください。</p>
      <a class="btn btn-primary btn-lg" href="${ctx}/tableConfirm">テーブル番号を設定する</a>
    </c:when>

    <%-- 空席:人数入力 --%>
    <c:when test="${not empty terminal && empty errorMessage}">
      <h2 class="welcome-title">いらっしゃいませ</h2>
      <form class="guest-form" action="${ctx}/guestCount" method="post">
        <p class="welcome-lead" id="label-guests">ご利用の人数を入力してください</p>
        <div class="guest-qty" role="group" aria-labelledby="label-guests">
          <button type="button" class="btn btn-secondary guest-btn" data-step="-1" aria-label="1人減らす">−</button>
          <input type="number" name="guestCount" id="guestCount" class="guest-input"
                 value="1" min="1" max="10" required aria-labelledby="label-guests">
          <button type="button" class="btn btn-secondary guest-btn" data-step="1" aria-label="1人増やす">＋</button>
          <span class="guest-unit">人</span>
        </div>
        <button type="submit" class="btn btn-primary btn-lg">注文をはじめる</button>
        <p class="welcome-note">※人数を入力するとテーブルが「利用中」になり、メニュー画面が表示されます</p>
      </form>
    </c:when>
  </c:choose>
</main>

<script>
// −/＋ボタンで人数を 1〜10 の範囲で増減する
(function () {
  const input = document.getElementById('guestCount');
  if (!input) return;
  document.querySelectorAll('.guest-btn').forEach(function (b) {
    b.addEventListener('click', function () {
      const n = (Number(input.value) || 1) + Number(b.dataset.step);
      input.value = Math.min(10, Math.max(1, n));
    });
  });
})();
</script>
</body>
</html>
