<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%-- A-02 管理者メニュー画面(管理者端末) --%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="ja">
<head>
<%@ include file="common/head.jspf" %>
<title>管理者メニュー | 喫茶 生成り</title>
</head>
<body>
<header class="app-header">
  <p class="app-title">
    <span class="app-shop">喫茶 生成り</span>
    <span class="app-screen">管理者メニュー</span>
  </p>
  <div class="app-header-row">
    <p class="table-info">
      管理者端末
      <span class="register-login">ログイン:<span class="table-info-value"><c:out value="${sessionScope.loginUser.name}" /></span></span>
    </p>
    <div class="header-actions">
      <form method="post" action="${ctx}/logout">
        <button type="submit" class="btn btn-secondary btn-sm">ログアウト</button>
      </form>
    </div>
  </div>
</header>

<main class="admin-menu">
  <section aria-labelledby="adminFunctions">
    <h1 class="section-title admin-menu-title" id="adminFunctions">管理機能</h1>
    <ul class="admin-menu-grid">
      <li>
        <a class="btn btn-primary admin-menu-btn" href="${ctx}/admin/users">ユーザー管理</a>
        <p class="admin-menu-note">ユーザーの登録・参照・変更・削除</p>
      </li>
      <li>
        <a class="btn btn-primary admin-menu-btn" href="${ctx}/admin/products">メニュー(商品)管理</a>
        <p class="admin-menu-note">カテゴリ・商品の登録・参照・変更・削除</p>
      </li>
      <li>
        <a class="btn btn-primary admin-menu-btn" href="${ctx}/admin/sales">売上管理</a>
        <p class="admin-menu-note">期間を指定して売上を集計・分析</p>
      </li>
    </ul>
  </section>

  <section aria-labelledby="staffWork">
    <h2 class="section-title admin-menu-title" id="staffWork">社員業務</h2>
    <ul class="admin-menu-grid">
      <li>
        <a class="btn btn-secondary admin-menu-btn" href="${ctx}/hall">ホール業務</a>
        <p class="admin-menu-note">席状況画面へ</p>
      </li>
      <li>
        <a class="btn btn-secondary admin-menu-btn" href="${ctx}/kitchen">キッチン業務</a>
        <p class="admin-menu-note">調理待ち一覧画面へ</p>
      </li>
      <li>
        <a class="btn btn-secondary admin-menu-btn" href="${ctx}/register/table">レジ業務</a>
        <p class="admin-menu-note">テーブル番号入力画面へ</p>
      </li>
    </ul>
    <p class="admin-menu-foot">※社員業務の画面のヘッダーにある【管理者メニュー】で、この画面に戻れます</p>
  </section>
</main>
</body>
</html>
