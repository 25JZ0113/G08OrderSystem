<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%-- S-01 テーブル番号確認画面(営業開始時にテーブル端末を起動したとき) --%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="screenTitle" value="テーブル番号確認" />
<c:set var="showActions" value="${false}" />
<c:set var="showHomeLink" value="${true}" />
<!DOCTYPE html>
<html lang="ja">
<head>
<%@ include file="common/head.jspf" %>
<title>テーブル番号確認 | 喫茶 生成り</title>
</head>
<body>
<%@ include file="common/tableHeader.jspf" %>

<main class="welcome-page">
  <c:if test="${not empty errorMessage}">
    <p class="alert alert-danger">エラー:<c:out value="${errorMessage}" /></p>
  </c:if>

  <p class="welcome-lead">この端末のテーブル番号</p>
  <p class="table-no-box">
    <c:out value="${empty terminal ? '-' : terminal.tableNo}" />
  </p>

  <c:choose>
    <c:when test="${not empty terminal}">
      <p class="welcome-lead">番号が正しいか確認してください</p>
    </c:when>
    <c:otherwise>
      <p class="welcome-lead">この端末にはテーブル番号が設定されていません。【変更する】から設定してください</p>
    </c:otherwise>
  </c:choose>

  <div class="table-confirm-actions">
    <%-- S-02 テーブル番号変更画面へ --%>
    <a class="btn btn-secondary btn-lg" href="${ctx}/tableChange">変更する</a>
    <c:choose>
      <c:when test="${not empty terminal}">
        <%-- 確定 → T-08 人数入力画面(営業開始時はテーブル状態が「空席」のため) --%>
        <a class="btn btn-primary btn-lg" href="${ctx}/guestCount">確定する</a>
      </c:when>
      <c:otherwise>
        <button type="button" class="btn btn-primary btn-lg" disabled>確定する</button>
      </c:otherwise>
    </c:choose>
  </div>

</main>
</body>
</html>
