package controller;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

// Tomcat11 なので jakarta.* を使う
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import dao.OrderDetailDAO;
import model.OrderLine;

/**
 * T-05 注文履歴画面(Controller)
 * 今の来店(セッションの注文 ID)で注文した商品・数量・金額と、合計金額を表示する
 */
@WebServlet("/orderHistory")
public class OrderHistoryServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer orderId = (Integer)request.getSession().getAttribute("orderId");   // TableSessionFilter で確認済み

        try {
            List<OrderLine> lines = new OrderDetailDAO().findLinesByOrderId(orderId);
            int total = 0;
            for (OrderLine line : lines) {
                total += line.getAmount();
            }
            request.setAttribute("lines", lines);
            request.setAttribute("total", total);
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "注文履歴を取得できませんでした。もう一度お試しください。");
        }
        request.getRequestDispatcher("/WEB-INF/views/orderHistory.jsp").forward(request, response);
    }
}
