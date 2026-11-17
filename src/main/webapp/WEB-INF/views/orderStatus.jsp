<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%-- T-04 注文状況画面 --%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="screenTitle" value="注文状況" />
<c:set var="showActions" value="${false}" />
<!DOCTYPE html>
<html lang="ja">
<head>
<%@ include file="common/head.jspf" %>
<title>注文状況 | 喫茶 生成り</title>
</head>
<body>
<%@ include file="common/tableHeader.jspf" %>

<main class="status-page">
  <div class="page-actions">
    <a class="btn btn-secondary" href="${ctx}/productList">メニューに戻る</a>
    <%-- キッチン・ホールで状態が変わるので、最新の状態を見られるようにする --%>
    <a class="btn btn-secondary" href="${ctx}/orderStatus">最新の状態にする</a>
  </div>

  <c:if test="${not empty errorMessage}">
    <p class="alert alert-danger">エラー:<c:out value="${errorMessage}" /></p>
  </c:if>

  <c:choose>
    <c:when test="${empty lines}">
      <p class="empty status-empty">まだ注文はありません。メニューから商品を選んでください。</p>
    </c:when>
    <c:otherwise>
      <table class="status-table">
        <colgroup>
          <col>
          <col class="col-qty">
          <col class="col-status">
        </colgroup>
        <thead>
          <tr>
            <th>商品名</th>
            <th class="num">数量</th>
            <th>注文状態</th>
          </tr>
        </thead>
        <tbody>
          <%-- line は OrderLine(商品の明細 + 付いたトッピング) --%>
          <c:forEach var="line" items="${lines}">
            <c:set var="d" value="${line.detail}" />
            <tr class="${d.canceled ? 'is-canceled' : ''}">
              <td>
                <span class="status-name">
                  <c:out value="${d.productName}" /><c:if test="${not empty d.sizeName}">(<c:out value="${d.sizeName}" />)</c:if>
                </span>
                <c:if test="${not empty line.toppings}">
                  <span class="status-toppings">
                    <c:forEach var="t" items="${line.toppings}" varStatus="st">
                      <c:out value="${t.productName}" /><c:if test="${!st.last}">・</c:if>
                    </c:forEach>
                  </span>
                </c:if>
              </td>
              <td class="num">${d.quantity}</td>
              <td>
                <%-- 状態ごとにタグの色を変える(色だけでなく文言でも伝わる) --%>
                <c:choose>
                  <c:when test="${d.orderStatus == '提供済み'}"><c:set var="tone" value="tag-success" /></c:when>
                  <c:when test="${d.orderStatus == '調理中' || d.orderStatus == '調理済み(提供待ち)'}"><c:set var="tone" value="tag-accent" /></c:when>
                  <c:when test="${d.canceled}"><c:set var="tone" value="tag-muted" /></c:when>
                  <c:otherwise><c:set var="tone" value="" /></c:otherwise>
                </c:choose>
                <span class="tag ${tone}"><c:out value="${d.orderStatus}" /></span>
              </td>
            </tr>
          </c:forEach>
        </tbody>
      </table>
    </c:otherwise>
  </c:choose>
</main>
</body>
</html>
