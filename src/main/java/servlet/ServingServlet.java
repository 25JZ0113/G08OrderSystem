package servlet;

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
 * H-03 提供待ち一覧画面(Controller)
 * ホール端末からログインなしで利用する
 * GET : 「調理済み(提供待ち)」の明細を注文時刻の古い順に表示
 * POST: 提供済みにする(調理済み(提供待ち) → 提供済み)。お客様の注文状況画面(T-04)にも反映される
 */
@WebServlet("/serving")
public class ServingServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            request.setAttribute("lines", new OrderDetailDAO().findServingLines());
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "提供待ち一覧を取得できませんでした。もう一度お試しください。");
        }

        // POST の結果(リダイレクトのパラメータで受け取る)
        if ("conflict".equals(request.getParameter("result"))) {
            request.setAttribute("errorMessage", "ほかの端末ですでに変更されています。一覧を最新にしました。");
        }
        request.getRequestDispatcher("/WEB-INF/jsp/serving.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        int detailId;
        try {
            detailId = Integer.parseInt(request.getParameter("id"));
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/serving");
            return;
        }

        try {
            // 変更前・変更後の状態はサーバー側で決める(画面から送られた状態は使わない)
            boolean changed = new OrderDetailDAO().changeStatus(
                    detailId, OrderDetail.COOKED, OrderDetail.SERVED);
            // PRG:更新後は GET にリダイレクトして、再読み込みで二重に更新されないようにする
            response.sendRedirect(request.getContextPath() + "/serving"
                    + (changed ? "" : "?result=conflict"));
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "注文状態を変更できませんでした。もう一度お試しください。");
            doGet(request, response);
        }
    }
}
