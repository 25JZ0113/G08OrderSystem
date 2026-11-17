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
import dao.OrderDetailDAO;
import model.Order;
import model.OrderLine;

/**
 * R-01 テーブル番号入力画面(Controller)。レジ端末でログインすると表示する
 * GET : テーブル番号の入力欄とテンキーを表示
 * POST: お客様が持ってきたテーブル番号札の番号で、未会計の注文を探す
 *   未提供あり → R-02 未提供警告画面 / 未提供なし → R-03 会計画面 / 該当なし → エラー
 */
@WebServlet("/register/table")
public class RegisterTableServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // 会計を確定した・ほかの端末で会計済みだったなどのメッセージ
        if ("paid".equals(request.getParameter("result"))) {
            request.setAttribute("errorMessage", "この注文はすでに会計済みです。");
        }
        forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String input = request.getParameter("tableNo");
        int    tableNo;
        try {
            tableNo = Integer.parseInt(input == null ? "" : input.trim());
        } catch (NumberFormatException e) {
            request.setAttribute("errorMessage", "テーブル番号を入力してください。");
            forward(request, response);
            return;
        }

        try {
            Order order = new OrderDAO().findUnpaidByTableNo(tableNo);
            if (order == null) {
                request.setAttribute("errorMessage", "テーブル" + tableNo + "に未会計の注文はありません。");
                request.setAttribute("inputNo", input);
                forward(request, response);
                return;
            }

            // 未提供の商品があるか(キャンセルされていない明細で、提供済みでないもの)
            boolean unserved = false;
            for (OrderLine line : new OrderDetailDAO().findLinesByOrderId(order.getId())) {
                if (line.getDetail().isUnserved()) {
                    unserved = true;
                    break;
                }
            }
            response.sendRedirect(request.getContextPath()
                    + (unserved ? "/register/unserved" : "/register/pay") + "?orderId=" + order.getId());
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "会計内容を取得できませんでした。もう一度お試しください。");
            request.setAttribute("inputNo", input);
            forward(request, response);
        }
    }

    private void forward(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/registerTable.jsp").forward(request, response);
    }
}
