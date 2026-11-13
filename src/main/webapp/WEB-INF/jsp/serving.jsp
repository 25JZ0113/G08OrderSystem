<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%-- H-03 提供待ち一覧画面(ホール端末・ログインなし) --%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="ja">
<head>
<%@ include file="common/head.jspf" %>
<%-- キッチンで調理済みになった商品が増えるので、30 秒ごとに自動で再表示する --%>
<meta http-equiv="refresh" content="30;url=${ctx}/serving">
<title>提供待ち一覧 | 喫茶 生成り</title>
</head>
<body>
<header class="app-header">
  <p class="app-title">
    <span class="app-shop">喫茶 生成り</span>
    <span class="app-screen">提供待ち一覧</span>
  </p>
  <div class="app-header-row">
    <p class="table-info">ホール端末</p>
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
    <div class="heading-left">
      <a class="btn btn-secondary btn-sm" href="${ctx}/hall">席状況に戻る</a>
      <h1 class="section-title">調理済み(提供待ち)の商品</h1>
    </div>
    <a class="btn btn-secondary btn-sm" href="${ctx}/serving">最新の状態にする</a>
  </div>

  <c:if test="${not empty errorMessage}">
    <p class="alert alert-danger"><c:out value="${errorMessage}" /></p>
  </c:if>

  <c:choose>
    <c:when test="${empty lines}">
      <p class="empty status-empty">提供待ちの商品はありません。</p>
    </c:when>
    <c:otherwise>
      <table class="status-table kitchen-table">
        <colgroup>
          <col class="col-table">
          <col>
          <col class="col-qty">
          <col class="col-time">
          <col class="col-action">
        </colgroup>
        <thead>
          <tr>
            <th class="num">テーブル番号</th>
            <th>商品名</th>
            <th class="num">数量</th>
            <th class="num">調理完了時刻</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <%-- line は OrderLine(商品の明細 + 付いたトッピング) --%>
          <c:forEach var="line" items="${lines}">
            <c:set var="d" value="${line.detail}" />
            <tr>
              <td class="num kitchen-qty">${d.tableNo}</td>
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
              <td class="num"><fmt:formatDate value="${d.cookedDatetime}" pattern="HH:mm" /></td>
              <td>
                <%-- 変更後の状態はサーバー側で決める。送るのは明細 ID だけ --%>
                <form method="post" action="${ctx}/serving">
                  <input type="hidden" name="id" value="${d.id}">
                  <button type="submit" class="btn btn-primary btn-block">提供済みにする</button>
                </form>
              </td>
            </tr>
          </c:forEach>
        </tbody>
      </table>
    </c:otherwise>
  </c:choose>

  <p class="kitchen-note">※提供済みにすると、お客様の注文状況画面にも反映されます。この画面は 30 秒ごとに自動で最新になります。</p>
</main>
</body>
</html>
