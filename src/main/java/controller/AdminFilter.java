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
 * 管理者の画面(A-02〜A-05)の前に通るフィルター
 * 管理者端末で管理者がログインしていないときは、管理者端末の A-01 ログイン画面へ移動させる
 * ログイン中に利用停止・削除されたとき、社員に変更されたときは、ログアウトさせる
 */
@WebFilter("/admin/*")
public class AdminFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest  request  = (HttpServletRequest)req;
        HttpServletResponse response = (HttpServletResponse)res;

        boolean adminTerminal = LoginServlet.TERMINAL_ADMIN.equals(LoginFilter.terminalOf(request));
        User user = LoginServlet.refreshLoginUser(request);
        if (user == LoginServlet.STOPPED) {
            LoginServlet.redirectToLogin(request, response, LoginServlet.TERMINAL_ADMIN, true);
            return;
        }
        if (user == null || !adminTerminal) {
            LoginServlet.redirectToLogin(request, response, LoginServlet.TERMINAL_ADMIN, false);
            return;
        }
        if (!user.isAdmin()) {
            // 管理者端末にログイン中に、ほかの管理者に社員へ変更された
            request.getSession().invalidate();
            LoginServlet.redirectToLogin(request, response, LoginServlet.TERMINAL_ADMIN, true);
            return;
        }
        chain.doFilter(req, res);
    }
}
