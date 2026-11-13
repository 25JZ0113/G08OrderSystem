package servlet;

import java.io.IOException;

// Tomcat11 なので jakarta.* を使う
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * A-02 管理者メニュー画面(Controller)
 * 管理機能(A-03〜A-05)と社員業務(H-01・K-01・R-01)を選ぶ。管理者端末でのみ開ける(AdminFilter で確認)
 */
@WebServlet("/admin/menu")
public class AdminMenuServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/jsp/adminMenu.jsp").forward(request, response);
    }
}
