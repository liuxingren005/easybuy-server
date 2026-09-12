package org.maven.util;

import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.util.encoders.Hex;

import java.nio.charset.StandardCharsets;

/**
 * 国密安全工具类
 *
 * 256 位（32 字节）
 */
public final class SecurityUtil {

    private SecurityUtil() {

    }

    /**
     * SM3 密码加密
     *
     * @param input 明文密码
     * @return 64 位十六进制 SM3 密文
     */
    public static String sm3(String input) {
        SM3Digest digest = new SM3Digest();
        byte[] data = input.getBytes(StandardCharsets.UTF_8);
        digest.update(data, 0, data.length);
        byte[] hash = new byte[digest.getDigestSize()];
        digest.doFinal(hash, 0);
        return Hex.toHexString(hash);
    }

    /**
     * 校验明文密码与密文是否匹配
     *
     * @param input    明文密码
     * @param encrypted SM3 密文
     * @return true / false
     */
    public static boolean verify(String input, String encrypted) {
        return sm3(input).equalsIgnoreCase(encrypted);
    }
}
