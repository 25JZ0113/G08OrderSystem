package controller;

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

import dao.OrderDAO;
import dao.OrderDetailDAO;
import model.Order;
import model.OrderDetail;
import model.OrderLine;
import model.User;

/**
 * R-02 未提供警告画面(Controller)
 * GET : 未提供の商品名・数量・状態と、商品ごとの【取り消す】を表示する (register/unserved?orderId=1)
 * POST: 商品を取り消す(設計書 12 章)
 *       注文受付済み              → 「キャンセル(請求なし)」。料金は請求しない
 *       調理中・調理済み(提供待ち) → 「キャンセル(請求あり)」。料金は請求する
 *       取り消した社員(ログイン中のユーザー)と日時を記録する
 */
@WebServlet("/register/unserved")
public class RegisterUnservedServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer orderId = parseId(request.getParameter("orderId"));
        if (orderId == null) {
            response.sendRedirect(request.getContextPath() + "/register/table");
            return;
        }

        try {
            Order order = new OrderDAO().findUnpaidById(orderId);
            if (order == null) {
                response.sendRedirect(request.getContextPath() + "/register/table?result=paid");
                return;
            }
            List<OrderLine> unserved = new ArrayList<>();
            for (OrderLine line : new OrderDetailDAO().findLinesByOrderId(orderId)) {
                if (line.getDetail().isUnserved()) {
                    unserved.add(line);
                }
            }
            request.setAttribute("order", order);
            request.setAttribute("unserved", unserved);
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "未提供の商品を取得できませんでした。もう一度お試しください。");
        }

        // POST の結果
        String result = request.getParameter("result");
        if ("canceledNoCharge".equals(result)) {
            request.setAttribute("successMessage", "取り消しました(料金は請求しません)。");
        }
        else if ("canceledCharge".equals(result)) {
            request.setAttribute("successMessage", "取り消しました(調理を始めていたため、料金は請求します)。");
        }
        else if ("conflict".equals(result)) {
            request.setAttribute("errorMessage", "商品の状態が変わっていたため、取り消していません。最新の状態を確認してください。");
        }
        request.getRequestDispatcher("/WEB-INF/views/registerUnserved.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        Integer orderId  = parseId(request.getParameter("orderId"));
        Integer detailId = parseId(request.getParameter("detailId"));
        String  status   = request.getParameter("status");   // 画面に表示していた状態
        if (orderId == null || detailId == null) {
            response.sendRedirect(request.getContextPath() + "/register/table");
            return;
        }

        User user = (User)request.getSession().getAttribute(LoginServlet.LOGIN_USER);   // LoginFilter で確認済み
        String back = request.getContextPath() + "/register/unserved?orderId=" + orderId;
        try {
            String to = new OrderDetailDAO().cancel(orderId, detailId, status, user.getId());
            // PRG:結果はリダイレクトのパラメータで伝える
            String result;
            if (to == null) {
                result = "conflict";
            }
            else if (OrderDetail.CANCELED_CHARGE.equals(to)) {
                result = "canceledCharge";
            }
            else {
                result = "canceledNoCharge";
            }
            response.sendRedirect(back + "&result=" + result);
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendRedirect(back + "&result=conflict");
        }
    }

    private Integer parseId(String s) {
        try {
            return Integer.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
