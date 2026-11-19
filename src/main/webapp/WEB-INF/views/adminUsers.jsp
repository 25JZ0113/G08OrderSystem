<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%-- A-03 ユーザー管理画面(管理者端末) --%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="isNew" value="${form.id == 0}" />
<c:set var="isMe" value="${form.id == sessionScope.loginUser.id}" />
<!DOCTYPE html>
<html lang="ja">
<head>
<%@ include file="common/head.jspf" %>
<title>ユーザー管理 | 喫茶 生成り</title>
</head>
<body>
<header class="app-header">
  <p class="app-title">
    <span class="app-shop">喫茶 生成り</span>
    <span class="app-screen">ユーザー管理</span>
  </p>
  <div class="app-header-row">
    <p class="table-info">
      管理者端末
      <span class="register-login">ログイン:<span class="table-info-value"><c:out value="${sessionScope.loginUser.name}" /></span></span>
    </p>
  </div>
</header>

<main class="admin-users">
  <c:if test="${not empty successMessage}">
    <p class="alert alert-success admin-users-alert"><c:out value="${successMessage}" /></p>
  </c:if>
  <c:if test="${not empty errorMessage}">
    <p class="alert alert-danger admin-users-alert" role="alert"><c:out value="${errorMessage}" /></p>
  </c:if>

  <%-- 左:ユーザー一覧 --%>
  <section class="admin-users-list" aria-label="ユーザー一覧">
    <div class="admin-users-actions">
      <a class="btn btn-secondary" href="${ctx}/admin/menu">管理者メニューに戻る</a>
      <a class="btn btn-secondary" href="${ctx}/admin/users">新規登録</a>
    </div>

    <table class="status-table admin-users-table">
      <colgroup>
        <col class="col-login">
        <col>
        <col class="col-role">
        <col class="col-user-status">
        <col class="col-select">
      </colgroup>
      <thead>
        <tr>
          <th>ログインID</th>
          <th>氏名</th>
          <th>権限</th>
          <th>利用状態</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <c:forEach var="u" items="${users}">
          <tr class="${u.id == form.id ? 'is-selected' : ''}">
            <td class="admin-users-login"><c:out value="${u.loginId}" /></td>
            <td>
              <c:out value="${u.name}" />
              <c:if test="${u.id == sessionScope.loginUser.id}"><span class="admin-users-me">(自分)</span></c:if>
            </td>
            <td><c:out value="${u.role}" /></td>
            <td><span class="tag ${u.status == '利用中' ? 'tag-success' : 'tag-muted'}"><c:out value="${u.status}" /></span></td>
            <td>
              <c:choose>
                <c:when test="${u.id == form.id}">
                  <span class="btn btn-primary btn-sm btn-block" aria-current="true">選択中</span>
                </c:when>
                <c:otherwise>
                  <a class="btn btn-secondary btn-sm btn-block" href="${ctx}/admin/users?id=${u.id}">選択</a>
                </c:otherwise>
              </c:choose>
            </td>
          </tr>
        </c:forEach>
      </tbody>
    </table>
  </section>

  <%-- 右:ユーザー情報(新規登録のときは空、選択したときはそのユーザー) --%>
  <section class="admin-users-form" aria-labelledby="userFormTitle">
    <h1 class="section-title" id="userFormTitle">ユーザー情報<span class="admin-users-mode">${isNew ? '(新規登録)' : '(選択中)'}</span></h1>

    <form method="post" action="${ctx}/admin/users" id="userForm">
      <input type="hidden" name="id" value="${form.id}">

      <div class="admin-field">
        <label for="loginId">ログインID</label>
        <input type="text" name="loginId" id="loginId" class="login-input" maxlength="20"
               value="<c:out value='${form.loginId}' />" required autocomplete="off"
               pattern="[A-Za-z0-9_\-]+" title="半角英数字(_ と - も可)">
      </div>

      <div class="admin-field">
        <label for="name">氏名</label>
        <input type="text" name="name" id="name" class="login-input" maxlength="30"
               value="<c:out value='${form.name}' />" required autocomplete="off">
      </div>

      <div class="admin-field">
        <label for="password">パスワード</label>
        <input type="password" name="password" id="password" class="login-input" minlength="6"
               autocomplete="new-password" ${isNew ? 'required' : ''}
               placeholder="${isNew ? '6文字以上' : '変更するときだけ入力'}">
      </div>

      <%-- 自分の権限・利用状態は変えられない(管理者がいなくならないように) --%>
      <div class="admin-field">
        <span id="roleLabel">権限</span>
        <div class="segmented ${isMe ? 'is-locked' : ''}" role="radiogroup" aria-labelledby="roleLabel">
          <c:forEach var="r" items="管理者,社員">
            <label class="segmented-item">
              <input type="radio" name="role" value="${r}" ${r == form.role ? 'checked' : ''} required ${isMe ? 'disabled' : ''}>
              <span>${r}</span>
            </label>
          </c:forEach>
        </div>
      </div>

      <div class="admin-field">
        <span id="statusLabel">利用状態</span>
        <div class="segmented ${isMe ? 'is-locked' : ''}" role="radiogroup" aria-labelledby="statusLabel">
          <c:forEach var="s" items="利用中,利用停止">
            <label class="segmented-item">
              <input type="radio" name="status" value="${s}" ${s == form.status ? 'checked' : ''} required ${isMe ? 'disabled' : ''}>
              <span>${s}</span>
            </label>
          </c:forEach>
        </div>
      </div>
      <c:if test="${isMe}">
        <%-- disabled の欄は送られないので、今の値を hidden で送る --%>
        <input type="hidden" name="role" value="<c:out value='${form.role}' />">
        <input type="hidden" name="status" value="<c:out value='${form.status}' />">
        <p class="admin-users-note">※ログイン中の自分の権限・利用状態は変更できません</p>
      </c:if>

      <div class="admin-users-buttons">
        <button type="submit" name="action" value="register" class="btn btn-primary" ${isNew ? '' : 'disabled'}>登録</button>
        <button type="submit" name="action" value="update" class="btn btn-primary" ${isNew ? 'disabled' : ''}>変更</button>
        <button type="submit" name="action" value="delete" class="btn btn-danger" id="deleteButton"
                formnovalidate ${isNew || isMe ? 'disabled' : ''}
                data-confirm="<c:out value='「${form.name}」を削除します。元に戻せません。よろしいですか?' />">削除</button>
      </div>
      <p class="admin-users-note">
        ${isNew ? '※【新規登録】の状態です。入力して【登録】を押してください' : '※一覧の【新規登録】で、入力欄を空にできます'}
      </p>
    </form>
  </section>
</main>

<script>
<%-- 送信してエラーで戻ってきた画面(POST の結果)は、URL を GET の URL に置き換える。
     そのままだと再読み込みで同じフォームがもう一度送られるため --%>
<c:if test="${pageContext.request.method == 'POST'}">
history.replaceState(null, '', '${pageContext.request.contextPath}/admin/users${form.id > 0 ? "?id=" : ""}${form.id > 0 ? form.id : ""}');
</c:if>

// 【削除】は確認してから送信する
document.getElementById('deleteButton').addEventListener('click', function (e) {
  if (!confirm(this.dataset.confirm)) {
    e.preventDefault();
  }
});
</script>
</body>
</html>
