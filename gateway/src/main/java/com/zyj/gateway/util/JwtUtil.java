package com.zyj.gateway.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Map;

public class JwtUtil {
    /**
     * 生成jwt
     * 使用Hs256算法, 私匙使用固定秘钥
     *
     * @param secretKey jwt秘钥
     * @param ttlMillis jwt过期时间(毫秒)
     * @param claims    设置的信息
     * @return
     */
//    public static String createJWT(String secretKey, long ttlMillis, Map<String, Object> claims) {
//        // 指定签名的时候使用的签名算法，也就是header那部分
//        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
//
//        // 生成JWT的时间
//        long expMillis = System.currentTimeMillis() + ttlMillis;
//        Date exp = new Date(expMillis);
//
//        // 设置jwt的body
//        JwtBuilder builder = Jwts.builder()
//                // 如果有私有声明，一定要先设置这个自己创建的私有的声明，这个是给builder的claim赋值，一旦写在标准的声明赋值之后，就是覆盖了那些标准的声明的
//                .setClaims(claims)
//                // 设置签名使用的签名算法和签名使用的秘钥
//                .signWith(signatureAlgorithm, secretKey.getBytes(StandardCharsets.UTF_8))
//                // 设置过期时间
//                .setExpiration(exp);
//
//        return builder.compact();
//    }

    /**
     * Token解密
     *
     * @param secretKey jwt秘钥 此秘钥一定要保留好在服务端, 不能暴露出去, 否则sign就可以被伪造, 如果对接多个客户端建议改造成多个
     * @param token     加密后的token
     * @return
     */
//    public static Claims parseJWT(String secretKey, String token) {
//        // 得到DefaultJwtParser
//        Claims claims = Jwts.parser()
//                // 设置签名的秘钥
//                .setSigningKey(secretKey.getBytes(StandardCharsets.UTF_8))
//                // 设置需要解析的jwt
//                .parseClaimsJws(token).getBody();
//        return claims;
//    }

    /**
     * 创建 JWT
     *
     * @param secretKey 密钥
     * @param ttlMillis 过期时间（毫秒）
     * @param claims 私有声明
     * @return 生成的 JWT
     */

//    public static String createJWT(String secretKey, long ttlMillis, Map<String, Object> claims) {
//        // secretKey 密钥 ttlMillis 过期时间 claims 负载
//        // 指定签名的时候使用的签名算法，也就是header那部分 // 选择签名算法，使用 HMAC SHA-256 算法
//        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
//
//        //设置过期时间
//        long expMillis = System.currentTimeMillis() + ttlMillis;
//        Date exp = new Date(expMillis); // // 将毫秒转化为 Date 对象
//        String jwt = Jwts.builder()
//                .setClaims(claims) //设置私有声明
//                .setIssuedAt(new Date()) // 设置签发时间
//                .setExpiration(exp) // 设置过期时间
//                .signWith(signatureAlgorithm, secretKey.getBytes(StandardCharsets.UTF_8)) // 设置签名和密钥
//                .compact();
//        return jwt;
//    }

    public static String createJWT(String secretKey, long ttlMillis, Map<String, Object> claims) {
        // 将字符串密钥转换为 Base64 编码的 Key
        Key key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

        String token = Jwts.builder()
                .setClaims(claims)
                .signWith(key, SignatureAlgorithm.HS256)  // 使用 Key 对象
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ttlMillis))
                .compact();

        return token;
    }

    public static Claims parseJWT(String secretKey, String token) {
        try {
            // 将字符串密钥转换为 Base64 编码的 Key
            Key key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

            Claims claims = Jwts.parserBuilder()  // 使用 parserBuilder() 替代 parser()
                    .setSigningKey(key)  // 使用 Key 对象
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims;
        } catch (ExpiredJwtException e) {
            throw new RuntimeException("JWT token has expired: " + e.getMessage(), e);
        } catch (SignatureException e) {
            throw new RuntimeException("Invalid JWT signature: " + e.getMessage(), e);
        } catch (MalformedJwtException e) {
            throw new RuntimeException("Invalid JWT token format: " + e.getMessage(), e);
        } catch (UnsupportedJwtException e) {
            throw new RuntimeException("Unsupported JWT token: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Illegal JWT token argument: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Invalid JWT token - " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }


    /**
     * 验证 JWT
     */
//    public static Claims parseJWT(String secretKey, String token) {
//        // 解析JWT并验证签名，获取JWT的Claims部分
//        try {
//            return Jwts.parserBuilder()
//                    .setSigningKey(secretKey.getBytes(StandardCharsets.UTF_8))  // ① 设置密钥
//                    .build()                                                    // ② 构建解析器
//                    .parseClaimsJws(token)                                     // ③ 解析并验证签名
//                    .getBody();                                                // ④ 获取载荷数据
//            /**
//             * 详细过程：
//             * ① 设置签名密钥
//             * 使用相同的密钥（secretKey）来验证签名
//             * 密钥必须与签发时使用的密钥完全一致
//             * ② 构建解析器
//             * 创建 JWT 解析器实例
//             * 准备好验证环境
//             * ③ 核心验证过程 当执行 parseClaimsJws(token) 时，系统会：
//             * 解析 JWT 结构：将 token 分解为 header.payload.signature 三部分
//             * 验证签名：
//             * 用 header 中指定的算法（如 HS256）
//             * 用你提供的密钥重新计算签名
//             * 对比计算出的签名与 token 中的签名是否一致
//             * 检查有效期：验证 exp（过期时间）是否已过期
//             * 检查生效时间：验证 nbf（not before）是否已到达
//             * ④ 获取载荷
//             * 验证通过后，返回 Claims 对象
//             * 包含 JWT 中存储的所有信息（用户ID、用户名等）
//             */
//        } catch (Exception e) {
//            // 解析失败或签名验证失败的情况
//            throw new RuntimeException("Invalid JWT token", e);
//        }
//    }


}
