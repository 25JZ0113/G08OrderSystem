<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%-- T-02 商品詳細画面 --%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="screenTitle" value="商品詳細" />
<c:set var="showActions" value="${false}" />
<!DOCTYPE html>
<html lang="ja">
<head>
<%@ include file="common/head.jspf" %>
<title><c:out value="${product.name}" /> | 喫茶 生成り</title>
</head>
<body>
<%@ include file="common/tableHeader.jspf" %>

<main class="detail-page">
  <%-- 戻る:カートから選び直しているときはカートへ、それ以外は元のカテゴリのメニューへ --%>
  <c:choose>
    <c:when test="${not empty editItem}">
      <c:url var="backUrl" value="/cart" />
    </c:when>
    <c:otherwise>
      <c:url var="backUrl" value="/productList">
        <c:if test="${not empty product}"><c:param name="category" value="${product.categoryName}" /></c:if>
      </c:url>
    </c:otherwise>
  </c:choose>
  <p><a class="btn btn-secondary" href="${backUrl}">戻る</a></p>

  <c:choose>
    <c:when test="${not empty errorMessage}">
      <p class="alert alert-danger">エラー:<c:out value="${errorMessage}" /></p>
    </c:when>
    <c:otherwise>
      <form class="detail" action="${ctx}/cartAdd" method="post">
        <%-- カートから選び直しているときは、置き換える行番号を送る --%>
        <c:if test="${not empty editItem}">
          <input type="hidden" name="editLine" value="${editItem.lineId}">
        </c:if>
        <%-- 左:商品画像(ファイルがないときは「商品画像」の枠) --%>
        <div class="detail-image">
          <c:if test="${not empty product.imageUrl}">
            <img src="${ctx}/images/<c:out value='${product.imageUrl}' />" alt="<c:out value='${product.name}' />" onerror="this.remove()">
          </c:if>
          <span class="product-image-placeholder">商品画像</span>
        </div>

        <%-- 右:商品名・説明・サイズ・トッピング・数量・金額 --%>
        <div class="detail-body">
          <h2 class="detail-name"><c:out value="${product.name}" /></h2>
          <c:if test="${not empty product.description}">
            <p class="detail-desc"><c:out value="${product.description}" /></p>
          </c:if>

          <%-- サイズ:選んだ商品の ID がそのまま注文する商品 ID になる --%>
          <c:choose>
            <c:when test="${variants.size() > 1 || not empty product.sizeName}">
              <div class="detail-row" role="radiogroup" aria-labelledby="label-size">
                <span class="detail-label" id="label-size">サイズ</span>
                <div class="segmented">
                  <c:forEach var="v" items="${variants}">
                    <label class="segmented-item">
                      <input type="radio" name="productId" value="${v.id}" data-price="${v.price}"
                             <c:if test="${v.id == product.id}">checked</c:if>>
                      <span><c:out value="${v.sizeName}" /></span>
                    </label>
                  </c:forEach>
                </div>
              </div>
            </c:when>
            <c:otherwise>
              <input type="hidden" name="productId" value="${product.id}" data-price="${product.price}">
            </c:otherwise>
          </c:choose>

          <%-- トッピング(PRODUCT_TOPPINGS に設定がある商品だけ表示) --%>
          <c:if test="${not empty toppings}">
            <div class="detail-row detail-row-top">
              <span class="detail-label">トッピング</span>
              <div class="topping-list">
                <c:forEach var="t" items="${toppings}">
                  <label class="topping-item">
                    <input type="checkbox" name="toppingId" value="${t.id}" data-price="${t.price}"
                           <c:if test="${not empty editItem && editItem.toppingIds.contains(t.id)}">checked</c:if>>
                    <span class="topping-name"><c:out value="${t.name}" /></span>
                    <span class="topping-price">+<fmt:formatNumber value="${t.price}" />円</span>
                  </label>
                </c:forEach>
              </div>
            </div>
          </c:if>

          <%-- 数量 --%>
          <div class="detail-row">
            <span class="detail-label" id="label-qty">数量</span>
            <div class="qty">
              <button type="button" class="btn btn-secondary qty-btn" data-step="-1" aria-label="1つ減らす">−</button>
              <input type="number" name="quantity" id="quantity" class="qty-input" value="${empty editItem ? 1 : editItem.quantity}" min="1" max="10" aria-labelledby="label-qty">
              <button type="button" class="btn btn-secondary qty-btn" data-step="1" aria-label="1つ増やす">＋</button>
            </div>
          </div>

          <%-- 金額(サイズ・トッピング・数量から計算) --%>
          <p class="detail-total" id="total" aria-live="polite">-</p>
        </div>

        <div class="detail-footer">
          <button type="submit" class="btn btn-primary btn-lg">${empty editItem ? 'カートに入れる' : '変更する'}</button>
        </div>
      </form>
    </c:otherwise>
  </c:choose>
</main>

<script>
// 選んだサイズ・トッピング・数量から金額を計算して表示する
// (画面表示用。注文の確定金額はサーバー側で計算する)
(function () {
  const form = document.querySelector('form.detail');
  if (!form) return;
  const qty = document.getElementById('quantity');
  const total = document.getElementById('total');

  function update() {
    const size = form.querySelector('input[name="productId"]:checked, input[type="hidden"][name="productId"]');
    let unit = size ? Number(size.dataset.price) : 0;
    form.querySelectorAll('input[name="toppingId"]:checked').forEach(function (t) {
      unit += Number(t.dataset.price);
    });
    const n = Math.min(10, Math.max(1, Number(qty.value) || 1));
    total.textContent = (unit * n).toLocaleString('ja-JP') + '円';
  }

  form.querySelectorAll('.qty-btn').forEach(function (b) {
    b.addEventListener('click', function () {
      const n = (Number(qty.value) || 1) + Number(b.dataset.step);
      qty.value = Math.min(10, Math.max(1, n));
      update();
    });
  });
  form.addEventListener('change', update);
  qty.addEventListener('input', update);
  update();
})();
</script>
</body>
</html>
