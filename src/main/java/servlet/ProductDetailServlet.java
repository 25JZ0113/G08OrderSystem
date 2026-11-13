package servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

// Tomcat11 なので jakarta.* を使う
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import dao.ProductDAO;
import model.CartItem;
import model.Product;

/**
 * T-02 商品詳細画面(Controller)
 * productDetail?id=商品ID で呼ばれ、商品・サイズ違い・トッピングを JSP に渡す
 */
@WebServlet("/productDetail")
public class ProductDetailServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // パラメータ id を数値にする。不正な値なら一覧へ戻す
        int id;
        try {
            id = Integer.parseInt(request.getParameter("id"));
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/productList");
            return;
        }

        try {
            ProductDAO dao     = new ProductDAO();
            Product    product = dao.findById(id);

            // 存在しない・販売停止・トッピング自体の場合は一覧へ戻す
            if (product == null
                    || !ProductDAO.ON_SALE.equals(product.getSalesStatus())
                    || ProductDAO.TOPPING_CATEGORY.equals(product.getCategoryName())) {
                response.sendRedirect(request.getContextPath() + "/productList");
                return;
            }

            // サイズ違い(サイズのない商品は自分 1 件だけ)
            List<Product> variants = dao.findVariants(product.getCategoryId(), product.getName());
            // 付けられるトッピング(なければ空のリスト)
            List<Product> toppings = dao.findToppings(product.getId());

            // カートから選び直すとき(productDetail?id=商品ID&edit=行番号)は、
            // カートの内容(トッピング・数量)を選んだ状態で表示する
            CartItem editItem = null;
            try {
                int line = Integer.parseInt(request.getParameter("edit"));
                editItem = CartServlet.getCart(request.getSession()).find(line);
            } catch (NumberFormatException e) {
                // edit がない・数値でない → 新規追加
            }
            request.setAttribute("editItem", editItem);

            request.setAttribute("product", product);
            request.setAttribute("variants", variants);
            request.setAttribute("toppings", toppings);
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "商品情報の取得に失敗しました。");
        }

        request.getRequestDispatcher("/WEB-INF/jsp/productDetail.jsp").forward(request, response);
    }
}
