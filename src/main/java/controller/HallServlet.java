package controller;

import java.io.IOException;
import java.sql.SQLException;

// Tomcat11 なので jakarta.* を使う
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import dao.TableDAO;

/**
 * H-01 席状況画面(Controller)
 * ホール端末を起動するとログインなしで表示する
 * GET : 全テーブル(15 台)の状態(空席・利用中・準備中)と、利用中のテーブルの人数を表示
 * POST: 準備中のテーブルを選ぶ → 清掃完了として空席にする
 */
@WebServlet("/hall")
public class HallServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            request.setAttribute("seats", new TableDAO().findSeats());
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "席状況を取得できませんでした。もう一度お試しください。");
        }

        // POST の結果(リダイレクトのパラメータで受け取る)
        String result = request.getParameter("result");
        if ("cleaned".equals(result)) {
            request.setAttribute("successMessage",
                    "テーブル" + request.getParameter("no") + "を空席にしました。");
        }
        else if ("guestChanged".equals(result)) {
            // H-02 人数変更画面から戻ったとき
            request.setAttribute("successMessage",
                    "テーブル" + request.getParameter("no") + "の人数を"
                            + request.getParameter("count") + "人に変更しました。");
        }
        else if ("conflict".equals(result)) {
            request.setAttribute("errorMessage", "ほかの端末ですでに変更されています。席状況を最新にしました。");
        }
        else if ("error".equals(result)) {
            request.setAttribute("errorMessage", "変更できませんでした。もう一度お試しください。");
        }
        request.getRequestDispatcher("/WEB-INF/views/hall.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        int tableId;
        int tableNo;
        try {
            tableId = Integer.parseInt(request.getParameter("tableId"));
            tableNo = Integer.parseInt(request.getParameter("tableNo"));   // メッセージ表示用
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/hall");
            return;
        }

        try {
            boolean changed = new TableDAO().finishCleaning(tableId);
            // PRG:更新後は GET にリダイレクトして、再読み込みで二重に更新されないようにする
            response.sendRedirect(request.getContextPath() + "/hall"
                    + (changed ? "?result=cleaned&no=" + tableNo : "?result=conflict"));
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "テーブル状態を変更できませんでした。もう一度お試しください。");
            doGet(request, response);
        }
    }
}
