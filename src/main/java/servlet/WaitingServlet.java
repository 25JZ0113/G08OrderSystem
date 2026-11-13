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

import dao.TerminalDAO;
import model.TableTerminal;

/**
 * T-07 待機画面(Controller)
 * テーブル状態が「準備中」のときに表示する。注文はできない
 * 画面は数秒ごとに自動で再表示し、ホール担当が「空席」に変更したら人数入力画面(T-08)に切り替える
 */
@WebServlet("/waiting")
public class WaitingServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String code = GuestCountServlet.readTerminalCookie(request);

        try {
            TableTerminal terminal =
                    (code == null) ? null : new TerminalDAO().findTableTerminal(code);

            // 準備中でなければ(空席になった・端末が未設定など)入口(T-08)に任せる
            if (terminal == null || !TableTerminal.STATUS_PREPARING.equals(terminal.getTableStatus())) {
                response.sendRedirect(request.getContextPath() + "/guestCount");
                return;
            }

            // 前のお客様の情報を消し、テーブル番号だけ残す
            HttpSession session = request.getSession();
            GuestCountServlet.clearVisit(session);
            session.setAttribute("tableNo", terminal.getTableNo());
        } catch (SQLException e) {
            // 状態を確認できなくても待機画面を出し、次の自動再表示でもう一度確認する
            e.printStackTrace();
        }
        request.getRequestDispatcher("/WEB-INF/jsp/waiting.jsp").forward(request, response);
    }
}
