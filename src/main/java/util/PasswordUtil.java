package util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * パスワードのハッシュ化と照合
 * USERS.PASSWORD には PBKDF2(HmacSHA256・100000 回・256bit)でハッシュ化した値を、
 * USERS.SALT にはソルトを、どちらも Base64 で保存している
 */
public class PasswordUtil {

    private static final String ALGORITHM  = "PBKDF2WithHmacSHA256";
    private static final int    ITERATIONS = 100000;
    private static final int    KEY_LENGTH = 256;   // bit
    private static final int    SALT_BYTES = 16;

    private PasswordUtil() {}

    /** 入力されたパスワードが、保存されているハッシュと一致するか */
    public static boolean matches(String password, String hashBase64, String saltBase64) {
        byte[] salt     = Base64.getDecoder().decode(saltBase64);
        byte[] expected = Base64.getDecoder().decode(hashBase64);
        byte[] actual   = hash(password, salt);
        // 比較にかかる時間で推測されないように、MessageDigest.isEqual で比べる
        return MessageDigest.isEqual(expected, actual);
    }

    /** 新しいソルトを作る(Base64)。ユーザー登録・パスワード変更で使う */
    public static String newSalt() {
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /** パスワードをハッシュ化する(Base64)。ユーザー登録・パスワード変更で使う */
    public static String hash(String password, String saltBase64) {
        return Base64.getEncoder().encodeToString(hash(password, Base64.getDecoder().decode(saltBase64)));
    }

    private static byte[] hash(String password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("パスワードのハッシュ化に失敗しました", e);
        } finally {
            spec.clearPassword();
        }
    }
}
