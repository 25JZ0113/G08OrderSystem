<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%-- T-05 注文履歴画面 --%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="screenTitle" value="注文履歴" />
<c:set var="showActions" value="${false}" />
<!DOCTYPE html>
<html lang="ja">
<head>
<%@ include file="common/head.jspf" %>
<title>注文履歴 | 喫茶 生成り</title>
</head>
<body>
<%@ include file="common/tableHeader.jspf" %>

<main class="cart-page">
  <div class="page-actions">
    <a class="btn btn-secondary" href="${ctx}/productList">メニューに戻る</a>
  </div>

  <c:if test="${not empty errorMessage}">
    <p class="alert alert-danger">エラー:<c:out value="${errorMessage}" /></p>
  </c:if>

  <c:choose>
    <c:when test="${empty lines}">
      <p class="empty cart-empty">まだ注文はありません。メニューから商品を選んでください。</p>
    </c:when>
    <c:otherwise>
      <%-- line は OrderLine(商品の明細 + 付いたトッピング)。注文した順に並ぶ --%>
      <ul class="cart-list">
        <c:forEach var="line" items="${lines}">
          <c:set var="d" value="${line.detail}" />
          <li class="cart-row history-row ${d.noCharge ? 'is-canceled' : ''}">
            <div class="cart-item">
              <span class="history-name">
                <c:out value="${d.productName}" /><c:if test="${not empty d.sizeName}">(<c:out value="${d.sizeName}" />)</c:if>
                ×${d.quantity}
              </span>
              <c:if test="${not empty line.toppings}">
                <span class="cart-item-toppings">
                  <c:forEach var="t" items="${line.toppings}" varStatus="st">
                    <c:out value="${t.productName}" /><c:if test="${!st.last}">・</c:if>
                  </c:forEach>
                </span>
              </c:if>
              <%-- 取り消された商品は状態も出す
                   (キャンセル(請求なし)は 0 円で取り消し線、キャンセル(請求あり)は料金を請求する) --%>
              <c:if test="${d.canceled}">
                <span class="tag tag-muted history-tag"><c:out value="${d.orderStatus}" /></span>
              </c:if>
            </div>
            <p class="cart-item-price"><fmt:formatNumber value="${line.amount}" />円</p>
          </li>
        </c:forEach>
      </ul>
    </c:otherwise>
  </c:choose>
</main>

<footer class="cart-footer">
  <p class="cart-total">合計</p>
  <p class="cart-total"><span class="cart-total-price"><fmt:formatNumber value="${empty total ? 0 : total}" />円</span></p>
</footer>
</body>
</html>
