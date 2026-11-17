package controller;

import java.io.IOException;
import java.sql.SQLException;

// Tomcat11 なので jakarta.* を使う
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import dao.OrderDetailDAO;
import model.OrderDetail;

/**
 * K-01 調理待ち一覧画面(Controller)
 * キッチン端末を起動するとログインなしで表示する
 * GET : 「注文受付済み」「調理中」の明細を注文時刻の古い順に表示
 * POST: 調理中にする(注文受付済み → 調理中)/ 調理済みにする(調理中 → 調理済み(提供待ち))
 */
@WebServlet("/kitchen")
public class KitchenServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            request.setAttribute("lines", new OrderDetailDAO().findKitchenLines());
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "調理待ち一覧を取得できませんでした。もう一度お試しください。");
        }

        // POST の結果(リダイレクトのパラメータで受け取る)
        if ("conflict".equals(request.getParameter("result"))) {
            request.setAttribute("errorMessage", "ほかの端末ですでに変更されています。一覧を最新にしました。");
        }
        request.getRequestDispatcher("/WEB-INF/views/kitchen.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        // 変更前・変更後の状態はサーバー側で決める(画面から送られた状態は使わない)
        String action = request.getParameter("action");
        String from;
        String to;
        if ("cook".equals(action)) {
            from = OrderDetail.RECEIVED;
            to = OrderDetail.COOKING;
        }
        else if ("done".equals(action)) {
            from = OrderDetail.COOKING;
            to = OrderDetail.COOKED;
        }
        else {
            response.sendRedirect(request.getContextPath() + "/kitchen");
            return;
        }

        int detailId;
        try {
            detailId = Integer.parseInt(request.getParameter("id"));
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/kitchen");
            return;
        }

        try {
            boolean changed = new OrderDetailDAO().changeStatus(detailId, from, to);
            // PRG:更新後は GET にリダイレクトして、再読み込みで二重に更新されないようにする
            response.sendRedirect(request.getContextPath() + "/kitchen"
                    + (changed ? "" : "?result=conflict"));
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "注文状態を変更できませんでした。もう一度お試しください。");
            doGet(request, response);
        }
    }
}
