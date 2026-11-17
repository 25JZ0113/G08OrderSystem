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

import dao.OrderDAO;
import dao.OrderDAO.PaymentException;
import dao.OrderDetailDAO;
import model.Bill;
import model.Order;
import model.OrderLine;
import model.Payment;

/**
 * R-03 会計画面(Controller)
 * GET : テーブル番号・人数・注文明細・小計・内消費税・合計金額を表示 (register/pay?orderId=1)
 *       確定後は register/pay?done=1 で、確定した内容(おつり・レシートの発行)を表示
 * POST: 支払方法(現金のときは預かり金額)を受け取り、会計を確定する
 *       会計状態を「会計済み」、テーブル状態を「準備中」にする(テーブル端末は待機画面になる)
 */
@WebServlet("/register/pay")
public class RegisterPayServlet extends HttpServlet {

    /** 確定した会計をセッションに入れる名前 */
    private static final String LAST_PAYMENT = "lastPayment";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // 確定後の表示
        if (request.getParameter("done") != null) {
            Payment payment = (Payment)request.getSession().getAttribute(LAST_PAYMENT);
            if (payment == null) {
                response.sendRedirect(request.getContextPath() + "/register/table");
                return;
            }
            request.setAttribute("payment", payment);
            request.setAttribute("bill", payment.getBill());
            forward(request, response);
            return;
        }

        Integer orderId = parseInt(request.getParameter("orderId"));
        if (orderId == null) {
            response.sendRedirect(request.getContextPath() + "/register/table");
            return;
        }
        try {
            Bill bill = loadBill(orderId);
            if (bill == null) {
                response.sendRedirect(request.getContextPath() + "/register/table?result=paid");
                return;
            }
            if (hasUnserved(orderId)) {
                // 未提供が残っているときは R-02 へ
                response.sendRedirect(request.getContextPath() + "/register/unserved?orderId=" + orderId);
                return;
            }
            request.setAttribute("bill", bill);
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "会計内容を取得できませんでした。もう一度お試しください。");
        }
        forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        Integer orderId = parseInt(request.getParameter("orderId"));
        String  method  = request.getParameter("method");
        if (orderId == null) {
            response.sendRedirect(request.getContextPath() + "/register/table");
            return;
        }

        try {
            Bill bill = loadBill(orderId);
            if (bill == null) {
                response.sendRedirect(request.getContextPath() + "/register/table?result=paid");
                return;
            }
            request.setAttribute("bill", bill);
            request.setAttribute("method", method);
            request.setAttribute("receivedInput", request.getParameter("received"));

            // 支払方法は 3 つのどれか
            if (!Order.METHOD_CASH.equals(method) && !Order.METHOD_CREDIT.equals(method)
                    && !Order.METHOD_EMONEY.equals(method)) {
                request.setAttribute("errorMessage", "支払方法を選んでください。");
                forward(request, response);
                return;
            }

            // 現金のときだけ預かり金額を入力する
            Integer received = null;
            if (Order.METHOD_CASH.equals(method)) {
                received = parseInt(request.getParameter("received"));
                if (received == null || received < bill.getTotal()) {
                    request.setAttribute("errorMessage", "預かり金額が合計金額より少ないため、会計を確定できません。");
                    forward(request, response);
                    return;
                }
            }

            new OrderDAO().pay(orderId, method, received);

            // PRG:確定した内容をセッションに入れて、確定後の画面へ
            request.getSession().setAttribute(LAST_PAYMENT, new Payment(bill, method, received));
            response.sendRedirect(request.getContextPath() + "/register/pay?done=1");
        } catch (PaymentException e) {
            request.setAttribute("errorMessage", e.getMessage());
            forward(request, response);
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "会計を確定できませんでした。もう一度お試しください。");
            forward(request, response);
        }
    }

    /** 未会計の注文の会計内容(会計済み・見つからないときは null) */
    private Bill loadBill(int orderId) throws SQLException {
        Order order = new OrderDAO().findUnpaidById(orderId);
        if (order == null) {
            return null;
        }
        return new Bill(order, new OrderDetailDAO().findLinesByOrderId(orderId));
    }

    private boolean hasUnserved(int orderId) throws SQLException {
        List<OrderLine> lines = new OrderDetailDAO().findLinesByOrderId(orderId);
        for (OrderLine line : lines) {
            if (line.getDetail().isUnserved()) {
                return true;
            }
        }
        return false;
    }

    private Integer parseInt(String s) {
        try {
            return (s == null) ? null : Integer.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void forward(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/registerPay.jsp").forward(request, response);
    }
}
