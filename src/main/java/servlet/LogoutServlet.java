package servlet;

import java.io.IOException;

// Tomcat11 なので jakarta.* を使う
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * ログアウト(R-01・A-02 の【ログアウト】)
 * セッションを破棄して A-01 ログイン画面へ戻る。同じ端末の種類のログイン画面に戻す
 * 状態を変える操作なので POST で受ける
 */
@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String terminal = LoginServlet.TERMINAL_REGISTER;
        HttpSession session = request.getSession(false);
        if (session != null) {
            if (LoginServlet.TERMINAL_ADMIN.equals(session.getAttribute("loginTerminal"))) {
                terminal = LoginServlet.TERMINAL_ADMIN;
            }
            session.invalidate();
        }
        response.sendRedirect(request.getContextPath() + "/login"
                + (LoginServlet.TERMINAL_ADMIN.equals(terminal) ? "?terminal=admin" : ""));
    }
}
