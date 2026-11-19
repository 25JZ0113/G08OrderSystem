package controller;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

// Tomcat11 なので jakarta.* を使う
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import dao.OrderDetailDAO;
import dao.ProductDAO;
import dao.SalesDAO;
import model.Order;
import model.Product;
import model.SalesOrder;

/**
 * A-06 売上の注文一覧(Controller)
 * 売上管理画面(A-05)の表の行・グラフの棒を押すと開き、その日・曜日・月・商品の注文を、明細つきで表示する
 *   GET /admin/sales/orders?from=2026-10-01&to=2026-10-31&view=day&key=2026-10-03
 *   view=day     … key は日付(yyyy-MM-dd)。その日に会計した注文
 *   view=weekday … key は 0(月)〜6(日)。期間中のその曜日に会計した注文
 *   view=month   … key は年月(yyyy-MM)。期間のうち、その月に会計した注文
 *   view=product … key は商品 ID。期間中に会計した注文のうち、その商品を含む注文
 * 見るだけで DB は変えないので GET で受ける
 */
@WebServlet("/admin/sales/orders")
public class AdminSalesOrdersServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /** 売上管理画面と同じ:一度に見られる期間(日数) */
    private static final int MAX_DAYS = 366;
    /** 一度に表示する注文の数(多すぎると画面が重くなるので上限を決める) */
    private static final int MAX_ORDERS = 200;

    private static final String[] WEEKDAYS = { "月", "火", "水", "木", "金", "土", "日" };

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LocalDate from = parseDate(request.getParameter("from"));
        LocalDate to   = parseDate(request.getParameter("to"));
        String    view = request.getParameter("view");
        String    key  = request.getParameter("key");

        // 売上管理画面に戻るときの URL(同じ期間・同じ表示で戻る)
        String back = request.getContextPath() + "/admin/sales";
        if (from == null || to == null || from.isAfter(to) || ChronoUnit.DAYS.between(from, to) + 1 > MAX_DAYS
                || view == null || key == null) {
            response.sendRedirect(back);
            return;
        }
        back += "?from=" + from + "&to=" + to + "&view=" + view;
        request.setAttribute("backUrl", back);
        request.setAttribute("from", from);
        request.setAttribute("to", to);
        request.setAttribute("view", view);

        // 見る範囲(期間・曜日・商品)と見出しを決める
        LocalDate rangeFrom = from;
        LocalDate rangeTo   = to;
        Integer   weekday   = null;
        Integer   productId = null;
        String    title;
        try {
            switch (view) {
                case "day" -> {
                    LocalDate day = LocalDate.parse(key);
                    rangeFrom = later(from, day);
                    rangeTo   = earlier(to, day);
                    title = String.format("%d/%02d/%02d(%s)の注文", day.getYear(), day.getMonthValue(),
                            day.getDayOfMonth(), WEEKDAYS[day.getDayOfWeek().getValue() - 1]);
                }
                case "weekday" -> {
                    weekday = Integer.valueOf(key);
                    if (weekday < 0 || weekday > 6) {
                        throw new IllegalArgumentException();
                    }
                    title = WEEKDAYS[weekday] + "曜日の注文";
                }
                case "month" -> {
                    YearMonth month = YearMonth.parse(key);
                    rangeFrom = later(from, month.atDay(1));
                    rangeTo   = earlier(to, month.atEndOfMonth());
                    title = String.format("%d/%02d の注文", month.getYear(), month.getMonthValue());
                }
                case "product" -> {
                    productId = Integer.valueOf(key);
                    Product p = new ProductDAO().findById(productId);
                    if (p == null) {
                        throw new IllegalArgumentException();
                    }
                    title = "「" + p.getName() + (p.getSizeName() == null ? "" : "(" + p.getSizeName() + ")")
                            + "」を含む注文";
                    request.setAttribute("productId", productId);
                }
                default -> throw new IllegalArgumentException();
            }
        } catch (DateTimeParseException | IllegalArgumentException e) {
            // 日付・数字として読めない、ありえない値(NumberFormatException も IllegalArgumentException)
            response.sendRedirect(back);
            return;
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "注文を取得できませんでした。");
            request.getRequestDispatcher("/WEB-INF/views/adminSalesOrders.jsp").forward(request, response);
            return;
        }
        request.setAttribute("title", title);
        // 見ている会計日の範囲(1 日だけのときは日付 1 つ)
        request.setAttribute("periodText", rangeFrom.equals(rangeTo)
                ? slash(rangeFrom)
                : slash(rangeFrom) + " 〜 " + slash(rangeTo));

        try {
            List<SalesOrder> orders = new ArrayList<>();
            // 期間の外の日・月を指定されたとき(rangeFrom > rangeTo)は、注文なしとして出す
            if (!rangeFrom.isAfter(rangeTo)) {
                // 1 件多く取って、上限を超えたかを判断する
                List<Order> found = new SalesDAO().findOrders(rangeFrom, rangeTo, weekday, productId, MAX_ORDERS + 1);
                request.setAttribute("truncated", found.size() > MAX_ORDERS);
                OrderDetailDAO detailDAO = new OrderDetailDAO();
                for (Order o : found.subList(0, Math.min(found.size(), MAX_ORDERS))) {
                    orders.add(new SalesOrder(o, detailDAO.findLinesByOrderId(o.getId())));
                }
            }
            request.setAttribute("orders", orders);
            request.setAttribute("maxOrders", MAX_ORDERS);

            // 一覧の上に出す合計(表示している注文の分)
            int  guests = 0;
            long amount = 0;
            for (SalesOrder so : orders) {
                guests += so.getOrder().getGuestCount();
                amount += so.getOrder().getTotalAmount();
            }
            request.setAttribute("totalGuests", guests);
            request.setAttribute("totalAmount", amount);
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "注文を取得できませんでした。");
        }
        request.getRequestDispatcher("/WEB-INF/views/adminSalesOrders.jsp").forward(request, response);
    }

    /** 2026-11-03 → 2026/11/03 */
    private static String slash(LocalDate d) {
        return String.format("%d/%02d/%02d", d.getYear(), d.getMonthValue(), d.getDayOfMonth());
    }

    private static LocalDate later(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalDate earlier(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }

    /** yyyy-MM-dd を日付にする。指定がない・日付でなければ null */
    private static LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(s.strip());
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
