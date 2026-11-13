package servlet;

import java.io.IOException;
import java.sql.SQLException;

// Tomcat11 なので jakarta.* を使う
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import dao.TerminalDAO;
import model.TableTerminal;

/**
 * S-01 テーブル番号確認画面(Controller)
 * 営業開始時にテーブル端末を起動したとき、この端末に設定されているテーブル番号を表示する
 * 【確定する】→ T-08 人数入力画面 / 【変更する】→ S-02 テーブル番号変更画面
 *
 * どのテーブルの端末かは、Cookie「terminalCode」に保存した端末コードで判断する(GuestCountServlet と同じ)
 */
@WebServlet("/tableConfirm")
public class TableConfirmServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String code = GuestCountServlet.readTerminalCookie(request);

        try {
            TableTerminal terminal =
                    (code == null) ? null : new TerminalDAO().findTableTerminal(code);
            if (terminal != null) {
                request.getSession().setAttribute("tableNo", terminal.getTableNo());   // ヘッダーのテーブル番号
                request.setAttribute("terminal", terminal);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "テーブル番号を取得できませんでした。もう一度お試しください。");
        }
        request.getRequestDispatcher("/WEB-INF/jsp/tableConfirm.jsp").forward(request, response);
    }
}
