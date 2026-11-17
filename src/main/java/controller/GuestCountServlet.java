package controller;

import java.io.IOException;
import java.sql.SQLException;

// Tomcat11 なので jakarta.* を使う
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import dao.OrderDAO;
import dao.TerminalDAO;
import model.Order;
import model.TableTerminal;

/**
 * T-08 人数入力画面(Controller)。テーブル端末の入口
 *
 * GET : 端末のテーブル状態を見て、表示する画面を決める
 * 空席 → 人数入力画面
 * 利用中 → 今の来店(未会計の注文)をセッションに入れてメニュー画面へ
 * 準備中 → T-07 待機画面へ
 * POST: 人数を受け取り、テーブルを「利用中」にして注文を登録し、メニュー画面へ
 *
 * どのテーブルの端末かは、Cookie「terminalCode」に保存した端末コード(TERMINAL_SETTINGS.CODE)で判断する。
 * 端末コードは S-01 テーブル番号確認 / S-02 テーブル番号変更画面で設定する。
 * (テスト用に guestCount?terminal=TABLE-06 のように URL でも設定できる)
 */
@WebServlet("/guestCount")
public class GuestCountServlet extends HttpServlet {

    /** 端末コードを保存する Cookie の名前 */
    public static final String TERMINAL_COOKIE = "terminalCode";
    /** 人数の上限 */
    public static final int    MAX_GUESTS      = 10;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            // URL で端末コードが指定されたら Cookie に保存する(テスト用。通常は S-02 で設定する)
            String code = request.getParameter("terminal");
            if (code != null && !code.isEmpty()) {
                if (new TerminalDAO().findTableTerminal(code) != null) {
                    saveTerminalCookie(request, response, code);
                    response.sendRedirect(request.getContextPath() + "/guestCount");
                    return;
                }
                request.setAttribute("errorMessage", "端末コード「" + code + "」のテーブル端末は登録されていません。");
            }
            else {
                code = readTerminalCookie(request);
            }

            TableTerminal terminal =
                    (code == null) ? null : new TerminalDAO().findTableTerminal(code);
            if (terminal == null) {
                // 端末にテーブルが設定されていない
                request.setAttribute("needSetup", true);
                forward(request, response);
                return;
            }

            HttpSession session = request.getSession();
            session.setAttribute("tableNo", terminal.getTableNo());   // ヘッダーのテーブル番号
            request.setAttribute("terminal", terminal);

            switch (terminal.getTableStatus()) {
                case TableTerminal.STATUS_IN_USE -> {
                    // 利用中:今の来店を引き継いでメニューへ(セッションが切れたときもここで戻せる)
                    Order order = new OrderDAO().findUnpaidByTableId(terminal.getTableId());
                    if (order != null) {
                        startSession(session, terminal, order.getId(), order.getGuestCount(),
                                false);
                        response.sendRedirect(request.getContextPath() + "/productList");
                        return;
                    }
                    // 利用中なのに未会計の注文がない(データの不整合)
                    request.setAttribute("errorMessage", "このテーブルの注文情報が見つかりません。スタッフをお呼びください。");
                }
                case TableTerminal.STATUS_PREPARING -> {
                    // 準備中:注文できない → T-07 待機画面へ
                    clearVisit(session);
                    response.sendRedirect(request.getContextPath() + "/waiting");
                    return;
                }
                default -> {
                    // 空席:人数入力画面を表示する
                    clearVisit(session);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "テーブル情報の取得に失敗しました。スタッフをお呼びください。");
        }
        forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String code = readTerminalCookie(request);
        int    guestCount;
        try {
            guestCount = Integer.parseInt(request.getParameter("guestCount"));
        } catch (NumberFormatException e) {
            guestCount = 0;
        }

        try {
            TableTerminal terminal =
                    (code == null) ? null : new TerminalDAO().findTableTerminal(code);
            if (terminal == null || guestCount < 1 || guestCount > MAX_GUESTS) {
                // 端末未設定・人数が範囲外 → 入力画面をもう一度
                response.sendRedirect(request.getContextPath() + "/guestCount");
                return;
            }

            Integer orderId = new OrderDAO().startVisit(terminal.getTableId(), guestCount);
            if (orderId == null) {
                // 空席でなかった(別の操作で状態が変わった)→ GET で状態に合った画面を出し直す
                response.sendRedirect(request.getContextPath() + "/guestCount");
                return;
            }

            startSession(request.getSession(), terminal, orderId, guestCount, true);
            response.sendRedirect(request.getContextPath() + "/productList");
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "受付できませんでした。もう一度お試しいただくか、スタッフをお呼びください。");
            forward(request, response);
        }
    }

    /** 来店の情報をセッションに入れる(newVisit のときはカートも空にする) */
    private void startSession(HttpSession session, TableTerminal terminal, int orderId,
            int guestCount, boolean newVisit) {
        session.setAttribute("tableId", terminal.getTableId());
        session.setAttribute("tableNo", terminal.getTableNo());
        session.setAttribute("orderId", orderId);
        session.setAttribute("guestCount", guestCount);
        if (newVisit) {
            session.removeAttribute("cart");
        }
    }

    /** 前のお客様の情報(人数・カート・注文)を消す。テーブル番号は残す */
    public static void clearVisit(HttpSession session) {
        session.removeAttribute("tableId");
        session.removeAttribute("orderId");
        session.removeAttribute("guestCount");
        session.removeAttribute("cart");
    }

    private void forward(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/guestCount.jsp").forward(request, response);
    }

    /** Cookie から端末コードを読む(なければ null) */
    public static String readTerminalCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if (TERMINAL_COOKIE.equals(c.getName())) {
                    return c.getValue();
                }
            }
        }
        return null;
    }

    /** 端末コードを Cookie に保存する(1 年間。ブラウザを閉じても残る) */
    public static void saveTerminalCookie(HttpServletRequest request, HttpServletResponse response,
            String code) {
        Cookie c = new Cookie(TERMINAL_COOKIE, code);
        c.setPath(request.getContextPath().isEmpty() ? "/" : request.getContextPath());
        c.setMaxAge(60 * 60 * 24 * 365);
        c.setHttpOnly(true);
        response.addCookie(c);
    }
}
