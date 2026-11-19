package util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import jakarta.servlet.ServletContext;

/**
 * 商品画像(メニュー管理画面でアップロードしたもの)を保存する
 *
 * 画像はブラウザで 4:3・1024×768 の JPG にしてから送られてくる(js/imageCrop.js)。
 * 保存先は 2 か所:
 *   1. 配備先の images(Tomcat が実際に表示している場所。保存してすぐ表示される)
 *   2. プロジェクトの src/main/webapp/images(Eclipse で「クリーン」しても消えない。Git でコミットできる)
 * 2 の場所は PC ごとに違うので、WEB-INF/upload.properties の project.images.dir に書く
 * (upload.properties は Git に上げない。upload.properties.example をコピーして作る)。
 * upload.properties がなければ 1 だけに保存する。
 */
public class ImageStore {

    /** アップロードできる画像の大きさ(ブラウザで縮める前の元の画像。ブラウザ側でも確かめる) */
    public static final long MAX_BYTES = 10L * 1024 * 1024;

    private static final String CONFIG = "/WEB-INF/upload.properties";
    private static final String KEY    = "project.images.dir";

    private ImageStore() {}

    /** ファイル名の拡張子を .jpg にそろえる(blend.png → blend.jpg、blend → blend.jpg) */
    public static String toJpgName(String name) {
        String base = name.replaceFirst("(?i)\\.(jpe?g|png|webp)$", "");
        return base + ".jpg";
    }

    /** 送られてきたデータが JPG の画像として読めるか */
    public static boolean isJpeg(byte[] data) {
        if (data.length < 3 || (data[0] & 0xFF) != 0xFF || (data[1] & 0xFF) != 0xD8 || (data[2] & 0xFF) != 0xFF) {
            return false;
        }
        try (InputStream in = new ByteArrayInputStream(data)) {
            return ImageIO.read(in) != null;
        } catch (IOException e) {
            return false;
        }
    }

    /** 同じ名前の画像が、どちらかの保存先にすでにあるか */
    public static boolean exists(ServletContext ctx, String fileName) {
        for (Path dir : dirs(ctx)) {
            if (Files.exists(dir.resolve(fileName))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 画像を保存する。保存したファイルの一覧を返す(DB の登録に失敗したときに消すため)
     * 同じ名前のファイルがあるときは上書きせずに IOException
     */
    public static List<Path> save(ServletContext ctx, String fileName, byte[] data) throws IOException {
        List<Path> saved = new ArrayList<>();
        try {
            for (Path dir : dirs(ctx)) {
                Files.createDirectories(dir);
                Path file = dir.resolve(fileName);
                Files.write(file, data, StandardOpenOption.CREATE_NEW);
                saved.add(file);
            }
        } catch (IOException e) {
            delete(saved);
            throw e;
        }
        return saved;
    }

    /** 保存した画像を消す */
    public static void delete(List<Path> files) {
        for (Path f : files) {
            try {
                Files.deleteIfExists(f);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /** プロジェクトの images にも保存するか(upload.properties があるか) */
    public static boolean savesToProject(ServletContext ctx) {
        return projectDir(ctx) != null;
    }

    /** 保存先(配備先の images と、設定があればプロジェクトの images) */
    private static List<Path> dirs(ServletContext ctx) {
        List<Path> dirs = new ArrayList<>();
        String real = ctx.getRealPath("/images");
        if (real != null) {
            dirs.add(Path.of(real));
        }
        Path project = projectDir(ctx);
        if (project != null && dirs.stream().noneMatch(d -> sameDir(d, project))) {
            dirs.add(project);
        }
        return dirs;
    }

    private static Path projectDir(ServletContext ctx) {
        // Properties.load は「\」を特別な文字として扱い、Windows のパス(C:\Users\…)が崩れるので、1 行ずつ読む
        try (InputStream in = ctx.getResourceAsStream(CONFIG)) {
            if (in == null) {
                return null;
            }
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            for (String line : text.split("\\R")) {
                String t = line.strip();
                if (t.startsWith(KEY + "=")) {
                    String dir = t.substring(KEY.length() + 1).strip();
                    return dir.isEmpty() ? null : Path.of(dir);
                }
            }
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static boolean sameDir(Path a, Path b) {
        return a.toAbsolutePath().normalize().equals(b.toAbsolutePath().normalize());
    }
}
