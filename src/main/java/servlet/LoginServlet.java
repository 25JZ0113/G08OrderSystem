package servlet;

import java.io.IOException;
import java.sql.SQLException;

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
 * A-01 ログイン画面(Controller)
 * ログインするのはレジ端末と管理者端末だけ(ホール端末・キッチン端末はログインなし)
 * 端末の種類は開く URL で判断する
 *   レジ端末  :login(または login?terminal=register)→ ログイン後 R-01 テーブル番号入力画面
 *   管理者端末:login?terminal=admin → ログイン後 A-02 管理者メニュー(管理者だけログインできる)
 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    /** 端末の種類 */
    public static final String TERMINAL_REGISTER = "register";
    public static final String TERMINAL_ADMIN    = "admin";

    /** セッションに入れるログイン中のユーザー */
    public static final String LOGIN_USER = "loginUser";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // ログイン中に利用停止・削除されて、ログアウトさせられたとき(LoginFilter・AdminFilter から)
        if ("stopped".equals(request.getParameter("result"))) {
            request.setAttribute("errorMessage", "このユーザーは利用できなくなったため、ログアウトしました。");
        }
        forward(request, response, terminalOf(request));
    }

    /**
     * 画面を開くたびに LoginFilter・AdminFilter から呼ぶ。ログインの状態を確かめて、次のどれかを返す
     *   null      … ログインしていない
     *   STOPPED   … ログイン中に削除・利用停止された(セッションは破棄してログアウトさせる)
     *   それ以外  … DB から読み直したユーザー(氏名・権限の変更もすぐに反映するため、セッションにも入れ直す)
     * 管理者がユーザー管理で利用停止にしたら、その人の次の操作からすぐに使えなくするため
     */
    public static User refreshLoginUser(HttpServletRequest request) throws ServletException {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute(LOGIN_USER) instanceof User user)) {
            return null;
        }
        try {
            User latest = new UserDAO().findById(user.getId());
            if (latest == null || !User.STATUS_ACTIVE.equals(latest.getStatus())) {
                session.invalidate();
                return STOPPED;
            }
            session.setAttribute(LOGIN_USER, latest);
            return latest;
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    /** refreshLoginUser の結果:ログイン中に使えなくなった */
    public static final User STOPPED = new User();

    /** ログイン画面へ戻す(使えなくなったときはメッセージ付き) */
    public static void redirectToLogin(HttpServletRequest request, HttpServletResponse response,
            String terminal, boolean stopped) throws IOException {
        StringBuilder url = new StringBuilder(request.getContextPath()).append("/login");
        String sep = "?";
        if (TERMINAL_ADMIN.equals(terminal)) {
            url.append(sep).append("terminal=admin");
            sep = "&";
        }
        if (stopped) {
            url.append(sep).append("result=stopped");
        }
        response.sendRedirect(url.toString());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String terminal = terminalOf(request);
        String loginId  = request.getParameter("loginId");
        String password = request.getParameter("password");

        if (loginId == null || loginId.isBlank() || password == null || password.isEmpty()) {
            request.setAttribute("errorMessage", "ログインIDとパスワードを入力してください。");
            request.setAttribute("loginId", loginId);
            forward(request, response, terminal);
            return;
        }

        try {
            User user = new UserDAO().login(loginId.trim(), password);
            if (user == null) {
                // ID・パスワード違い・利用停止(どれかは教えない)
                request.setAttribute("errorMessage", "ログインIDまたはパスワードが正しくありません。");
                request.setAttribute("loginId", loginId);
                forward(request, response, terminal);
                return;
            }
            if (TERMINAL_ADMIN.equals(terminal) && !user.isAdmin()) {
                request.setAttribute("errorMessage", "この端末は管理者専用です。");
                request.setAttribute("loginId", loginId);
                forward(request, response, terminal);
                return;
            }

            // ログインしたらセッション ID を変える(セッション固定攻撃の対策)
            HttpSession session = request.getSession();
            request.changeSessionId();
            session.setAttribute(LOGIN_USER, user);
            session.setAttribute("loginTerminal", terminal);

            // 管理者端末 → A-02 管理者メニュー / レジ端末 → R-01 テーブル番号入力
            response.sendRedirect(request.getContextPath()
                    + (TERMINAL_ADMIN.equals(terminal) ? "/admin/menu" : "/register/table"));
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "ログインできませんでした。もう一度お試しください。");
            request.setAttribute("loginId", loginId);
            forward(request, response, terminal);
        }
    }

    /** URL の terminal パラメーターから端末の種類を決める(指定がなければレジ端末) */
    private String terminalOf(HttpServletRequest request) {
        return TERMINAL_ADMIN.equals(request.getParameter("terminal")) ? TERMINAL_ADMIN : TERMINAL_REGISTER;
    }

    private void forward(HttpServletRequest request, HttpServletResponse response, String terminal)
            throws ServletException, IOException {
        request.setAttribute("terminal", terminal);
        request.setAttribute("terminalName", TERMINAL_ADMIN.equals(terminal) ? "管理者端末" : "レジ端末");
        request.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(request, response);
    }
}
