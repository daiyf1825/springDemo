package com.xz.demo.util;

import com.google.common.base.Throwables;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

/**
 * Token 工具类
 * 过期时间单位是秒,而不是毫秒
 * Created by yangyang on 16/5/16.
 */
public class TokenUtil {

    private static final Logger log = LoggerFactory.getLogger(TokenUtil.class);
    private static final String salt = "0beb009a3213370e6cc54a7ebb989bf2e4db247fa115f31c6f95a94b7a0e";
    private static final int BS = 16;
    /**
     * 默认过期时间长度为 7 天
     */
    private static final int DEFAULT_EXPIRATION = 7 * 24 * 3600;

    /**
     * @param msg        待加密的数据字符串（非JSON字符串）
     * @param enc        加密 or 解密
     * @param expiration 加密字符串有效时间
     * @return NULL 无效数据，处理失败
     * @throws Exception
     */
    public static String token(String msg, boolean enc, Integer expiration) {
        try {
            if (!enc) {
                // 默认有效期 7D
                expiration = Objects.isNull(expiration) ? DEFAULT_EXPIRATION : expiration;
                // token 有效长度校验
                if (msg.length() < 42) {
                    return null;
                }
                // 后 32 位为sign
                String _hmac = msg.substring(msg.length() - 32);
                // sign字符串前10位为时间戳，单位：秒
                String _time = msg.substring(msg.length() - 42).substring(0, 10);
                // msg 除去后面42位（10位时间戳 + 32位sign）之前的为context信息
                String context = msg.substring(0, msg.length() - 42); // msg.replace(msg.substring(msg.length() - 42), "");
                // salt key
                String key = MD5Util.encode(_time + salt, "UTF-8");//加密key
                // 时间有效性判断
                if (System.currentTimeMillis() / 1000 - Long.parseLong(_time) > expiration) {
                    log.error("token expired: {}", msg);
                    return null;
                }
                // 验签（签名有效性判断）
                if (!_hmac.equals(bytesToHexString(hmacEncrypt(context + _time, key)))) {
                    log.error("token invalid: {}", msg);
                    return null;
                }
                // 返回解密后的信息
                return decrypt(context, key).trim(); //去掉前后空格(实际只去掉后面不满16位不上的空格,前面没有空格的)
            } else {
                // 当前时间戳 单位：秒
                String _time = String.valueOf((System.currentTimeMillis()) - 3).substring(0, 10);
                // salt key
                String key = MD5Util.encode(_time + salt, "UTF-8");//加密key
                // 待加密信息与salt key 组合进行ASE加密，生成加密内容
                String context = (encrypt(pad(msg), key)).trim();//为了和python兼容,msg需要做json.dumps()处理
                // sign(ASE密文+时间戳作为待加密的字符串，使用KEY作为密钥生成HmacMD5签名)
                String hmac = bytesToHexString(hmacEncrypt(context + _time, key));
                return context + _time + hmac;
            }
        } catch (Exception e) {
            log.error("handle exception: {}", Throwables.getStackTraceAsString(e));
            return null;
        }
    }

    /**
     * 从token中解析出uid
     *
     * @param token 原始token
     * @return null 如果 token过期 or 无效token or token为空
     */
    public static Long tokenUid(String token) {
        if (StringUtils.isBlank(token)) return null;
        String ret = token(token, false, null);
        if (RegexUtil.checkNumeric(ret)) {
            return Long.parseLong(ret);
        }
        return null;
    }

    private static String pad(String s) {
        int len = (BS - s.length() % BS);
        char c = (char) (BS - s.length() % BS);
        for (int i = 0; i < len; i++) {
            s += c;
        }
        return s;
    }

    /**
     * 字节数组转字符串
     *
     * @param src 字节数组
     * @return 字符串
     */
    private static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder();
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    private static String encrypt(String src, String key) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(key.substring(0, 16).getBytes(), "AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(key.substring(16).getBytes(StandardCharsets.UTF_8)));
            return new String(Base64.getEncoder().encode(cipher.doFinal(src.getBytes())));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String decrypt(String src, String key) {
        String decrypted;
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(key.substring(0, 16).getBytes(), "AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(key.substring(16).getBytes(StandardCharsets.UTF_8)));
            decrypted = new String(cipher.doFinal(Base64.getDecoder().decode(src)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return decrypted;
    }

    private static byte[] hmacEncrypt(String encryptText, String encryptKey) throws Exception {
        byte[] data = encryptKey.getBytes(StandardCharsets.UTF_8);
        //根据给定的字节数组构造一个密钥,第二参数指定一个密钥算法的名称
        SecretKey secretKey = new SecretKeySpec(data, "HmacMD5");
        //生成一个指定 Mac 算法 的 Mac 对象
        Mac mac = Mac.getInstance("HmacMD5");
        //用给定密钥初始化 Mac 对象
        mac.init(secretKey);
        byte[] text = encryptText.getBytes(StandardCharsets.UTF_8);
        //完成 Mac 操作
        return mac.doFinal(text);
    }

}
