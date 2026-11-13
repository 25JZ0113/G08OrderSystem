package servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Tomcat11 なので jakarta.* を使う
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import dao.OrderDetailDAO;
import model.OrderDetail;
import model.OrderLine;

/**
 * T-06 会計依頼画面(Controller)
 * 未提供の商品があるか確認する
 * 未提供あり:未提供の商品名・数量・状態を表示し、会計できないことを伝える
 * 未提供なし:合計金額と、テーブル番号札を持ってレジへ行く案内を表示する
 * (この画面では DB は変更しない。会計はレジで行う)
 */
@WebServlet("/checkout")
public class CheckoutServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer orderId = (Integer)request.getSession().getAttribute("orderId");   // TableSessionFilter で確認済み

        try {
            List<OrderLine> lines    = new OrderDetailDAO().findLinesByOrderId(orderId);
            List<OrderLine> unserved = new ArrayList<>();
            int             total    = 0;
            for (OrderLine line : lines) {
                total += line.getAmount();
                // 未提供 = 提供済みでもキャンセルでもない(注文受付済み・調理中・調理済み(提供待ち))
                OrderDetail d = line.getDetail();
                if (!OrderDetail.SERVED.equals(d.getOrderStatus()) && !d.isCanceled()) {
                    unserved.add(line);
                }
            }
            request.setAttribute("unserved", unserved);
            request.setAttribute("total", total);
            request.setAttribute("ordered", !lines.isEmpty());
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "会計の確認ができませんでした。もう一度お試しいただくか、スタッフをお呼びください。");
        }
        request.getRequestDispatcher("/WEB-INF/jsp/checkout.jsp").forward(request, response);
    }
}
