package controller;

import java.io.IOException;

// Tomcat11 なので jakarta.* を使う
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import model.User;

/**
 * レジの画面(R-01〜R-03)の前に通るフィルター
 * ログインしていないときは A-01 ログイン画面へ移動させる
 * ログイン中に利用停止・削除されたときは、ログアウトさせてログイン画面へ移動させる
 */
@WebFilter("/register/*")
public class LoginFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest  request  = (HttpServletRequest)req;
        HttpServletResponse response = (HttpServletResponse)res;

        // 管理者端末からレジ業務を開いていることもあるので、戻すログイン画面の種類を先に覚えておく
        String terminal = terminalOf(request);
        User user = LoginServlet.refreshLoginUser(request);
        if (user == null || user == LoginServlet.STOPPED) {
            LoginServlet.redirectToLogin(request, response, terminal, user == LoginServlet.STOPPED);
            return;
        }
        chain.doFilter(req, res);
    }

    /** セッションに入っている端末の種類(なければレジ端末) */
    static String terminalOf(HttpServletRequest request) {
        var session = request.getSession(false);
        Object terminal = session == null ? null : session.getAttribute("loginTerminal");
        return LoginServlet.TERMINAL_ADMIN.equals(terminal) ? LoginServlet.TERMINAL_ADMIN : LoginServlet.TERMINAL_REGISTER;
    }
}
