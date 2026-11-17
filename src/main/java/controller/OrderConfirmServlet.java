package controller;

import java.io.IOException;
import java.sql.SQLException;

// Tomcat11 なので jakarta.* を使う
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import dao.OrderDetailDAO;
import dao.OrderDetailDAO.OrderConfirmException;
import model.Cart;

/**
 * 「注文を確定する」の処理(T-03 から POST)
 * カートの内容を注文明細に登録し、カートを空にしてメニュー画面へ戻る(T-03 → T-01)
 * 確定できなかったときは、理由をカート画面に表示する
 */
@WebServlet("/orderConfirm")
public class OrderConfirmServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        Cart        cart    = CartServlet.getCart(session);
        Integer     orderId = (Integer)session.getAttribute("orderId");   // TableSessionFilter で確認済み

        // カートが空(二重送信・再読み込みなど)のときは何もしない
        if (cart.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/cart");
            return;
        }

        try {
            new OrderDetailDAO().confirm(orderId, cart);
        } catch (OrderConfirmException e) {
            request.setAttribute("errorMessage", e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/cart.jsp").forward(request, response);
            return;
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "注文を確定できませんでした。もう一度お試しいただくか、スタッフをお呼びください。");
            request.getRequestDispatcher("/WEB-INF/views/cart.jsp").forward(request, response);
            return;
        }

        // 確定したらカートを空にする(確定済みの注文はカートから変更できない)
        session.removeAttribute("cart");
        response.sendRedirect(request.getContextPath() + "/productList?ordered=1");
    }
}
