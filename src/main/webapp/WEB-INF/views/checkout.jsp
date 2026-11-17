<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%-- T-06 会計依頼画面 --%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="screenTitle" value="会計" />
<c:set var="showActions" value="${false}" />
<!DOCTYPE html>
<html lang="ja">
<head>
<%@ include file="common/head.jspf" %>
<title>お会計 | 喫茶 生成り</title>
</head>
<body>
<%@ include file="common/tableHeader.jspf" %>

<main class="status-page">
  <div class="page-actions">
    <a class="btn btn-secondary" href="${ctx}/productList">メニューに戻る</a>
  </div>

  <c:choose>
    <c:when test="${not empty errorMessage}">
      <p class="alert alert-danger">エラー:<c:out value="${errorMessage}" /></p>
    </c:when>

    <%-- まだ何も注文していない --%>
    <c:when test="${not ordered}">
      <p class="empty status-empty">まだ注文はありません。メニューから商品を選んでください。</p>
    </c:when>

    <%-- 未提供あり:会計できない --%>
    <c:when test="${not empty unserved}">
      <p class="checkout-warning" role="alert">未提供の商品があります</p>

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
            <th>状態</th>
          </tr>
        </thead>
        <tbody>
          <c:forEach var="line" items="${unserved}">
            <c:set var="d" value="${line.detail}" />
            <tr>
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
                <span class="tag ${d.orderStatus == '注文受付済み' ? '' : 'tag-accent'}"><c:out value="${d.orderStatus}" /></span>
              </td>
            </tr>
          </c:forEach>
        </tbody>
      </table>

      <p class="checkout-message">商品の提供完了までお待ちください</p>
    </c:when>

    <%-- 未提供なし:会計できる --%>
    <c:otherwise>
      <div class="checkout-ready">
        <p class="checkout-label">お会計金額</p>
        <p class="checkout-amount"><fmt:formatNumber value="${total}" />円</p>
        <p class="checkout-message">
          テーブル番号札(<c:out value="${sessionScope.tableNo}" />番)をお持ちのうえ、レジへお越しください
        </p>
      </div>
    </c:otherwise>
  </c:choose>
</main>
</body>
</html>
