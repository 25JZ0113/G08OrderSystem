<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%-- A-05 売上管理画面(管理者端末) --%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="isProduct" value="${view == 'product'}" />
<c:choose>
  <c:when test="${view == 'weekday'}"><c:set var="viewName" value="曜日別" /><c:set var="labelHead" value="曜日" /></c:when>
  <c:when test="${view == 'month'}"><c:set var="viewName" value="月別" /><c:set var="labelHead" value="月" /></c:when>
  <c:when test="${isProduct}"><c:set var="viewName" value="商品別" /><c:set var="labelHead" value="商品名" /></c:when>
  <c:otherwise><c:set var="viewName" value="日別" /><c:set var="labelHead" value="日付" /></c:otherwise>
</c:choose>
<!DOCTYPE html>
<html lang="ja">
<head>
<%@ include file="common/head.jspf" %>
<title>売上管理 | 喫茶 生成り</title>
<%-- 期間の日付入力を、画面に合わせたカレンダーにする(JavaScript が動かないときはブラウザ標準の日付入力のまま) --%>
<script src="${ctx}/js/calendar.js" defer></script>
</head>
<body>
<header class="app-header">
  <p class="app-title">
    <span class="app-shop">喫茶 生成り</span>
    <span class="app-screen">売上管理</span>
  </p>
  <div class="app-header-row">
    <p class="table-info">
      管理者端末
      <span class="register-login">ログイン:<span class="table-info-value"><c:out value="${sessionScope.loginUser.name}" /></span></span>
    </p>
  </div>
</header>

