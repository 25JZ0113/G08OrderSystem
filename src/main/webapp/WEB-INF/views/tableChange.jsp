<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%-- S-02 テーブル番号変更画面 --%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="screenTitle" value="テーブル番号変更" />
<c:set var="showActions" value="${false}" />
<!DOCTYPE html>
<html lang="ja">
<head>
<%@ include file="common/head.jspf" %>
<title>テーブル番号変更 | 喫茶 生成り</title>
</head>
<body>
<%@ include file="common/tableHeader.jspf" %>

<main class="welcome-page">
  <c:if test="${not empty errorMessage}">
    <p class="alert alert-danger" role="alert">エラー:<c:out value="${errorMessage}" /></p>
  </c:if>

  <form class="table-change-form" action="${ctx}/tableChange" method="post">
    <label class="welcome-lead" for="tableNo">新しいテーブル番号を入力</label>
    <input type="number" name="tableNo" id="tableNo" class="guest-input table-no-input"
           value="<c:out value='${inputNo}' />" min="${minTableNo}" max="${maxTableNo}"
           required autofocus aria-describedby="tableNoHelp">
    <p class="welcome-lead" id="tableNoHelp">${minTableNo}〜${maxTableNo}の番号を入力できます</p>

    <div class="table-confirm-actions">
      <%-- 変更せずに S-01 テーブル番号確認画面へ戻る --%>
      <a class="btn btn-secondary btn-lg" href="${ctx}/tableConfirm">戻る</a>
      <button type="submit" class="btn btn-primary btn-lg">保存する</button>
    </div>
  </form>
</main>
</body>
</html>
