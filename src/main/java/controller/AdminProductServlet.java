package controller;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

// Tomcat11 なので jakarta.* を使う
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import dao.CategoryDAO;
import dao.ProductDAO;
import model.Category;
import model.Product;
import util.ImageStore;

/**
 * A-04 メニュー(商品)管理画面(Controller)
 * 左:カテゴリ一覧と【追加】【変更】【削除】
 * 右上:選んだカテゴリの商品一覧【選択】【商品を追加】
 * 右下:商品情報の入力欄と【登録】【変更】【削除】、付けられるトッピングの【追加】【外す】
 *   GET  /admin/products?categoryId=C            … カテゴリ C の商品一覧(商品情報は新規登録)
 *   GET  /admin/products?categoryId=C&productId=P … 商品 P を選んだ状態
 *   POST action=categoryAdd / categoryUpdate / categoryDelete
 *              productRegister / productUpdate / productDelete / toppingAdd / toppingRemove
 * 価格を変えても、注文明細に保存した販売価格には影響しない
 * 商品画像は【登録】【変更】のときにアップロードできる(ブラウザで 4:3・1024×768 の JPG にしたもの。util.ImageStore)
 */
@WebServlet("/admin/products")
@MultipartConfig(maxFileSize = ImageStore.MAX_BYTES, maxRequestSize = ImageStore.MAX_BYTES + 1024 * 1024)
public class AdminProductServlet extends HttpServlet {

    private static final int CATEGORY_NAME_MAX = 20;
    private static final int TEXT_MAX          = 100;
    private static final int PRODUCT_NAME_MAX  = 30;
    private static final int SIZE_MAX          = 10;
    private static final int PRICE_MAX         = 99999;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        setResultMessage(request);
        show(request, response, parseInt(request.getParameter("categoryId")),
                parseInt(request.getParameter("productId")), null, null);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");
        int categoryId = parseInt(request.getParameter("categoryId"));
        int productId  = parseInt(request.getParameter("productId"));

