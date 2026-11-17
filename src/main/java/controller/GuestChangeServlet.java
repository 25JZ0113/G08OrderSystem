package controller;

import java.io.IOException;
import java.sql.SQLException;

// Tomcat11 なので jakarta.* を使う
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import dao.OrderDAO;
import dao.TableDAO;
import model.Order;
import model.TableSeat;

/**
 * H-02 人数変更画面(Controller)
 * H-01 席状況画面で利用中のテーブルを選ぶと表示する
 * GET : 選択したテーブル番号と現在の人数を表示 (guestChange?tableId=1)
 * POST: 人数だけを変更して H-01 に戻る。テーブル状態とテーブル端末の画面は変わらない
 */
@WebServlet("/guestChange")
public class GuestChangeServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int tableId;
        try {
            tableId = Integer.parseInt(request.getParameter("tableId"));
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/hall");
            return;
        }

        try {
            TableSeat seat  = new TableDAO().findSeat(tableId);
            Order     order = new OrderDAO().findUnpaidByTableId(tableId);
            // 利用中でない・今の来店がない(会計が済んだなど)ときは席状況に戻す
            if (seat == null || !seat.isInUse() || order == null) {
                response.sendRedirect(request.getContextPath() + "/hall?result=conflict");
                return;
            }
            request.setAttribute("seat", seat);
            request.setAttribute("order", order);
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "テーブル情報を取得できませんでした。もう一度お試しください。");
        }
        request.setAttribute("maxGuests", GuestCountServlet.MAX_GUESTS);
        request.getRequestDispatcher("/WEB-INF/views/guestChange.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        int orderId;
        int tableNo;
        int guestCount;
        try {
            orderId    = Integer.parseInt(request.getParameter("orderId"));
            tableNo    = Integer.parseInt(request.getParameter("tableNo"));   // メッセージ表示用
            guestCount = Integer.parseInt(request.getParameter("guestCount"));
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/hall");
            return;
        }

        // 人数は 1〜10 人(T-08 人数入力と同じ)
        if (guestCount < 1 || guestCount > GuestCountServlet.MAX_GUESTS) {
            response.sendRedirect(request.getContextPath() + "/hall");
            return;
        }

        try {
            boolean changed = new OrderDAO().changeGuestCount(orderId, guestCount);
            // PRG:変更後は席状況(GET)にリダイレクトする
            response.sendRedirect(request.getContextPath() + "/hall"
                    + (changed
                            ? "?result=guestChanged&no=" + tableNo + "&count=" + guestCount
                            : "?result=conflict"));
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/hall?result=error");
        }
    }
}
