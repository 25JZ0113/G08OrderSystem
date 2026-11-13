package servlet;

import java.io.IOException;
import java.sql.SQLException;

// Tomcat11 なので jakarta.* を使う
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import dao.OrderDAO;

/**
 * お客様の注文画面(T-01〜T-03)の前に通るフィルター
 * 人数入力(T-08)が済んでいない(セッションに注文 ID がない)ときは、人数入力画面へ移動させる
 * 画面を表示するときは、ヘッダーの人数を DB から読み直す(ホールで人数変更されたときのため)
 * 会計が済んでいたら(未会計の注文がない)、端末を初期化して待機画面(T-07)へ移動させる
 */
@WebFilter({"/productList", "/productDetail", "/cart", "/cartAdd", "/orderConfirm", "/orderStatus",
        "/orderHistory", "/checkout"})
public class TableSessionFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest  request  = (HttpServletRequest)req;
        HttpServletResponse response = (HttpServletResponse)res;

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("orderId") == null) {
            response.sendRedirect(request.getContextPath() + "/guestCount");
            return;
        }

        // 画面を表示するとき(GET)は、ヘッダーの人数を DB から読み直す
        // ホール端末の人数変更(H-02)で変わっていても、次の画面表示で反映される
        if ("GET".equals(request.getMethod())) {
            if (!refreshGuestCount(session)) {
                // 未会計の注文がなくなった = レジで会計が完了した
                // テーブル端末を初期化して(テーブル番号は残す)入口へ。準備中なら T-07 待機画面になる
                GuestCountServlet.clearVisit(session);
                response.sendRedirect(request.getContextPath() + "/guestCount");
                return;
            }
        }
        chain.doFilter(req, res);
    }

    /**
     * 人数を DB から読み直す
     * @return 来店が続いていれば true。会計が済んで未会計の注文がなくなっていれば false
     */
    private boolean refreshGuestCount(HttpSession session) {
        try {
            int     orderId    = (Integer)session.getAttribute("orderId");
            Integer guestCount = new OrderDAO().findUnpaidGuestCount(orderId);
            if (guestCount == null) {
                return false;
            }
            session.setAttribute("guestCount", guestCount);
        } catch (SQLException e) {
            // 読み直せなくても注文はできるので、前の人数のまま表示する
            e.printStackTrace();
        }
        return true;
    }
}