        try {
            switch (action == null ? "" : action) {
                case "categoryAdd", "categoryUpdate", "categoryDelete" ->
                        doCategory(request, response, action, categoryId, productId);
                case "productRegister", "productUpdate", "productDelete" ->
                        doProduct(request, response, action, categoryId, productId);
                case "toppingAdd", "toppingRemove" ->
                        doTopping(request, response, action, categoryId, productId);
                default -> redirect(request, response, categoryId, productId, null);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "保存できませんでした。もう一度お試しください。");
            show(request, response, categoryId, productId, null, null);
        }
    }

    // ------------------------------------------------------------
    // カテゴリ
    // ------------------------------------------------------------
    private void doCategory(HttpServletRequest request, HttpServletResponse response,
            String action, int categoryId, int productId) throws ServletException, IOException, SQLException {
        CategoryDAO dao = new CategoryDAO();
        Category cat = new Category(categoryId,
                strip(request.getParameter("categoryName")), emptyToNull(request.getParameter("categoryDescription")));

        // トッピングのカテゴリは名前で探しているので、名前の変更・削除はできない
        Category current = findCategory(dao.findAll(), categoryId);
        boolean isTopping = current != null && ProductDAO.TOPPING_CATEGORY.equals(current.getName());

        switch (action) {
            case "categoryAdd" -> {
                cat.setId(0);
                String error = validateCategory(dao, cat);
                if (error != null) {
                    categoryError(request, response, categoryId, productId, cat, error);
                    return;
                }
                int newId = dao.insert(cat);
                redirect(request, response, newId, 0, "categoryAdded");
            }
            case "categoryUpdate" -> {
                if (current == null) {
                    redirect(request, response, 0, 0, "gone");
                    return;
                }
                String error = isTopping && !ProductDAO.TOPPING_CATEGORY.equals(cat.getName())
                        ? "「" + ProductDAO.TOPPING_CATEGORY + "」のカテゴリは名前を変更できません(説明は変更できます)。"
                        : validateCategory(dao, cat);
                if (error != null) {
                    categoryError(request, response, categoryId, productId, cat, error);
                    return;
                }
                redirect(request, response, categoryId, productId,
                        dao.update(cat) ? "categoryUpdated" : "gone");
            }
            case "categoryDelete" -> {
                if (current == null) {
                    redirect(request, response, 0, 0, "gone");
                    return;
                }
                String error = null;
                if (isTopping) {
                    error = "「" + ProductDAO.TOPPING_CATEGORY + "」のカテゴリは削除できません。";
                } else if (dao.countProducts(categoryId) > 0) {
                    error = "商品が入っているカテゴリは削除できません。先に商品を削除するか、ほかのカテゴリへ移してください。";
                }
                if (error != null) {
                    categoryError(request, response, categoryId, productId, null, error);
                    return;
                }
                redirect(request, response, 0, 0, dao.delete(categoryId) ? "categoryDeleted" : "gone");
            }
            default -> redirect(request, response, categoryId, productId, null);
        }
    }

    private String validateCategory(CategoryDAO dao, Category cat) throws SQLException {
        if (cat.getName() == null || cat.getName().isEmpty()) {
            return "カテゴリ名を入力してください。";
        }
        if (cat.getName().length() > CATEGORY_NAME_MAX) {
            return "カテゴリ名は" + CATEGORY_NAME_MAX + "文字以内で入力してください。";
        }
        if (cat.getDescription() != null && cat.getDescription().length() > TEXT_MAX) {
            return "カテゴリの説明は" + TEXT_MAX + "文字以内で入力してください。";
        }
        if (dao.existsName(cat.getName(), cat.getId())) {
            return "カテゴリ「" + cat.getName() + "」はすでにあります。";
        }
        return null;
    }

    private void categoryError(HttpServletRequest request, HttpServletResponse response,
            int categoryId, int productId, Category catForm, String message) throws ServletException, IOException {
        request.setAttribute("errorMessage", message);
        show(request, response, categoryId, productId, null, catForm);
    }

    // ------------------------------------------------------------
    // 商品
    // ------------------------------------------------------------
    private void doProduct(HttpServletRequest request, HttpServletResponse response,
            String action, int categoryId, int productId) throws ServletException, IOException, SQLException {
        ProductDAO dao = new ProductDAO();

        if ("productDelete".equals(action)) {
            if (dao.countOrderDetails(productId) > 0) {
                request.setAttribute("errorMessage",
                        "この商品は注文されたことがあるため削除できません。販売状態を「販売停止」にしてください。");
                show(request, response, categoryId, productId, readProduct(request, productId), null);
                return;
            }
            redirect(request, response, categoryId, 0, dao.delete(productId) ? "productDeleted" : "gone");
            return;
        }

        boolean isNew = "productRegister".equals(action);
        Product form = readProduct(request, isNew ? 0 : productId);
        byte[] image = readImage(request);
        if (image != null && form.getImageUrl() != null) {
            form.setImageUrl(ImageStore.toJpgName(form.getImageUrl()));   // ブラウザで JPG にしてあるので拡張子をそろえる
        }
        String error = validateProduct(request, form);
        if (error == null && image != null) {
            error = validateImage(request, form, image);
        }
        if (error == null && dao.existsSame(form.getCategoryId(), form.getName(), form.getSizeName(), form.getId())) {
            error = "同じカテゴリに、同じ商品名・サイズの商品がすでにあります。";
        }
        if (error != null) {
            if (image != null) {
                error += "(画像はもう一度選んでください)";
            }
            request.setAttribute("errorMessage", error);
            show(request, response, categoryId, isNew ? 0 : productId, form, null);
            return;
        }

        // 画像を保存してから DB に登録する。DB に登録できなかったら、保存した画像を消す
        List<Path> saved = List.of();
        if (image != null) {
            try {
                saved = ImageStore.save(getServletContext(), form.getImageUrl(), image);
            } catch (IOException e) {
                e.printStackTrace();
                request.setAttribute("errorMessage", "画像を保存できませんでした。もう一度お試しください。(画像はもう一度選んでください)");
                show(request, response, categoryId, isNew ? 0 : productId, form, null);
                return;
            }
        }
        String done = isNew ? "productRegistered" : "productUpdated";
        if (image != null && !ImageStore.savesToProject(getServletContext())) {
            done += "NoProject";
        }
        try {
            if (isNew) {
                int newId = dao.insert(form);
                redirect(request, response, form.getCategoryId(), newId, done);
            } else if (dao.update(form)) {
                // カテゴリを変えたときは、移した先のカテゴリを開く
                redirect(request, response, form.getCategoryId(), productId, done);
            } else {
                ImageStore.delete(saved);
                redirect(request, response, categoryId, 0, "gone");
            }
        } catch (SQLException e) {
            ImageStore.delete(saved);
            throw e;
        }
    }

    /** アップロードされた画像(選ばれていなければ null) */
    private byte[] readImage(HttpServletRequest request) throws IOException, ServletException {
        if (request.getContentType() == null || !request.getContentType().startsWith("multipart/")) {
            return null;
        }
        Part part = request.getPart("imageFile");
        if (part == null || part.getSize() == 0) {
            return null;
        }
        try (InputStream in = part.getInputStream()) {
            return in.readAllBytes();
        }
    }

    private String validateImage(HttpServletRequest request, Product p, byte[] image) {
        if (p.getImageUrl() == null) {
            return "画像のファイル名を入力してください(例:cafe-au-lait.jpg)。";
        }
        if (!ImageStore.isJpeg(image)) {
            return "画像を読み込めませんでした。JPG・PNG・WebP の画像を選んでください。";
        }
        if (ImageStore.exists(getServletContext(), p.getImageUrl())) {
            return "画像「" + p.getImageUrl() + "」はすでにあります。別のファイル名を入力してください。";
        }
        return null;
    }

    /** 入力欄の値を Product に詰める(数字でない値は -1 にして、入力チェックでエラーにする) */
    private Product readProduct(HttpServletRequest request, int id) {
        Product p = new Product();
        p.setId(id);
        p.setCategoryId(parseInt(request.getParameter("productCategoryId")));
        p.setName(strip(request.getParameter("name")));
        p.setSizeName(emptyToNull(request.getParameter("sizeName")));
        p.setPrice(parseIntOrMinus(request.getParameter("price")));
        p.setTaxRate(parseIntOrMinus(request.getParameter("taxRate")));
        p.setSalesStatus(request.getParameter("salesStatus"));
        p.setDescription(emptyToNull(request.getParameter("description")));
        p.setImageUrl(emptyToNull(request.getParameter("imageUrl")));
        return p;
    }

    private String validateProduct(HttpServletRequest request, Product p) throws SQLException {
        if (findCategory(new CategoryDAO().findAll(), p.getCategoryId()) == null) {
            return "カテゴリを選んでください。";
        }
        if (p.getName() == null || p.getName().isEmpty()) {
            return "商品名を入力してください。";
        }
        if (p.getName().length() > PRODUCT_NAME_MAX) {
            return "商品名は" + PRODUCT_NAME_MAX + "文字以内で入力してください。";
        }
        if (p.getSizeName() != null && p.getSizeName().length() > SIZE_MAX) {
            return "サイズは" + SIZE_MAX + "文字以内で入力してください。";
        }
        if (p.getPrice() < 0 || p.getPrice() > PRICE_MAX) {
            return "価格は0〜" + PRICE_MAX + "円の整数で入力してください。";
        }
        if (p.getTaxRate() != 8 && p.getTaxRate() != 10) {
            return "税率は8%か10%を選んでください。";
        }
        if (!ProductDAO.ON_SALE.equals(p.getSalesStatus()) && !ProductDAO.STOPPED.equals(p.getSalesStatus())) {
            return "販売状態を選んでください。";
        }
        if (p.getDescription() != null && p.getDescription().length() > TEXT_MAX) {
            return "説明は" + TEXT_MAX + "文字以内で入力してください。";
        }
        if (p.getImageUrl() != null && !p.getImageUrl().matches("[A-Za-z0-9._-]{1," + TEXT_MAX + "}")) {
            return "画像のファイル名は半角英数字(. _ - も可)で入力してください(例:blend.jpg)。";
        }
        return null;
    }

    // ------------------------------------------------------------
    // トッピング
    // ------------------------------------------------------------
    private void doTopping(HttpServletRequest request, HttpServletResponse response,
            String action, int categoryId, int productId) throws ServletException, IOException, SQLException {
        ProductDAO dao = new ProductDAO();
        int toppingId = parseInt(request.getParameter("toppingId"));
        if (toppingId == 0) {
            request.setAttribute("errorMessage", "追加するトッピングを選んでください。");
            show(request, response, categoryId, productId, null, null);
            return;
        }
        if ("toppingAdd".equals(action)) {
            try {
                dao.addTopping(productId, toppingId);
                redirect(request, response, categoryId, productId, "toppingAdded");
            } catch (SQLIntegrityConstraintViolationException e) {
                // すでに付いている・商品かトッピングがほかの端末で削除された
                redirect(request, response, categoryId, productId, "toppingConflict");
            }
        } else {
            redirect(request, response, categoryId, productId,
                    dao.removeTopping(productId, toppingId) ? "toppingRemoved" : "toppingConflict");
        }
    }

    // ------------------------------------------------------------
    // 画面の表示
    // ------------------------------------------------------------
    /**
     * 画面を表示する。productForm・categoryForm はエラーのときに入力された値(null なら DB の値)
     */
    private void show(HttpServletRequest request, HttpServletResponse response, int categoryId, int productId,
            Product productForm, Category categoryForm) throws ServletException, IOException {
        try {
            CategoryDAO categoryDao = new CategoryDAO();
            ProductDAO  productDao  = new ProductDAO();

            List<Category> categories = categoryDao.findAll();
            Category selected = findCategory(categories, categoryId);
            if (selected == null && !categories.isEmpty()) {
                selected = categories.get(0);    // 指定がなければ最初のカテゴリ
            }

            Product product = null;
            if (productId > 0) {
                product = productDao.findById(productId);
                if (product == null && productForm == null) {
                    request.setAttribute("errorMessage", "選んだ商品はほかの端末で削除されています。");
                }
            }

            request.setAttribute("categories", categories);
            request.setAttribute("selectedCategory", selected);
            request.setAttribute("categoryForm", categoryForm != null ? categoryForm : selected);
            if (selected != null) {
                request.setAttribute("products", productDao.findByCategory(selected.getId()));
                request.setAttribute("isToppingCategory", ProductDAO.TOPPING_CATEGORY.equals(selected.getName()));
            }

            // 商品情報の入力欄:エラーのときは入力された値、選んだ商品があればその値、なければ新規登録
            Product form = productForm != null ? productForm : product;
            if (form == null) {
                form = new Product();
                form.setCategoryId(selected == null ? 0 : selected.getId());
                form.setTaxRate(10);
                form.setSalesStatus(ProductDAO.ON_SALE);
            }
            request.setAttribute("form", form);
            request.setAttribute("selectedProductId", product == null ? 0 : product.getId());

            // 選んだ商品に付けられるトッピング(トッピング自身には付けない)
            if (product != null && !ProductDAO.TOPPING_CATEGORY.equals(product.getCategoryName())) {
                request.setAttribute("toppings", productDao.findAttachedToppings(product.getId()));
                request.setAttribute("toppingCandidates", productDao.findToppingCandidates(product.getId()));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "メニューを読み込めませんでした。");
        }
        request.getRequestDispatcher("/WEB-INF/views/adminProducts.jsp").forward(request, response);
    }

    private void setResultMessage(HttpServletRequest request) {
        String result = request.getParameter("result");
        if (result == null) {
            return;
        }
        switch (result) {
            case "categoryAdded"     -> request.setAttribute("successMessage", "カテゴリを追加しました。");
            case "categoryUpdated"   -> request.setAttribute("successMessage", "カテゴリを変更しました。");
            case "categoryDeleted"   -> request.setAttribute("successMessage", "カテゴリを削除しました。");
            case "productRegistered" -> request.setAttribute("successMessage", "商品を登録しました。");
            case "productUpdated"    -> request.setAttribute("successMessage", "商品を変更しました。");
            case "productRegisteredNoProject", "productUpdatedNoProject" -> request.setAttribute("successMessage",
                    (result.startsWith("productRegistered") ? "商品を登録しました。" : "商品を変更しました。")
                    + "※画像はプロジェクトの images に保存していません(WEB-INF/upload.properties がないため)。Eclipse で「クリーン」すると消えます。");
            case "productDeleted"    -> request.setAttribute("successMessage", "商品を削除しました。");
            case "toppingAdded"      -> request.setAttribute("successMessage", "トッピングを追加しました。");
            case "toppingRemoved"    -> request.setAttribute("successMessage", "トッピングを外しました。");
            case "toppingConflict"   -> request.setAttribute("errorMessage", "ほかの端末ですでに変更されています。最新の状態を確認してください。");
            case "gone"              -> request.setAttribute("errorMessage", "ほかの端末ですでに削除されています。");
            default -> { }
        }
    }

    /** 登録・変更・削除のあとは GET で開き直す(PRG) */
    private void redirect(HttpServletRequest request, HttpServletResponse response,
            int categoryId, int productId, String result) throws IOException {
        StringBuilder url = new StringBuilder(request.getContextPath()).append("/admin/products?");
        url.append("categoryId=").append(categoryId);
        if (productId > 0) {
            url.append("&productId=").append(productId);
        }
        if (result != null) {
            url.append("&result=").append(result);
        }
        response.sendRedirect(url.toString());
    }

    private static Category findCategory(List<Category> categories, int id) {
        for (Category c : categories) {
            if (c.getId() == id) {
                return c;
            }
        }
        return null;
    }

    private static int parseInt(String s) {
        try {
            return s == null ? 0 : Integer.parseInt(s.strip());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static int parseIntOrMinus(String s) {
        try {
            return Integer.parseInt(s.strip());
        } catch (NullPointerException | NumberFormatException e) {
            return -1;
        }
    }

    private static String strip(String s) {
        return s == null ? null : s.strip();
    }

    private static String emptyToNull(String s) {
        String t = strip(s);
        return t == null || t.isEmpty() ? null : t;
    }
}
