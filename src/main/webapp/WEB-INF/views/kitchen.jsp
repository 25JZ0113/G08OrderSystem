<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%-- K-01 調理待ち一覧画面(キッチン端末・ログインなし) --%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="ja">
<head>
<%@ include file="common/head.jspf" %>
<%-- 新しい注文が入るので、30 秒ごとに自動で再表示する --%>
<meta http-equiv="refresh" content="30;url=${ctx}/kitchen">
<title>調理待ち一覧 | 喫茶 生成り</title>
</head>
<body>
<header class="app-header">
  <p class="app-title">
    <span class="app-shop">喫茶 生成り</span>
    <span class="app-screen">調理待ち一覧</span>
  </p>
  <div class="app-header-row">
    <p class="table-info">キッチン端末</p>
    <div class="header-actions">
      <%@ include file="common/adminMenuButton.jspf" %>
      <%-- 管理者メニューから開いたときは【管理者メニュー】で戻るので、入口ページへのボタンは出さない --%>
      <c:if test="${!(sessionScope.loginUser.admin && sessionScope.loginTerminal == 'admin')}">
        <a class="btn btn-secondary btn-sm" href="${ctx}/">入口ページに戻る</a>
      </c:if>
    </div>
  </div>
</header>

<main class="status-page kitchen-page">
  <div class="kitchen-heading">
    <h1 class="section-title">注文受付済み・調理中の商品(注文時刻の古い順)</h1>
    <a class="btn btn-secondary btn-sm" href="${ctx}/kitchen">最新の状態にする</a>
  </div>

  <c:if test="${not empty errorMessage}">
    <p class="alert alert-danger"><c:out value="${errorMessage}" /></p>
  </c:if>

  <c:choose>
    <c:when test="${empty lines}">
      <p class="empty status-empty">調理待ちの商品はありません。</p>
    </c:when>
    <c:otherwise>
      <table class="status-table kitchen-table">
        <colgroup>
          <col class="col-time">
          <col class="col-table">
          <col>
          <col class="col-qty">
          <col class="col-state">
          <col class="col-action">
        </colgroup>
        <thead>
          <tr>
            <th class="num">注文時刻</th>
            <th class="num">テーブル</th>
            <th>商品名</th>
            <th class="num">数量</th>
            <th>状態</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <%-- line は OrderLine(商品の明細 + 付いたトッピング) --%>
          <c:forEach var="line" items="${lines}">
            <c:set var="d" value="${line.detail}" />
            <c:set var="cooking" value="${d.orderStatus == '調理中'}" />
            <tr class="${cooking ? 'is-cooking' : ''}">
              <td class="num"><fmt:formatDate value="${d.orderedDatetime}" pattern="HH:mm" /></td>
              <td class="num">${d.tableNo}</td>
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
              <td class="num kitchen-qty">${d.quantity}</td>
              <td>
                <span class="tag ${cooking ? 'tag-accent' : ''}"><c:out value="${d.orderStatus}" /></span>
              </td>
              <td>
                <%-- 変更後の状態はサーバー側で決める。送るのは明細 ID と操作の種類だけ --%>
                <form method="post" action="${ctx}/kitchen">
                  <input type="hidden" name="id" value="${d.id}">
                  <c:choose>
                    <c:when test="${cooking}">
                      <input type="hidden" name="action" value="done">
                      <button type="submit" class="btn btn-primary btn-block">調理済みにする</button>
                    </c:when>
                    <c:otherwise>
                      <input type="hidden" name="action" value="cook">
                      <button type="submit" class="btn btn-secondary btn-block">調理中にする</button>
                    </c:otherwise>
                  </c:choose>
                </form>
              </td>
            </tr>
          </c:forEach>
        </tbody>
      </table>
    </c:otherwise>
  </c:choose>

  <p class="kitchen-note">※調理済みにすると、ホールの提供待ち一覧に表示されます。この画面は 30 秒ごとに自動で最新になります。</p>
</main>
</body>
</html>
