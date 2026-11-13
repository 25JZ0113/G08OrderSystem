<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%-- 入口ページ(http://localhost:8080/OrderSystem/ で開く)。どの端末として使うかを選ぶ --%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="ja">
<head>
<%@ include file="/WEB-INF/jsp/common/head.jspf" %>
<title>端末の選択 | 喫茶 生成り</title>
</head>
<body>
<header class="app-header">
  <p class="app-title">
    <span class="app-shop">喫茶 生成り</span>
    <span class="app-screen">端末の選択</span>
  </p>
</header>

<main class="admin-menu">
  <section aria-labelledby="tableTerminal">
    <h1 class="section-title admin-menu-title" id="tableTerminal">テーブル端末(お客様)</h1>
    <ul class="admin-menu-grid">
      <li>
        <a class="btn btn-primary admin-menu-btn" href="${ctx}/guestCount">注文を始める</a>
        <p class="admin-menu-note">人数入力画面へ(設定済みの端末)</p>
      </li>
      <li>
        <a class="btn btn-secondary admin-menu-btn" href="${ctx}/tableConfirm">テーブル番号の設定</a>
        <p class="admin-menu-note">この端末のテーブル番号を確認・変更</p>
      </li>
    </ul>
  </section>

  <section aria-labelledby="staffTerminal">
    <h2 class="section-title admin-menu-title" id="staffTerminal">社員の端末</h2>
    <ul class="admin-menu-grid">
      <li>
        <a class="btn btn-primary admin-menu-btn" href="${ctx}/hall">ホール</a>
        <p class="admin-menu-note">席状況画面へ(ログインなし)</p>
      </li>
      <li>
        <a class="btn btn-primary admin-menu-btn" href="${ctx}/kitchen">キッチン</a>
        <p class="admin-menu-note">調理待ち一覧画面へ(ログインなし)</p>
      </li>
      <li>
        <a class="btn btn-primary admin-menu-btn" href="${ctx}/login">レジ</a>
        <p class="admin-menu-note">ログインしてテーブル番号入力画面へ</p>
      </li>
    </ul>
  </section>

  <section aria-labelledby="adminTerminal">
    <h2 class="section-title admin-menu-title" id="adminTerminal">管理者端末</h2>
    <ul class="admin-menu-grid">
      <li>
        <a class="btn btn-primary admin-menu-btn" href="${ctx}/login?terminal=admin">管理者メニュー</a>
        <p class="admin-menu-note">管理者でログインして管理者メニューへ</p>
      </li>
    </ul>
    <p class="admin-menu-foot">※テーブル端末を初めて使うときは、先に【テーブル番号の設定】でテーブルを決めてください</p>
  </section>
</main>
</body>
</html>