<main class="admin-sales">
  <%-- 期間と【集計】(見るだけなので GET。表示中の切り替えも引き継ぐ) --%>
  <form method="get" action="${ctx}/admin/sales" class="admin-sales-period">
    <a class="btn btn-secondary" href="${ctx}/admin/menu">管理者メニューに戻る</a>
    <input type="hidden" name="view" value="${view}">
    <label for="from">期間</label>
    <input type="date" name="from" id="from" class="login-input" value="${from}" required data-calendar>
    <span aria-hidden="true">〜</span>
    <input type="date" name="to" id="to" class="login-input" value="${to}" required aria-label="期間の終了日" data-calendar>
    <button type="submit" class="btn btn-primary">集計</button>
  </form>

  <c:if test="${not empty errorMessage}">
    <p class="alert alert-danger" role="alert"><c:out value="${errorMessage}" /></p>
  </c:if>

  <%-- 日別・曜日別・月別・商品別の切り替え --%>
  <nav class="admin-sales-tabs" aria-label="集計の単位">
    <c:forEach var="v" items="day:日別,weekday:曜日別,month:月別,product:商品別">
      <c:set var="key" value="${v.split(':')[0]}" />
      <a href="${ctx}/admin/sales?from=${from}&to=${to}&view=${key}"
         class="${key == view ? 'is-current' : ''}" ${key == view ? 'aria-current="page"' : ''}>${v.split(':')[1]}</a>
    </c:forEach>
  </nav>

  <c:if test="${not empty summary}">
    <%-- 売上状況 --%>
    <dl class="admin-sales-kpis">
      <div><dt>総売上</dt><dd><fmt:formatNumber value="${summary.amount}" /><small>円</small></dd></div>
      <div><dt>総客数</dt><dd><fmt:formatNumber value="${summary.guests}" /><small>人</small></dd></div>
      <div><dt>総来店数</dt><dd><fmt:formatNumber value="${summary.visits}" /><small>組</small></dd></div>
      <div><dt>客単価</dt><dd><fmt:formatNumber value="${perGuest}" /><small>円</small></dd></div>
      <div><dt>1来店あたり単価</dt><dd><fmt:formatNumber value="${perVisit}" /><small>円</small></dd></div>
    </dl>

    <div class="admin-sales-body">
      <%-- 集計表 --%>
      <section class="admin-sales-table-wrap" aria-label="<c:out value='${viewName}' />の集計表">
        <div class="admin-table-scroll admin-sales-scroll">
          <table class="status-table admin-sales-table">
            <thead>
              <tr>
                <th><c:out value="${labelHead}" /></th>
                <c:choose>
                  <c:when test="${isProduct}"><th class="amount">数量</th></c:when>
                  <c:otherwise><th class="amount">客数</th></c:otherwise>
                </c:choose>
                <th class="amount">売上金額</th>
              </tr>
            </thead>
            <tbody>
              <c:forEach var="r" items="${rows}">
                <tr>
                  <td><c:out value="${r.label}" /></td>
                  <td class="amount">${isProduct ? r.quantity : r.guests}${isProduct ? '' : '人'}</td>
                  <td class="amount"><fmt:formatNumber value="${r.amount}" />円</td>
                </tr>
              </c:forEach>
              <c:if test="${empty rows}">
                <tr><td colspan="3" class="admin-empty">この期間の売上はありません</td></tr>
              </c:if>
            </tbody>
            <tfoot>
              <tr>
                <th>合計</th>
                <th class="amount">${isProduct ? totalQuantity : totalGuests}${isProduct ? '' : '人'}</th>
                <th class="amount"><fmt:formatNumber value="${totalAmount}" />円</th>
              </tr>
            </tfoot>
          </table>
        </div>
      </section>

      <%-- 棒グラフ(1 系列なので凡例はなく、見出しで内容を示す。値は表と、棒に重ねたときの吹き出しで見る) --%>
      <section class="admin-sales-chart" aria-labelledby="chartTitle">
        <h2 class="section-title" id="chartTitle">売上金額(${viewName}<c:if test="${isProduct && rows.size() > chartRows.size()}">・上位${chartRows.size()}件</c:if>)</h2>
        <c:choose>
          <c:when test="${empty rows || totalAmount == 0}">
            <p class="admin-empty admin-chart-empty">この期間の売上はありません</p>
          </c:when>
          <c:when test="${isProduct}">
            <%-- 商品別:商品名が長いので横向きの棒 --%>
            <ul class="hbar-chart">
              <c:forEach var="r" items="${chartRows}">
                <li class="hbar-row">
                  <span class="hbar-label"><c:out value="${r.label}" /></span>
                  <span class="hbar-track">
                    <span class="hbar" style="width: ${r.barPercent}%"></span>
                  </span>
                  <span class="hbar-value"><fmt:formatNumber value="${r.amount}" />円</span>
                </li>
              </c:forEach>
            </ul>
          </c:when>
          <c:otherwise>
            <%-- 日別・曜日別・月別:縦向きの棒。日が多いときは目盛りの文字を間引く --%>
            <c:set var="n" value="${rows.size()}" />
            <c:set var="step" value="${n <= 12 ? 1 : (n <= 31 ? 5 : 10)}" />
            <div class="vbar-chart" style="--bar-count: ${n}">
              <c:forEach var="r" items="${rows}" varStatus="st">
                <div class="vbar-col" tabindex="0"
                     aria-label="<c:out value='${r.label}' /> <fmt:formatNumber value='${r.amount}' />円">
                  <span class="vbar-area">
                    <span class="vbar" style="height: ${r.barPercent}%"></span>
                  </span>
                  <span class="vbar-label"><c:out value="${st.index % step == 0 || st.last ? r.label : ''}" /></span>
                  <span class="vbar-tip" role="tooltip">
                    <c:out value="${r.label}" /><br>
                    <strong><fmt:formatNumber value="${r.amount}" />円</strong><br>
                    ${r.guests}人・${r.visits}組
                  </span>
                </div>
              </c:forEach>
            </div>
          </c:otherwise>
        </c:choose>
      </section>
    </div>
    <p class="admin-users-note">※会計済みの注文を、会計した日で集計しています(未会計の注文は入りません)。取り消した商品は売上に入れていません(会計でも請求しません)。客単価・1来店あたり単価は円未満を切り捨てています</p>
  </c:if>
</main>
</body>
</html>
