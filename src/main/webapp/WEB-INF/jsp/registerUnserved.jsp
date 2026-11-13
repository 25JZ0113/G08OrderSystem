<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%-- R-02 未提供警告画面(レジ端末) --%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="screenTitle" value="未提供警告" />
<c:set var="headerOrder" value="${order}" />
<!DOCTYPE html>
<html lang="ja">
<head>
<%@ include file="common/head.jspf" %>
<title>未提供警告 | 喫茶 生成り</title>
</head>
<body>
<%@ include file="common/registerHeader.jspf" %>

<main class="status-page kitchen-page">
  <c:if test="${not empty successMessage}">
    <p class="alert alert-success"><c:out value="${successMessage}" /></p>
  </c:if>
  <c:if test="${not empty errorMessage}">
    <p class="alert alert-danger" role="alert"><c:out value="${errorMessage}" /></p>
  </c:if>

  <c:choose>
    <c:when test="${not empty unserved}">
      <p class="checkout-warning">未提供の商品があります(取り消すと会計できます)</p>

      <table class="status-table kitchen-table">
        <colgroup>
          <col>
          <col class="col-qty">
          <col class="col-state">
          <col class="col-action">
        </colgroup>
        <thead>
          <tr>
            <th>商品名</th>
            <th class="num">数量</th>
            <th>状態</th>
            <th>操作</th>
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
              <td><span class="tag"><c:out value="${d.orderStatus}" /></span></td>
              <td>
                <%-- 表示していた状態も送る(その間に調理が始まっていたら取り消さない) --%>
                <c:set var="confirmText"
                       value="「${d.productName}」を取り消します。料金は請求しません。よろしいですか?" />
                <form method="post" action="${ctx}/register/unserved" class="cancel-form"
                      data-confirm="<c:out value='${confirmText}' />">
                  <input type="hidden" name="orderId" value="${order.id}">
                  <input type="hidden" name="detailId" value="${d.id}">
                  <input type="hidden" name="status" value="<c:out value='${d.orderStatus}' />">
                  <button type="submit" class="btn btn-secondary btn-block">取り消す</button>
                </form>
              </td>
            </tr>
          </c:forEach>
        </tbody>
      </table>

      <p class="kitchen-note">
        ※取り消した商品は、調理中・調理済みでも料金を請求しません<br>
        ※取り消した社員と日時を記録します(管理者の承認は不要)
      </p>
    </c:when>
    <c:otherwise>
      <p class="alert alert-success">未提供の商品はありません。会計へ進めます。</p>
    </c:otherwise>
  </c:choose>

  <div class="register-actions">
    <a class="btn btn-secondary btn-lg" href="${ctx}/register/table">テーブル番号入力に戻る</a>
    <div class="register-action-with-note">
      <c:choose>
        <c:when test="${empty unserved && not empty order}">
          <a class="btn btn-primary btn-lg" href="${ctx}/register/pay?orderId=${order.id}">会計へ進む</a>
        </c:when>
        <c:otherwise>
          <button type="button" class="btn btn-primary btn-lg" disabled>会計へ進む</button>
          <span class="register-action-note">未提供がなくなると押せます</span>
        </c:otherwise>
      </c:choose>
    </div>
  </div>
</main>

<script>
// 【取り消す】は確認してから送信する
document.querySelectorAll('.cancel-form').forEach(function (f) {
  f.addEventListener('submit', function (e) {
    if (!confirm(f.dataset.confirm)) {
      e.preventDefault();
    }
  });
});
</script>
</body>
</html>
