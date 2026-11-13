<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%-- T-01 メニュー画面 --%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="screenTitle" value="メニュー" />
<c:set var="showActions" value="${true}" />
<!DOCTYPE html>
<html lang="ja">
<head>
<%@ include file="common/head.jspf" %>
<title>メニュー | 喫茶 生成り</title>
</head>
<body>
<%@ include file="common/tableHeader.jspf" %>

<div class="menu-layout">
  <%-- 左:カテゴリ一覧とカート点数 --%>
  <aside class="sidebar">
    <nav class="category-list" aria-label="カテゴリ">
      <c:forEach var="entry" items="${menuByCategory}">
        <c:url var="categoryUrl" value="/productList">
          <c:param name="category" value="${entry.key}" />
        </c:url>
        <a class="category-item ${entry.key == selectedCategory ? 'is-current' : ''}" href="${categoryUrl}"
           <c:if test="${entry.key == selectedCategory}">aria-current="page"</c:if>>
          <c:out value="${entry.key}" />
        </a>
      </c:forEach>
    </nav>
    <%-- カートの点数(セッションのカートから) --%>
    <p class="cart-count">カート <span class="cart-count-value">${empty sessionScope.cart ? 0 : sessionScope.cart.totalQuantity}</span>点</p>
  </aside>

  <%-- 右:選んだカテゴリの商品 --%>
  <main class="menu-main">
    <c:if test="${not empty errorMessage}">
      <p class="alert alert-danger">エラー:<c:out value="${errorMessage}" /></p>
    </c:if>
    <%-- 商品詳細からカートに入れて戻ってきたとき --%>
    <c:if test="${param.added == '1'}">
      <p class="alert alert-success" role="status">カートに入れました。</p>
    </c:if>
    <%-- カート画面で注文を確定して戻ってきたとき --%>
    <c:if test="${param.ordered == '1'}">
      <p class="alert alert-success" role="status">ご注文を受け付けました。できあがりまでお待ちください。</p>
    </c:if>

    <c:choose>
      <c:when test="${empty items}">
        <p class="empty">表示できる商品がありません。</p>
      </c:when>
      <c:otherwise>
        <h2 class="menu-heading"><c:out value="${selectedCategory}" /></h2>
        <ul class="product-grid">
          <%-- item は MenuItem(サイズ違いをまとめた 1 商品) --%>
          <c:forEach var="item" items="${items}">
            <li>
              <a class="product-card" href="${ctx}/productDetail?id=${item.firstProductId}">
                <span class="product-image">
                  <c:if test="${not empty item.variants[0].imageUrl}">
                    <img src="${ctx}/images/<c:out value='${item.variants[0].imageUrl}' />" alt="" onerror="this.remove()">
                  </c:if>
                  <span class="product-image-placeholder">商品画像</span>
                </span>
                <span class="product-info">
                  <span class="product-name"><c:out value="${item.name}" /></span>
                  <span class="product-price"><fmt:formatNumber value="${item.minPrice}" />円<c:if test="${item.priceVaries}">〜</c:if></span>
                </span>
              </a>
            </li>
          </c:forEach>
        </ul>
      </c:otherwise>
    </c:choose>

    <div class="menu-footer">
      <a class="btn btn-primary btn-lg" href="${ctx}/cart">
        カートを見る
        <span class="btn-price"><fmt:formatNumber value="${empty sessionScope.cart ? 0 : sessionScope.cart.totalAmount}" />円</span>
      </a>
    </div>
  </main>
</div>
</body>
</html>
