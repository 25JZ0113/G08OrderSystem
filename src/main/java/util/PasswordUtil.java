package util;

import java.nio.charset.StandardCharsets;

import org.mindrot.jbcrypt.BCrypt;

/**
 * パスワードのハッシュ化と照合(BCrypt)
 * 使うライブラリ:jBCrypt 0.4(src/main/webapp/WEB-INF/lib/jbcrypt-0.4.jar)
 *
 * USERS.PASSWORD には BCrypt のハッシュ値(60 文字。例:$2a$10$...)を保存する
 *   $2a$ … BCrypt のバージョン  10$ … コスト(2 の 10 乗 = 1024 回)
 *   次の 22 文字 … ソルト(毎回ランダムに作る)  残り 31 文字 … ハッシュ結果
 * ソルトはハッシュ値の中に入っているので、別の列に保存しなくてよい
 *
 * 登録・変更のとき … hash() でハッシュ化して保存する
 * ログインのとき   … matches() で、入力されたパスワードと保存されているハッシュ値を照合する
 */
public class PasswordUtil {

    /** コスト。1 増やすと計算量はおよそ 2 倍(ログインにかかる時間とのバランスで 10 にする) */
    private static final int COST = 10;

    /** BCrypt で扱えるパスワードの長さの上限(文字数ではなく UTF-8 のバイト数) */
    public static final int MAX_BYTES = 72;

    private PasswordUtil() {}

    /** パスワードをハッシュ化する(ソルトは自動で作られ、ハッシュ値に含まれる)。ユーザー登録・パスワード変更で使う */
    public static String hash(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(COST));
    }

    /** 入力されたパスワードが、保存されているハッシュ値と一致するか(ハッシュ値の形がおかしいときは false) */
    public static boolean matches(String password, String hashed) {
        if (password == null || hashed == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(password, hashed);
        } catch (IllegalArgumentException e) {
            // BCrypt の形ではない値(古い方式で保存したままの値など)
            return false;
        }
    }

    /** BCrypt で扱える長さ(UTF-8 で 72 バイト以内)か */
    public static boolean isWithinMaxBytes(String password) {
        return password.getBytes(StandardCharsets.UTF_8).length <= MAX_BYTES;
    }
}
