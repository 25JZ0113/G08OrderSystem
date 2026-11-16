<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%-- R-03 会計画面(レジ端末)。確定後(payment がある)はおつりとレシートの発行を表示する --%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="screenTitle" value="会計" />
<c:set var="headerOrder" value="${bill.order}" />
<c:set var="showGuests" value="${true}" />
<c:set var="paid" value="${not empty payment}" />
<%-- 選んでいる支払方法(最初は現金) --%>
<c:set var="selected" value="${paid ? payment.method : (empty method ? '現金' : method)}" />
<!DOCTYPE html>
<html lang="ja">
<head>
<%@ include file="common/head.jspf" %>
<title>会計 | 喫茶 生成り</title>
</head>
<body>
<%@ include file="common/registerHeader.jspf" %>

<main class="register-pay">
  <c:if test="${not empty errorMessage}">
    <p class="alert alert-danger register-pay-alert" role="alert"><c:out value="${errorMessage}" /></p>
  </c:if>

  <c:if test="${not empty bill}">
    <%-- 左:注文明細と合計 --%>
    <section class="register-bill" aria-label="会計内容">
      <c:if test="${not paid}">
        <div class="page-actions">
          <a class="btn btn-secondary" href="${ctx}/register/table">戻る</a>
        </div>
      </c:if>

      <table class="status-table register-bill-table">
        <colgroup>
          <col>
          <col class="col-qty">
          <col class="col-amount">
        </colgroup>
        <thead>
          <tr>
            <th>商品名</th>
            <th class="num">数量</th>
            <th class="amount">金額</th>
          </tr>
        </thead>
        <tbody>
          <c:forEach var="line" items="${bill.lines}">
            <c:set var="d" value="${line.detail}" />
            <tr>
              <td>
                <span class="status-name">
                  <c:out value="${d.productName}" /><c:if test="${not empty d.sizeName}">(<c:out value="${d.sizeName}" />)</c:if>
                </span>
                <%-- キャンセル(請求あり)の商品は、取り消したことが分かるように状態も出す --%>
                <c:if test="${d.canceled}">
                  <span class="tag tag-muted"><c:out value="${d.orderStatus}" /></span>
                </c:if>
                <c:if test="${not empty line.toppings}">
                  <span class="status-toppings">
                    <c:forEach var="t" items="${line.toppings}" varStatus="st">
                      <c:out value="${t.productName}" /><c:if test="${!st.last}">・</c:if>
                    </c:forEach>
                  </span>
                </c:if>
              </td>
              <td class="num">${d.quantity}</td>
              <td class="amount"><fmt:formatNumber value="${line.amount}" />円</td>
            </tr>
          </c:forEach>
        </tbody>
      </table>

      <dl class="register-sum">
        <div><dt>小計</dt><dd><fmt:formatNumber value="${bill.subtotal}" />円</dd></div>
        <div class="register-tax"><dt>(内消費税)</dt><dd><fmt:formatNumber value="${bill.tax}" />円</dd></div>
        <div class="register-total"><dt>合計</dt><dd><fmt:formatNumber value="${bill.total}" />円</dd></div>
      </dl>
    </section>

    <%-- 右:支払方法・預かり金額・おつり・確定 --%>
    <section class="register-payment" aria-label="支払い">
      <form method="post" action="${ctx}/register/pay" id="payForm">
        <input type="hidden" name="orderId" value="${bill.order.id}">

        <fieldset class="register-methods" ${paid ? 'disabled' : ''}>
          <legend class="register-legend">支払方法</legend>
          <div class="segmented" role="radiogroup">
            <c:forEach var="m" items="現金,クレジットカード,電子マネー">
              <label class="segmented-item">
                <input type="radio" name="method" value="${m}" ${m == selected ? 'checked' : ''}>
                <span>${m == 'クレジットカード' ? 'クレジット' : m}</span>
              </label>
            </c:forEach>
          </div>
        </fieldset>

        <div class="register-cash" id="cashArea">
          <label class="register-row" for="received">
            <span>預かり金額</span>
            <span class="register-received">
              <input type="number" name="received" id="received" class="register-input" min="0" step="1"
                     value="<c:out value='${paid ? payment.received : receivedInput}' />" ${paid ? 'disabled' : ''}>
              <span>円</span>
            </span>
          </label>
          <p class="register-row">
            <span>おつり</span>
            <strong class="register-change"><span id="change">${paid ? payment.change : '-'}</span>円</strong>
          </p>
        </div>
        <p class="register-cash-note">※現金以外は預かり金額の入力なし</p>

        <c:choose>
          <c:when test="${not paid}">
            <button type="submit" class="btn btn-primary btn-lg btn-block" id="payButton">会計を確定する</button>
            <button type="button" class="btn btn-secondary btn-lg btn-block" disabled>レシートを発行する</button>
            <p class="register-pay-note">会計確定後、テーブルは「準備中」になります</p>
          </c:when>
          <c:otherwise>
            <p class="alert alert-success">会計を確定しました。テーブル${bill.order.tableNo}は「準備中」になりました。</p>
            <button type="button" class="btn btn-primary btn-lg btn-block" id="receiptButton">レシートを発行する</button>
          </c:otherwise>
        </c:choose>
      </form>
    </section>

    <%-- レシート(印刷するときだけ表示する) --%>
    <c:if test="${paid}">
      <section class="receipt" aria-hidden="true">
        <p class="receipt-shop">喫茶 生成り</p>
        <p><fmt:formatDate value="${payment.paidDate}" pattern="yyyy/MM/dd HH:mm" />　テーブル${bill.order.tableNo}</p>
        <table>
          <c:forEach var="line" items="${bill.lines}">
            <tr>
              <td><c:out value="${line.detail.productName}" /><c:if test="${not empty line.detail.sizeName}">(<c:out value="${line.detail.sizeName}" />)</c:if>
                <c:forEach var="t" items="${line.toppings}"> +<c:out value="${t.productName}" /></c:forEach>
                ×${line.detail.quantity}<c:if test="${line.detail.canceled}"> <c:out value="${line.detail.orderStatus}" /></c:if></td>
              <td class="amount"><fmt:formatNumber value="${line.amount}" />円</td>
            </tr>
          </c:forEach>
        </table>
        <p>合計 <fmt:formatNumber value="${bill.total}" />円(内消費税 <fmt:formatNumber value="${bill.tax}" />円)</p>
        <p><c:out value="${payment.method}" /><c:if test="${not empty payment.received}">　お預かり <fmt:formatNumber value="${payment.received}" />円　おつり <fmt:formatNumber value="${payment.change}" />円</c:if></p>
      </section>
    </c:if>
  </c:if>
