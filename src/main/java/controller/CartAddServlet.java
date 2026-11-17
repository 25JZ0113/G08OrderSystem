package controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Tomcat11 なので jakarta.* を使う
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import dao.ProductDAO;
import model.Cart;
import model.CartItem;
import model.Product;

/**
 * 「カートに入れる」の処理(T-02 から POST)
 * 受け取るもの: productId(サイズを選んだ商品 ID)、toppingId(複数)、quantity、
 * editLine(カートから選び直したときの行番号。新規追加のときはなし)
 * 金額は画面から受け取らず、DB の価格で計算する
 */
@WebServlet("/cartAdd")
public class CartAddServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String ctx = request.getContextPath();

        int productId = toInt(request.getParameter("productId"), -1);
        int quantity  = toInt(request.getParameter("quantity"), 1);
        quantity = Math.max(1, Math.min(Cart.MAX_QUANTITY, quantity));
        int editLine = toInt(request.getParameter("editLine"), 0);

        try {
            ProductDAO dao     = new ProductDAO();
            Product    product = dao.findById(productId);

            // 存在しない・販売停止・トッピング自体は追加しない
            if (product == null
                    || !ProductDAO.ON_SALE.equals(product.getSalesStatus())
                    || ProductDAO.TOPPING_CATEGORY.equals(product.getCategoryName())) {
                response.sendRedirect(ctx + "/productList");
                return;
            }

            // トッピングは「この商品に設定されているもの」だけを受け付ける
            List<Product> allowed       = dao.findToppings(product.getId());
            String[]      toppingParams = request.getParameterValues("toppingId");
            List<Product> toppings      = new ArrayList<>();
            for (Product t : allowed) {                 // allowed は ID 順なので順番がそろう
                if (toppingParams != null) {
                    for (String s : toppingParams) {
                        if (toInt(s, -1) == t.getId()) {
                            toppings.add(t);
                            break;
                        }
                    }
                }
            }

            Cart     cart = CartServlet.getCart(request.getSession());
            CartItem item = new CartItem(product, toppings, quantity);

            if (editLine > 0) {
                // カートから選び直した → その行を置き換えてカート画面へ戻る
                cart.replace(editLine, item);
                response.sendRedirect(ctx + "/cart");
            }
            else {
                // 新規追加 → 商品のカテゴリのメニュー画面へ戻る(T-02 → T-01)
                cart.add(item);
                String category =
                        URLEncoder.encode(product.getCategoryName(), StandardCharsets.UTF_8);
                response.sendRedirect(ctx + "/productList?category=" + category + "&added=1");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "カートに追加できませんでした。もう一度お試しください。");
            request.getRequestDispatcher("/WEB-INF/views/cart.jsp").forward(request, response);
        }
    }

    /** 数値に変換する。変換できなければ def を返す */
    private int toInt(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
