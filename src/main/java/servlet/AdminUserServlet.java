package servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

// Tomcat11 なので jakarta.* を使う
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import dao.UserDAO;
import model.User;

/**
 * A-03 ユーザー管理画面(Controller)
 * 左:ユーザー一覧【選択】 右:ユーザー情報の入力欄と【登録】【変更】【削除】
 *   GET  /admin/users          … 新規登録(入力欄は空)
 *   GET  /admin/users?id=N     … 選んだユーザーを入力欄に出す
 *   POST action=register / update / delete
 * 自分自身は削除・利用停止・社員への変更ができない(管理者がいなくならないように)
 */
@WebServlet("/admin/users")
public class AdminUserServlet extends HttpServlet {

    private static final int LOGIN_ID_MAX = 20;
    private static final int NAME_MAX     = 30;
    private static final int PASSWORD_MIN = 6;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User form = null;
        String id = request.getParameter("id");
        try {
            if (id != null) {
                form = new UserDAO().findById(Integer.parseInt(id));
                if (form == null) {
                    request.setAttribute("errorMessage", "選んだユーザーはほかの端末で削除されています。");
                }
            }
        } catch (NumberFormatException e) {
            // id が数字でないときは新規登録として表示する
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "ユーザーを読み込めませんでした。");
        }
        setResultMessage(request);
        forward(request, response, form);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");

        // 入力された値(エラーのときはこの値のまま画面に戻す)
        User form = new User();
        form.setId(parseId(request.getParameter("id")));
        form.setLoginId(trim(request.getParameter("loginId")));
        form.setName(trim(request.getParameter("name")));
        form.setRole(request.getParameter("role"));
        form.setStatus(request.getParameter("status"));
        String password = request.getParameter("password");
        if (password != null && password.isEmpty()) {
            password = null;     // 変更のときは空欄ならパスワードを変えない
        }

        User me = (User)request.getSession().getAttribute(LoginServlet.LOGIN_USER);
        UserDAO dao = new UserDAO();
        try {
            switch (action == null ? "" : action) {
                case "register" -> {
                    form.setId(0);
                    String error = validate(form, password, true);
                    if (error == null && dao.existsLoginId(form.getLoginId(), 0)) {
                        error = "ログインID「" + form.getLoginId() + "」はすでに使われています。";
                    }
                    if (error != null) {
                        error(request, response, form, error);
                        return;
                    }
                    int newId = dao.insert(form, password);
                    redirect(request, response, newId, "registered");
                }
                case "update" -> {
                    String error = validate(form, password, false);
                    if (error == null && form.getId() == me.getId()
                            && (!User.ROLE_ADMIN.equals(form.getRole()) || !User.STATUS_ACTIVE.equals(form.getStatus()))) {
                        error = "ログイン中の自分の権限・利用状態は変更できません。";
                    }
                    if (error == null && dao.existsLoginId(form.getLoginId(), form.getId())) {
                        error = "ログインID「" + form.getLoginId() + "」はすでに使われています。";
                    }
                    if (error != null) {
                        error(request, response, form, error);
                        return;
                    }
                    if (!dao.update(form, password)) {
                        redirect(request, response, 0, "gone");
                        return;
                    }
                    // 自分の氏名・ログイン ID を変えたら、ヘッダーの表示も変える
                    if (form.getId() == me.getId()) {
                        refreshLoginUser(request.getSession(), dao.findById(me.getId()));
                    }
                    redirect(request, response, form.getId(), "updated");
                }
                case "delete" -> {
                    if (form.getId() == me.getId()) {
                        error(request, response, form, "ログイン中の自分は削除できません。");
                        return;
                    }
                    if (dao.countCancelRecords(form.getId()) > 0) {
                        error(request, response, form,
                                "このユーザーは注文の取り消しの記録に使われているため削除できません。利用状態を「利用停止」にしてください。");
                        return;
                    }
                    redirect(request, response, 0, dao.delete(form.getId()) ? "deleted" : "gone");
                }
                default -> redirect(request, response, 0, null);
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            // 確認した後にほかの端末で同じログイン ID が登録された
            error(request, response, form, "ログインID「" + form.getLoginId() + "」はすでに使われています。");
        } catch (SQLException e) {
            e.printStackTrace();
            error(request, response, form, "保存できませんでした。もう一度お試しください。");
        }
    }

    /** 入力チェック。問題がなければ null */
    private String validate(User form, String password, boolean isNew) {
        if (form.getLoginId() == null || form.getLoginId().isEmpty()) {
            return "ログインIDを入力してください。";
        }
        if (!form.getLoginId().matches("[A-Za-z0-9_-]{1," + LOGIN_ID_MAX + "}")) {
            return "ログインIDは半角英数字(_ と - も可)" + LOGIN_ID_MAX + "文字以内で入力してください。";
        }
        if (form.getName() == null || form.getName().isEmpty()) {
            return "氏名を入力してください。";
        }
        if (form.getName().length() > NAME_MAX) {
            return "氏名は" + NAME_MAX + "文字以内で入力してください。";
        }
        if (isNew && password == null) {
            return "パスワードを入力してください。";
        }
        if (password != null && password.length() < PASSWORD_MIN) {
            return "パスワードは" + PASSWORD_MIN + "文字以上で入力してください。";
        }
        if (!User.ROLE_ADMIN.equals(form.getRole()) && !User.ROLE_STAFF.equals(form.getRole())) {
            return "権限を選んでください。";
        }
        if (!User.STATUS_ACTIVE.equals(form.getStatus()) && !User.STATUS_STOPPED.equals(form.getStatus())) {
            return "利用状態を選んでください。";
        }
        return null;
    }

    private void refreshLoginUser(HttpSession session, User user) {
        if (user != null) {
            session.setAttribute(LoginServlet.LOGIN_USER, user);
        }
    }

    /** 登録・変更・削除のあとは GET で開き直す(PRG) */
    private void redirect(HttpServletRequest request, HttpServletResponse response, int id, String result)
            throws IOException {
        StringBuilder url = new StringBuilder(request.getContextPath()).append("/admin/users");
        String sep = "?";
        if (id > 0) {
            url.append(sep).append("id=").append(id);
            sep = "&";
        }
        if (result != null) {
            url.append(sep).append("result=").append(result);
        }
        response.sendRedirect(url.toString());
    }

    private void setResultMessage(HttpServletRequest request) {
        String result = request.getParameter("result");
        if (result == null) {
            return;
        }
        switch (result) {
            case "registered" -> request.setAttribute("successMessage", "ユーザーを登録しました。");
            case "updated"    -> request.setAttribute("successMessage", "ユーザー情報を変更しました。");
            case "deleted"    -> request.setAttribute("successMessage", "ユーザーを削除しました。");
            case "gone"       -> request.setAttribute("errorMessage", "そのユーザーはほかの端末ですでに削除されています。");
            default -> { }
        }
    }

    private void error(HttpServletRequest request, HttpServletResponse response, User form, String message)
            throws ServletException, IOException {
        request.setAttribute("errorMessage", message);
        forward(request, response, form);
    }

    private void forward(HttpServletRequest request, HttpServletResponse response, User form)
            throws ServletException, IOException {
        try {
            request.setAttribute("users", new UserDAO().findAll());
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "ユーザー一覧を読み込めませんでした。");
        }
        if (form == null) {
            // 新規登録の初期値:社員・利用中
            form = new User(0, "", "", User.ROLE_STAFF, User.STATUS_ACTIVE);
        }
        request.setAttribute("form", form);
        request.getRequestDispatcher("/WEB-INF/jsp/adminUsers.jsp").forward(request, response);
    }

    private static int parseId(String s) {
        try {
            return s == null ? 0 : Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String trim(String s) {
        return s == null ? null : s.strip();
    }
}