</main>

<script>
(function () {
  const total    = ${empty bill ? 0 : bill.total};
  const cashArea = document.getElementById('cashArea');
  const received = document.getElementById('received');
  const change   = document.getElementById('change');
  const payBtn   = document.getElementById('payButton');
  if (!cashArea) return;

  // 現金のときだけ預かり金額を入力する。おつりを計算し、足りないときは確定できない
  function update() {
    const method = document.querySelector('input[name=method]:checked');
    const cash = method && method.value === '現金';
    cashArea.hidden = !cash;
    if (!payBtn) return;
    if (!cash) {
      received.required = false;
      payBtn.disabled = false;
      return;
    }
    received.required = true;
    const value = received.value === '' ? NaN : Number(received.value);
    const ok = !isNaN(value) && value >= total;
    change.textContent = ok ? (value - total).toLocaleString() : '-';
    payBtn.disabled = !ok;
  }
  document.querySelectorAll('input[name=method]').forEach(function (r) {
    r.addEventListener('change', update);
  });
  received.addEventListener('input', update);
  update();

  // レシートを発行(印刷)して R-01 テーブル番号入力画面へ戻る
  const receiptBtn = document.getElementById('receiptButton');
  if (receiptBtn) {
    receiptBtn.addEventListener('click', function () {
      window.print();
      location.href = '${ctx}/register/table';
    });
  }
})();
</script>
</body>
</html>
