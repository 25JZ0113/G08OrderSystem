<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%-- A-06 売上の注文一覧(管理者端末)。売上管理画面(A-05)の表の行・グラフの棒から開く --%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="ja">
<head>
<%@ include file="common/head.jspf" %>
<title><c:out value="${title}" /> | 売上管理 | 喫茶 生成り</title>
</head>
<body>
<header class="app-header">
  <p class="app-title">
    <span class="app-shop">喫茶 生成り</span>
    <span class="app-screen">売上の注文一覧</span>
  </p>
  <div class="app-header-row">
    <p class="table-info">
      管理者端末
      <span class="register-login">ログイン:<span class="table-info-value"><c:out value="${sessionScope.loginUser.name}" /></span></span>
    </p>
  </div>
</header>

<main class="admin-sales sales-orders">
  <div class="sales-orders-head">
    <a class="btn btn-secondary" href="<c:out value='${backUrl}' />">売上管理に戻る</a>
    <div>
      <h1 class="section-title sales-orders-title"><c:out value="${title}" /></h1>
      <p class="sales-orders-period">会計日:<c:out value="${periodText}" /></p>
    </div>
  </div>

  <c:if test="${not empty errorMessage}">
    <p class="alert alert-danger" role="alert"><c:out value="${errorMessage}" /></p>
  </c:if>

  <c:if test="${empty errorMessage}">
    <%-- 表示している注文の合計 --%>
    <dl class="admin-sales-kpis sales-orders-kpis">
      <div><dt>注文</dt><dd><fmt:formatNumber value="${orders.size()}" /><small>件</small></dd></div>
      <div><dt>客数</dt><dd><fmt:formatNumber value="${totalGuests}" /><small>人</small></dd></div>
      <div><dt>売上金額</dt><dd><fmt:formatNumber value="${totalAmount}" /><small>円</small></dd></div>
    </dl>

    <c:if test="${truncated}">
      <p class="alert alert-danger" role="status">注文が多いので、会計日時の早い順に ${maxOrders} 件まで表示しています。期間を短くすると、すべての注文を見られます。</p>
    </c:if>

    <c:choose>
      <c:when test="${empty orders}">
        <p class="admin-empty sales-orders-empty">この条件で会計した注文はありません</p>
      </c:when>
      <c:otherwise>
        <ol class="sales-order-list">
          <c:forEach var="so" items="${orders}">
            <c:set var="o" value="${so.order}" />
            <li class="sales-order">
              <%-- 注文の情報 --%>
              <div class="sales-order-head">
                <p class="sales-order-id">注文 <span class="sales-order-num">#${o.id}</span></p>
                <dl class="sales-order-meta">
                  <div><dt>会計</dt><dd><fmt:formatDate value="${o.paymentDatetime}" pattern="yyyy/MM/dd HH:mm" /></dd></div>
                  <div><dt>来店</dt><dd><fmt:formatDate value="${o.visitDatetime}" pattern="HH:mm" /></dd></div>
                  <div><dt>テーブル</dt><dd>${o.tableNo}</dd></div>
                  <div><dt>人数</dt><dd>${o.guestCount}名</dd></div>
                  <div><dt>支払</dt><dd><c:out value="${o.paymentMethod}" /></dd></div>
                </dl>
                <p class="sales-order-total"><fmt:formatNumber value="${o.totalAmount}" />円</p>
              </div>

              <%-- 注文明細(トッピングは元の商品の下にまとめる) --%>
              <table class="status-table sales-order-lines">
                <colgroup>
                  <col>
                  <col class="col-qty">
                  <col class="col-amount">
                  <col class="col-state">
                </colgroup>
                <thead>
                  <tr>
                    <th>商品名</th>
                    <th class="num">数量</th>
                    <th class="amount">金額</th>
                    <th>状態</th>
                  </tr>
                </thead>
                <tbody>
                  <c:forEach var="line" items="${so.lines}">
                    <c:set var="d" value="${line.detail}" />
                    <%-- 商品別から開いたときは、その商品(トッピングとして付けたときも)の行に印を付ける --%>
                    <c:set var="hit" value="${false}" />
                    <c:if test="${not empty productId}">
                      <c:if test="${d.productId == productId}"><c:set var="hit" value="${true}" /></c:if>
                      <c:forEach var="t" items="${line.toppings}">
                        <c:if test="${t.productId == productId}"><c:set var="hit" value="${true}" /></c:if>
                      </c:forEach>
                    </c:if>
                    <tr class="${hit ? 'is-hit' : ''} ${d.noCharge ? 'is-canceled' : ''}">
                      <td>
                        <span class="status-name">
                          <c:out value="${d.productName}" /><c:if test="${not empty d.sizeName}">(<c:out value="${d.sizeName}" />)</c:if>
                        </span>
                        <c:if test="${not empty line.toppings}">
                          <span class="status-toppings">
                            <c:forEach var="t" items="${line.toppings}" varStatus="st">
                              +<c:out value="${t.productName}" /><c:if test="${!st.last}"> </c:if>
                            </c:forEach>
                          </span>
                        </c:if>
                      </td>
                      <td class="num">${d.quantity}</td>
                      <td class="amount"><fmt:formatNumber value="${line.amount}" />円</td>
                      <td>
                        <c:choose>
                          <c:when test="${d.canceled}"><span class="tag tag-muted"><c:out value="${d.orderStatus}" /></span></c:when>
                          <c:otherwise><c:out value="${d.orderStatus}" /></c:otherwise>
                        </c:choose>
                      </td>
                    </tr>
                  </c:forEach>
                </tbody>
              </table>
            </li>
          </c:forEach>
        </ol>
      </c:otherwise>
    </c:choose>
    <p class="admin-users-note">※金額は税込みです。キャンセル(請求なし)の商品は 0 円で、注文の合計にも入っていません(キャンセル(請求あり)は入っています)</p>
  </c:if>
</main>
</body>
</html>
