<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%-- T-07 待機画面 --%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="screenTitle" value="待機" />
<c:set var="showActions" value="${false}" />
<c:set var="showHomeLink" value="${true}" />
<!DOCTYPE html>
<html lang="ja">
<head>
<%@ include file="common/head.jspf" %>
<%-- 5 秒ごとにテーブル状態を確認する。空席になっていればサーブレットが人数入力画面へ移動させる --%>
<meta http-equiv="refresh" content="5;url=${ctx}/waiting">
<title>ただいま準備中です | 喫茶 生成り</title>
</head>
<body>
<%@ include file="common/tableHeader.jspf" %>

<main class="welcome-page">
  <h2 class="welcome-title">ただいま準備中です</h2>
  <p class="welcome-lead">スタッフがテーブルを準備しています</p>
  <p class="welcome-note">準備が終わると人数入力画面が表示されます</p>
</main>
</body>
</html>
