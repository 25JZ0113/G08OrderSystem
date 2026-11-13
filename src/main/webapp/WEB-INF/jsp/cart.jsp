<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%-- T-03 カート画面 --%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="screenTitle" value="カート" />
<c:set var="showActions" value="${false}" />
<c:set var="cart" value="${sessionScope.cart}" />
<!DOCTYPE html>
<html lang="ja">
<head>
<%@ include file="common/head.jspf" %>
<title>カート | 喫茶 生成り</title>
</head>
<body>
<%@ include file="common/tableHeader.jspf" %>

<main class="cart-page">
  <p><a class="btn btn-secondary" href="${ctx}/productList">メニューに戻る</a></p>

  <c:if test="${not empty errorMessage}">
    <p class="alert alert-danger">エラー:<c:out value="${errorMessage}" /></p>
  </c:if>

  <c:choose>
    <c:when test="${empty cart || empty cart.items}">
      <p class="empty cart-empty">カートに商品が入っていません。メニューから商品を選んでください。</p>
    </c:when>
    <c:otherwise>
      <ul class="cart-list">
        <c:forEach var="item" items="${cart.items}">
          <li class="cart-row">
            <%-- 商品名:押すと商品詳細画面でサイズ・トッピング・数量を選び直せる --%>
            <div class="cart-item">
              <a class="cart-item-name" href="${ctx}/productDetail?id=${item.product.id}&amp;edit=${item.lineId}">
                <c:out value="${item.product.name}" />
                <c:if test="${not empty item.product.sizeName}">(<c:out value="${item.product.sizeName}" />)</c:if>
              </a>
              <c:if test="${not empty item.toppings}">
                <span class="cart-item-toppings">
                  <c:forEach var="t" items="${item.toppings}" varStatus="st">
                    <c:out value="${t.name}" /><c:if test="${!st.last}">・</c:if>
                  </c:forEach>
                </span>
              </c:if>
            </div>

            <%-- 小計(1 個あたりの金額 × 数量) --%>
            <p class="cart-item-price">
              <fmt:formatNumber value="${item.subtotal}" />円
              <c:if test="${item.quantity > 1}">
                <span class="cart-item-unit">(<fmt:formatNumber value="${item.unitPrice}" />円 × ${item.quantity})</span>
              </c:if>
            </p>

            <%-- 数量の増減 --%>
            <div class="qty">
              <form action="${ctx}/cart" method="post">
                <input type="hidden" name="action" value="minus">
                <input type="hidden" name="line" value="${item.lineId}">
                <button type="submit" class="btn btn-secondary qty-btn" aria-label="1つ減らす"
                        <c:if test="${item.quantity <= 1}">disabled</c:if>>−</button>
              </form>
              <span class="qty-value">${item.quantity}</span>
              <form action="${ctx}/cart" method="post">
                <input type="hidden" name="action" value="plus">
                <input type="hidden" name="line" value="${item.lineId}">
                <button type="submit" class="btn btn-secondary qty-btn" aria-label="1つ増やす"
                        <c:if test="${item.quantity >= 10}">disabled</c:if>>＋</button>
              </form>
            </div>

            <%-- 削除 --%>
            <form action="${ctx}/cart" method="post">
              <input type="hidden" name="action" value="delete">
              <input type="hidden" name="line" value="${item.lineId}">
              <button type="submit" class="btn btn-secondary">削除</button>
            </form>
          </li>
        </c:forEach>
      </ul>
      <p class="cart-note">※商品名を選ぶと、商品詳細画面でサイズなどを選び直せます。</p>
    </c:otherwise>
  </c:choose>
</main>

<%-- 下:合計と注文確定 --%>
<footer class="cart-footer">
  <p class="cart-total">
    合計<span class="cart-total-price"><fmt:formatNumber value="${empty cart ? 0 : cart.totalAmount}" />円</span>
  </p>
  <%-- 注文を確定する(カートが空のときは押せない)。二重送信を防ぐため、押したらボタンを無効にする --%>
  <form action="${ctx}/orderConfirm" method="post" onsubmit="this.querySelector('button').disabled = true;">
    <button type="submit" class="btn btn-primary btn-lg"
            <c:if test="${empty cart || empty cart.items}">disabled</c:if>>注文を確定する</button>
  </form>
</footer>
</body>
</html>
