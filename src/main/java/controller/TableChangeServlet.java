package controller;

import java.io.IOException;
import java.sql.SQLException;

// Tomcat11 なので jakarta.* を使う
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import dao.TerminalDAO;
import model.TableTerminal;

/**
 * S-02 テーブル番号変更画面(Controller)
 * GET : 現在のテーブル番号を入力欄に表示する
 * POST: 入力した番号(1〜15)を保存して S-01 テーブル番号確認画面に戻る。範囲外はエラーを表示
 *
 * 保存 = その番号のテーブル端末の端末コード(TERMINAL_SETTINGS.CODE)を、この端末の Cookie に保存する
 * (DB の端末設定は変えない。テーブルと端末コードは 1 対 1 で登録済み)
 */
@WebServlet("/tableChange")
public class TableChangeServlet extends HttpServlet {

    /** テーブル番号の範囲 */
    public static final int MIN_TABLE_NO = 1;
    public static final int MAX_TABLE_NO = 15;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String code = GuestCountServlet.readTerminalCookie(request);
        try {
            TableTerminal terminal =
                    (code == null) ? null : new TerminalDAO().findTableTerminal(code);
            if (terminal != null) {
                request.setAttribute("inputNo", String.valueOf(terminal.getTableNo()));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "テーブル番号を取得できませんでした。もう一度お試しください。");
        }
        forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String input = request.getParameter("tableNo");

        int tableNo;
        try {
            tableNo = Integer.parseInt(input == null ? "" : input.trim());
        } catch (NumberFormatException e) {
            tableNo = 0;
        }

        // 範囲外・数字でない → エラーを表示して入力し直してもらう
        if (tableNo < MIN_TABLE_NO || tableNo > MAX_TABLE_NO) {
            request.setAttribute("errorMessage",
                    "テーブル番号は " + MIN_TABLE_NO + "〜" + MAX_TABLE_NO + " の数字で入力してください。");
            request.setAttribute("inputNo", input);
            forward(request, response);
            return;
        }

        try {
            TableTerminal terminal = new TerminalDAO().findTableTerminalByTableNo(tableNo);
            if (terminal == null) {
                request.setAttribute("errorMessage", "テーブル" + tableNo + "の端末が登録されていません。");
                request.setAttribute("inputNo", input);
                forward(request, response);
                return;
            }

            // この端末の端末コードを保存し直す
            GuestCountServlet.saveTerminalCookie(request, response, terminal.getCode());

            // 前のテーブルの来店情報は消す(テーブル番号は新しい番号にする)
            HttpSession session = request.getSession();
            GuestCountServlet.clearVisit(session);
            session.setAttribute("tableNo", terminal.getTableNo());

            // PRG:S-01 テーブル番号確認画面に戻る
            response.sendRedirect(request.getContextPath() + "/tableConfirm");
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "保存できませんでした。もう一度お試しください。");
            request.setAttribute("inputNo", input);
            forward(request, response);
        }
    }

    private void forward(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("minTableNo", MIN_TABLE_NO);
        request.setAttribute("maxTableNo", MAX_TABLE_NO);
        request.getRequestDispatcher("/WEB-INF/views/tableChange.jsp").forward(request, response);
    }
}
