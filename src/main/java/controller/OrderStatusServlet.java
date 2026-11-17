package controller;

import java.io.IOException;
import java.sql.SQLException;

// Tomcat11 なので jakarta.* を使う
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import dao.OrderDetailDAO;

/**
 * T-04 注文状況画面(Controller)
 * 今の来店(セッションの注文 ID)の注文明細と、それぞれの注文状態を表示する
 */
@WebServlet("/orderStatus")
public class OrderStatusServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer orderId = (Integer)request.getSession().getAttribute("orderId");   // TableSessionFilter で確認済み

        try {
            request.setAttribute("lines", new OrderDetailDAO().findLinesByOrderId(orderId));
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "注文状況を取得できませんでした。もう一度お試しください。");
        }
        request.getRequestDispatcher("/WEB-INF/views/orderStatus.jsp").forward(request, response);
    }
}
