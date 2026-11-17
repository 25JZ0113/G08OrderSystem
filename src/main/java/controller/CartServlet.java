package controller;

import java.io.IOException;

// Tomcat11 なので jakarta.* を使う
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import model.Cart;

/**
 * T-03 カート画面(Controller)
 * GET : カート画面を表示する
 * POST: 数量の増減・削除をして、カート画面を表示し直す
 * action=plus / minus / delete、line=行番号
 */
@WebServlet("/cart")
public class CartServlet extends HttpServlet {

    /** セッションからカートを取り出す(なければ作って保存する) */
    public static Cart getCart(HttpSession session) {
        Cart cart = (Cart)session.getAttribute("cart");
        if (cart == null) {
            cart = new Cart();
            session.setAttribute("cart", cart);
        }
        return cart;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        getCart(request.getSession());   // 空でも表示できるように作っておく
        request.getRequestDispatcher("/WEB-INF/views/cart.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Cart   cart   = getCart(request.getSession());
        String action = request.getParameter("action");
        int    line;
        try {
            line = Integer.parseInt(request.getParameter("line"));
        } catch (NumberFormatException e) {
            line = 0;
        }

        if ("plus".equals(action)) {
            cart.changeQuantity(line, 1);
        }
        else if ("minus".equals(action)) {
            cart.changeQuantity(line, -1);
        }
        else if ("delete".equals(action)) {
            cart.remove(line);
        }

        // 再読み込みで同じ操作が繰り返されないように、GET でカート画面を開き直す
        response.sendRedirect(request.getContextPath() + "/cart");
    }
}
