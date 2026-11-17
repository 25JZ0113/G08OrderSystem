<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%-- A-04 メニュー(商品)管理画面(管理者端末) --%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="catId" value="${empty selectedCategory ? 0 : selectedCategory.id}" />
<c:set var="isNew" value="${selectedProductId == 0}" />
<!DOCTYPE html>
<html lang="ja">
<head>
<%@ include file="common/head.jspf" %>
<title>メニュー(商品)管理 | 喫茶 生成り</title>
</head>
<body>
<header class="app-header">
  <p class="app-title">
    <span class="app-shop">喫茶 生成り</span>
    <span class="app-screen">メニュー(商品)管理</span>
  </p>
  <div class="app-header-row">
    <p class="table-info">
      管理者端末
      <span class="register-login">ログイン:<span class="table-info-value"><c:out value="${sessionScope.loginUser.name}" /></span></span>
    </p>
  </div>
</header>

<main class="admin-products">
  <c:if test="${not empty successMessage}">
    <p class="alert alert-success admin-wide"><c:out value="${successMessage}" /></p>
  </c:if>
  <c:if test="${not empty errorMessage}">
    <p class="alert alert-danger admin-wide" role="alert"><c:out value="${errorMessage}" /></p>
  </c:if>

  <%-- 左:カテゴリ --%>
  <section class="admin-categories" aria-labelledby="categoryTitle">
    <a class="btn btn-secondary btn-block" href="${ctx}/admin/menu">管理者メニューに戻る</a>

    <h1 class="section-title admin-block-title" id="categoryTitle">カテゴリ</h1>
    <ul class="admin-category-list">
      <c:forEach var="cat" items="${categories}">
        <li>
          <a href="${ctx}/admin/products?categoryId=${cat.id}"
             class="${cat.id == catId ? 'is-selected' : ''}"
             ${cat.id == catId ? 'aria-current="page"' : ''}><c:out value="${cat.name}" /></a>
        </li>
      </c:forEach>
    </ul>

    <form method="post" action="${ctx}/admin/products" class="admin-category-form" id="categoryForm">
      <input type="hidden" name="categoryId" value="${catId}">
      <input type="hidden" name="productId" value="${selectedProductId}">
      <label for="categoryName">カテゴリ名</label>
      <input type="text" name="categoryName" id="categoryName" class="login-input" maxlength="20"
             value="<c:out value='${categoryForm.name}' />" autocomplete="off">
      <label for="categoryDescription">説明</label>
      <input type="text" name="categoryDescription" id="categoryDescription" class="login-input" maxlength="100"
             value="<c:out value='${categoryForm.description}' />" autocomplete="off">
      <div class="admin-category-buttons">
        <button type="submit" name="action" value="categoryAdd" class="btn btn-secondary">追加</button>
        <button type="submit" name="action" value="categoryUpdate" class="btn btn-secondary" ${catId == 0 ? 'disabled' : ''}>変更</button>
        <button type="submit" name="action" value="categoryDelete" class="btn btn-danger btn-confirm"
                ${catId == 0 ? 'disabled' : ''}
                data-confirm="<c:out value='カテゴリ「${selectedCategory.name}」を削除します。よろしいですか?' />">削除</button>
      </div>
      <p class="admin-users-note">※追加:名前を入力して【追加】<br>※変更:選んでいるカテゴリの名前・説明を直して【変更】</p>
    </form>
  </section>

  <div class="admin-products-main">
    <%-- 右上:商品一覧 --%>
    <section aria-labelledby="productListTitle">
      <div class="admin-products-head">
        <h2 class="section-title" id="productListTitle">商品一覧(<c:out value="${selectedCategory.name}" />)</h2>
        <a class="btn btn-secondary" href="${ctx}/admin/products?categoryId=${catId}">商品を追加</a>
      </div>

      <%-- 商品が多いと商品情報が下に隠れるので、一覧は枠の中でスクロールする --%>
      <div class="admin-table-scroll">
      <table class="status-table admin-products-table">
        <colgroup>
          <col>
          <col class="col-price">
          <col class="col-tax">
          <col class="col-sales">
          <col class="col-select">
        </colgroup>
        <thead>
          <tr>
            <th>商品名</th>
            <th class="amount">価格</th>
            <th class="num">税率</th>
            <th>販売状態</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <c:forEach var="p" items="${products}">
            <tr class="${p.id == selectedProductId ? 'is-selected' : ''}">
              <td><c:out value="${p.name}" /><c:if test="${not empty p.sizeName}">(<c:out value="${p.sizeName}" />)</c:if></td>
              <td class="amount"><fmt:formatNumber value="${p.price}" />円</td>
              <td class="num">${p.taxRate}%</td>
              <td><span class="tag ${p.salesStatus == '販売中' ? 'tag-success' : 'tag-muted'}"><c:out value="${p.salesStatus}" /></span></td>
              <td>
                <c:choose>
                  <c:when test="${p.id == selectedProductId}">
                    <span class="btn btn-primary btn-sm btn-block" aria-current="true">選択中</span>
                  </c:when>
                  <c:otherwise>
                    <a class="btn btn-secondary btn-sm btn-block" href="${ctx}/admin/products?categoryId=${catId}&productId=${p.id}">選択</a>
                  </c:otherwise>
                </c:choose>
              </td>
            </tr>
          </c:forEach>
          <c:if test="${empty products}">
            <tr><td colspan="5" class="admin-empty">このカテゴリには商品がありません</td></tr>
          </c:if>
        </tbody>
      </table>
      </div>
    </section>

    <%-- 右下:商品情報 --%>
    <section class="admin-product-form" aria-labelledby="productFormTitle">
      <h2 class="section-title" id="productFormTitle">商品情報<span class="admin-users-mode">${isNew ? '(新規登録)' : '(選択中)'}</span></h2>

      <form method="post" action="${ctx}/admin/products" id="productForm">
        <input type="hidden" name="categoryId" value="${catId}">
        <input type="hidden" name="productId" value="${selectedProductId}">

        <div class="admin-product-fields">
          <div class="admin-field">
            <label for="name">商品名</label>
            <input type="text" name="name" id="name" class="login-input" maxlength="30"
                   value="<c:out value='${form.name}' />" required autocomplete="off">
          </div>
          <div class="admin-field">
            <label for="sizeName">サイズ</label>
            <input type="text" name="sizeName" id="sizeName" class="login-input" maxlength="10"
                   value="<c:out value='${form.sizeName}' />" placeholder="例:S(なければ空欄)" autocomplete="off">
          </div>
          <div class="admin-field">
            <label for="price">価格</label>
            <span class="admin-price">
              <input type="number" name="price" id="price" class="login-input" min="0" max="99999" step="1"
                     value="${form.price < 0 || (isNew && empty form.name) ? '' : form.price}" required>
              <span>円(税込)</span>
            </span>
          </div>
          <div class="admin-field">
            <label for="productCategoryId">カテゴリ</label>
            <select name="productCategoryId" id="productCategoryId" class="login-input">
              <c:forEach var="cat" items="${categories}">
                <option value="${cat.id}" ${cat.id == form.categoryId ? 'selected' : ''}><c:out value="${cat.name}" /></option>
              </c:forEach>
            </select>
          </div>
          <div class="admin-field">
            <span id="taxLabel">税率</span>
            <div class="segmented" role="radiogroup" aria-labelledby="taxLabel">
              <c:forEach var="t" items="10,8">
                <label class="segmented-item">
                  <input type="radio" name="taxRate" value="${t}" ${t == form.taxRate ? 'checked' : ''} required>
                  <span>${t}%</span>
                </label>
              </c:forEach>
            </div>
          </div>
          <div class="admin-field">
            <span id="salesLabel">販売状態</span>
            <div class="segmented" role="radiogroup" aria-labelledby="salesLabel">
              <c:forEach var="s" items="販売中,販売停止">
                <label class="segmented-item">
                  <input type="radio" name="salesStatus" value="${s}" ${s == form.salesStatus ? 'checked' : ''} required>
                  <span>${s}</span>
                </label>
              </c:forEach>
            </div>
          </div>
          <div class="admin-field admin-field-wide">
            <label for="description">説明</label>
            <input type="text" name="description" id="description" class="login-input" maxlength="100"
                   value="<c:out value='${form.description}' />" placeholder="おすすめ情報など" autocomplete="off">
          </div>
          <div class="admin-field admin-field-wide">
            <label for="imageUrl">画像</label>
            <span class="admin-image">
              <input type="text" name="imageUrl" id="imageUrl" class="login-input" maxlength="100"
                     value="<c:out value='${form.imageUrl}' />" placeholder="ファイル名(例:blend.jpg)" autocomplete="off">
              <span class="product-image admin-image-preview">
                <c:if test="${not empty form.imageUrl}">
                  <img src="${ctx}/images/<c:out value='${form.imageUrl}' />" alt="" onerror="this.remove()">
                </c:if>
                <span class="product-image-placeholder">商品画像</span>
              </span>
            </span>
          </div>
        </div>

        <div class="admin-users-buttons admin-product-buttons">
          <button type="submit" name="action" value="productRegister" class="btn btn-primary" ${isNew ? '' : 'disabled'}>登録</button>
          <button type="submit" name="action" value="productUpdate" class="btn btn-primary" ${isNew ? 'disabled' : ''}>変更</button>
          <c:set var="productLabel" value="${form.name}" />
          <c:if test="${not empty form.sizeName}"><c:set var="productLabel" value="${form.name}(${form.sizeName})" /></c:if>
          <button type="submit" name="action" value="productDelete" class="btn btn-danger btn-confirm" formnovalidate
                  ${isNew ? 'disabled' : ''}
                  data-confirm="<c:out value='「${productLabel}」を削除します。元に戻せません。よろしいですか?' />">削除</button>
        </div>
        <p class="admin-users-note">※価格を変えても、すでに注文された明細の金額は変わりません。注文されたことがある商品は削除できないので「販売停止」にしてください</p>
      </form>

      <%-- 選んだ商品に付けられるトッピング --%>
      <%-- toppings はトッピングを付けられる商品を選んでいるときだけ入っている(トッピング自身には付けない) --%>
      <c:if test="${toppings != null}">
        <div class="admin-toppings">
          <h3 class="admin-toppings-title">付けられるトッピング</h3>
          <ul class="admin-topping-list">
            <c:forEach var="t" items="${toppings}">
              <li>
                <span><c:out value="${t.name}" />(+<fmt:formatNumber value="${t.price}" />円)<c:if test="${t.salesStatus != '販売中'}"><span class="tag tag-muted">販売停止</span></c:if></span>
                <form method="post" action="${ctx}/admin/products">
                  <input type="hidden" name="categoryId" value="${catId}">
                  <input type="hidden" name="productId" value="${selectedProductId}">
                  <input type="hidden" name="toppingId" value="${t.id}">
                  <button type="submit" name="action" value="toppingRemove" class="btn btn-secondary btn-sm">外す</button>
                </form>
              </li>
            </c:forEach>
            <c:if test="${empty toppings}">
              <li class="admin-empty">付けられるトッピングはありません</li>
            </c:if>
          </ul>
          <c:if test="${not empty toppingCandidates}">
            <form method="post" action="${ctx}/admin/products" class="admin-topping-add">
              <input type="hidden" name="categoryId" value="${catId}">
              <input type="hidden" name="productId" value="${selectedProductId}">
              <select name="toppingId" id="toppingId" class="login-input" aria-label="追加するトッピング">
                <c:forEach var="t" items="${toppingCandidates}">
                  <option value="${t.id}"><c:out value="${t.name}" />(+<fmt:formatNumber value="${t.price}" />円)</option>
                </c:forEach>
              </select>
              <button type="submit" name="action" value="toppingAdd" class="btn btn-secondary">追加</button>
            </form>
          </c:if>
        </div>
      </c:if>
    </section>
  </div>
</main>

<script>
// 選んでいる商品の行が、スクロールする一覧の中で見えるようにする
const selectedRow = document.querySelector('.admin-products-table tr.is-selected');
if (selectedRow) {
  const box = document.querySelector('.admin-table-scroll');
  box.scrollTop = selectedRow.offsetTop - box.clientHeight / 2;
}

// 【削除】は確認してから送信する
document.querySelectorAll('.btn-confirm').forEach(function (b) {
  b.addEventListener('click', function (e) {
    if (!confirm(b.dataset.confirm)) {
      e.preventDefault();
    }
  });
});
</script>
</body>
</html>
