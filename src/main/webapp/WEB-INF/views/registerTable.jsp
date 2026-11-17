<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%-- R-01 テーブル番号入力画面(レジ端末) --%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="screenTitle" value="テーブル番号入力" />
<c:set var="showLogout" value="${true}" />
<!DOCTYPE html>
<html lang="ja">
<head>
<%@ include file="common/head.jspf" %>
<title>テーブル番号入力 | 喫茶 生成り</title>
</head>
<body>
<%@ include file="common/registerHeader.jspf" %>

<main class="welcome-page">
  <c:if test="${not empty errorMessage}">
    <p class="alert alert-danger" role="alert"><c:out value="${errorMessage}" /></p>
  </c:if>

  <form class="register-table-form" method="post" action="${ctx}/register/table">
    <label class="welcome-lead" for="tableNo">テーブル番号札の番号を入力してください</label>
    <input type="text" name="tableNo" id="tableNo" class="guest-input table-no-input"
           value="<c:out value='${inputNo}' />" inputmode="numeric" pattern="[0-9]*" maxlength="2"
           required autofocus autocomplete="off">

    <%-- テンキー(押した数字を入力欄の末尾に足す) --%>
    <div class="keypad" role="group" aria-label="テンキー">
      <c:forEach var="n" begin="1" end="9">
        <button type="button" class="btn btn-secondary keypad-key" data-key="${n}">${n}</button>
      </c:forEach>
      <button type="button" class="btn btn-secondary keypad-key" data-key="clear">クリア</button>
      <button type="button" class="btn btn-secondary keypad-key" data-key="0">0</button>
      <button type="button" class="btn btn-secondary keypad-key" data-key="back" aria-label="1文字消す">←</button>
    </div>

    <button type="submit" class="btn btn-primary btn-lg">会計内容を表示</button>
  </form>
</main>

<script>
(function () {
  const input = document.getElementById('tableNo');
  document.querySelectorAll('.keypad-key').forEach(function (b) {
    b.addEventListener('click', function () {
      const k = b.dataset.key;
      if (k === 'clear') {
        input.value = '';
      } else if (k === 'back') {
        input.value = input.value.slice(0, -1);
      } else if (input.value.length < 2) {
        input.value += k;
      }
      input.focus();
    });
  });
})();
</script>
</body>
</html>
