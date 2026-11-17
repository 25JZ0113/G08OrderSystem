package controller;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
// Tomcat11 なので jakarta.* を使う
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import dao.ProductDAO;
import model.MenuItem;
import model.Product;

/**
 * T-01 メニュー画面(Controller)
 * productList?category=カテゴリ名 で、そのカテゴリの商品だけを表示する
 * (category がないときは最初のカテゴリ)
 */
@WebServlet("/productList")
public class ProductListServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            ProductDAO dao = new ProductDAO();
            // 販売中の商品(トッピングを除く)。カテゴリ順・ID順に並んでいる
            List<Product> productList = dao.findMenu();

            // カテゴリ名 → (商品名 → MenuItem) の 2 段でまとめる
            // サイズ違い(同じカテゴリ・同じ商品名)は 1 つの MenuItem に入る
            // LinkedHashMap なので、取得した順番(カテゴリ順・ID 順)のまま
            Map<String, Map<String, MenuItem>> work = new LinkedHashMap<>();
            for (Product p : productList) {
                Map<String, MenuItem> items =
                        work.computeIfAbsent(p.getCategoryName(), k -> new LinkedHashMap<>());
                items.computeIfAbsent(p.getName(), MenuItem::new).addVariant(p);
            }

            // JSP で扱いやすいように カテゴリ名 → List<MenuItem> に変換する
            Map<String, List<MenuItem>> menuByCategory = new LinkedHashMap<>();
            for (Map.Entry<String, Map<String, MenuItem>> e : work.entrySet()) {
                menuByCategory.put(e.getKey(), new ArrayList<>(e.getValue().values()));
            }

            // 表示するカテゴリ(指定がない・存在しないときは最初のカテゴリ)
            String selected = request.getParameter("category");
            if (selected == null || !menuByCategory.containsKey(selected)) {
                selected =
                        menuByCategory.isEmpty() ? null : menuByCategory.keySet().iterator().next();
            }

            // request 属性に格納
            request.setAttribute("menuByCategory", menuByCategory);   // 左のカテゴリ一覧用
            request.setAttribute("selectedCategory", selected);
            request.setAttribute("items", selected == null ? new ArrayList<MenuItem>()
                    : menuByCategory.get(selected));
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "商品一覧の取得に失敗しました。");
        }

        // JSP へ転送(WEB-INF 配下に置くと直接アクセスされない)
        request.getRequestDispatcher("/WEB-INF/views/productList.jsp").forward(request, response);
    }
}
