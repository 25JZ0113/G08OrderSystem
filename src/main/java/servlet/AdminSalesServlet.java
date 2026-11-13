package servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;

// Tomcat11 なので jakarta.* を使う
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import dao.SalesDAO;
import model.SalesRow;

/**
 * A-05 売上管理画面(Controller)
 * 期間を指定して【集計】し、売上状況(総売上・総客数・総来店数・客単価・1来店あたり単価)と、
 * 日別・曜日別・月別・商品別の集計表と棒グラフを表示する
 *   GET /admin/sales?from=2026-10-01&to=2026-10-31&view=day|weekday|month|product
 * 集計を見るだけで DB は変えないので GET で受ける(URL を保存すれば同じ集計を開ける)
 */
@WebServlet("/admin/sales")
public class AdminSalesServlet extends HttpServlet {

    /** 一度に集計できる期間(日数) */
    private static final int MAX_DAYS = 366;
    /** 商品別の棒グラフに出す数(表はすべて出す) */
    private static final int PRODUCT_CHART_MAX = 10;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // 期間の初期値:今月の 1 日 〜 今日
        LocalDate today = LocalDate.now();
        LocalDate from  = parseDate(request.getParameter("from"), today.withDayOfMonth(1));
        LocalDate to    = parseDate(request.getParameter("to"), today);
        String view = request.getParameter("view");
        if (view == null || !List.of("day", "weekday", "month", "product").contains(view)) {
            view = "day";
        }
        request.setAttribute("from", from);
        request.setAttribute("to", to);
        request.setAttribute("view", view);

        String error = null;
        if (from == null || to == null) {
            error = "期間は日付で入力してください。";
        } else if (from.isAfter(to)) {
            error = "期間の開始日は終了日より前にしてください。";
        } else if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_DAYS) {
            error = "期間は" + MAX_DAYS + "日以内で指定してください。";
        }
        if (error != null) {
            request.setAttribute("errorMessage", error);
            request.getRequestDispatcher("/WEB-INF/jsp/adminSales.jsp").forward(request, response);
            return;
        }

        try {
            SalesDAO dao = new SalesDAO();
            SalesRow summary = dao.summary(from, to);
            request.setAttribute("summary", summary);
            // 客単価・1来店あたり単価(円未満は切り捨て。0 で割らないようにする)
            request.setAttribute("perGuest", summary.getGuests() == 0 ? 0 : summary.getAmount() / summary.getGuests());
            request.setAttribute("perVisit", summary.getVisits() == 0 ? 0 : summary.getAmount() / summary.getVisits());

            List<SalesRow> rows = switch (view) {
                case "weekday" -> dao.byWeekday(from, to);
                case "month"   -> dao.byMonth(from, to);
                case "product" -> dao.byProduct(from, to);
                default        -> dao.byDay(from, to);
            };
            setBarPercent(rows);
            request.setAttribute("rows", rows);
            request.setAttribute("chartRows",
                    "product".equals(view) && rows.size() > PRODUCT_CHART_MAX ? rows.subList(0, PRODUCT_CHART_MAX) : rows);

            // 合計の行(商品別は数量と金額の合計)
            long total = 0;
            int  guests = 0;
            int  quantity = 0;
            for (SalesRow r : rows) {
                total    += r.getAmount();
                guests   += r.getGuests();
                quantity += r.getQuantity();
            }
            request.setAttribute("totalAmount", total);
            request.setAttribute("totalGuests", guests);
            request.setAttribute("totalQuantity", quantity);
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "売上を集計できませんでした。");
        }
        request.getRequestDispatcher("/WEB-INF/jsp/adminSales.jsp").forward(request, response);
    }

    /** 棒グラフの長さ:いちばん売上の多い行を 100 とした割合 */
    private void setBarPercent(List<SalesRow> rows) {
        long max = 0;
        for (SalesRow r : rows) {
            max = Math.max(max, r.getAmount());
        }
        for (SalesRow r : rows) {
            r.setBarPercent(max == 0 ? 0 : (int)Math.round(r.getAmount() * 100.0 / max));
        }
    }

    /** yyyy-MM-dd を日付にする。指定がなければ初期値、日付でなければ null */
    private static LocalDate parseDate(String s, LocalDate defaultValue) {
        if (s == null || s.isBlank()) {
            return defaultValue;
        }
        try {
            return LocalDate.parse(s.strip());
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
