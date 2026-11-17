<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%-- A-01 ログイン画面(レジ端末・管理者端末) --%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="ja">
<head>
<%@ include file="common/head.jspf" %>
<title>ログイン | 喫茶 生成り</title>
</head>
<body>
<header class="app-header">
  <p class="app-title">
    <span class="app-shop">喫茶 生成り</span>
    <span class="app-screen">ログイン</span>
  </p>
  <div class="app-header-row">
    <p class="table-info"><c:out value="${terminalName}" /></p>
    <div class="header-actions">
      <a class="btn btn-secondary btn-sm" href="${ctx}/">入口ページに戻る</a>
    </div>
  </div>
</header>

<main class="welcome-page">
  <h1 class="welcome-title login-title">テーブルオーダーシステム</h1>

  <c:if test="${not empty errorMessage}">
    <p class="alert alert-danger" role="alert"><c:out value="${errorMessage}" /></p>
  </c:if>

  <%-- 端末の種類は URL(?terminal=admin)で決まるので、送信先にも付けておく --%>
  <form class="login-form" method="post"
        action="${ctx}/login${terminal == 'admin' ? '?terminal=admin' : ''}">
    <label for="loginId">ログインID</label>
    <input type="text" name="loginId" id="loginId" class="login-input"
           value="<c:out value='${loginId}' />" required autofocus autocomplete="username">

    <label for="password">パスワード</label>
    <input type="password" name="password" id="password" class="login-input"
           required autocomplete="current-password">

    <button type="submit" class="btn btn-primary btn-lg login-submit">ログイン</button>
  </form>

  <p class="welcome-note">
    ※ログインするのはレジ端末と管理者端末だけです(ホール端末・キッチン端末はログインなし)<br>
    ログイン後の画面:レジ端末はテーブル番号入力画面、管理者端末は管理者メニュー。管理者端末には管理者だけがログインできます
  </p>
</main>
</body>
</html>
