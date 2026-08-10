package com.zyj.orderservice.webSocket;

import com.sky.constant.JwtClaimsConstant;
import com.sky.properties.JwtProperties;
import com.sky.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket 服务
 * 客户端通过查询参数传递 JWT：ws://host/ws/任意sid?token=xxx
 * JWT 在 @OnOpen 中校验，校验失败立即关闭连接。
 * sid 由服务端根据 JWT 中的身份信息（merchantId/userId）生成，不信任客户端传入值。
 * 商家端 token 需包含 empMerchantId claim，用户端 token 需包含 userId claim。
 */
@Component
@ServerEndpoint("/ws/{sid}")
@Slf4j
public class WebSocketServer {

    /**
     * WebSocketServer 实例由 jakarta.websocket 容器创建（非 Spring 管理），
     * 实例字段注入不生效，故用 static + setter 注入方式
     */
    private static JwtProperties jwtProperties;

    @Autowired
    public void setJwtProperties(JwtProperties jwtProperties) {
        WebSocketServer.jwtProperties = jwtProperties;
    }

    private static ConcurrentHashMap<String, Session> sessionMap = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<Long, CopyOnWriteArraySet<String>> merchantSessions = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, Long> SID_MERCHANT = new ConcurrentHashMap<>();

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String clientSid) {
        String token = getQueryParam(session, "token");
        if (token == null || token.isEmpty()) {
            log.warn("WebSocket 连接拒绝：缺少 token");
            closeSession(session, "缺少认证 token");
            return;
        }

        Long merchantId = null;
        Long userId = null;
        try {
            Claims claims;
            try {
                claims = JwtUtil.parseJWT(jwtProperties.getAdminSecretKey(), token);
                merchantId = claims.get(JwtClaimsConstant.EMP_MERCHANT_ID, Long.class);
            } catch (Exception e) {
                claims = JwtUtil.parseJWT(jwtProperties.getUserSecretKey(), token);
                userId = claims.get(JwtClaimsConstant.USER_ID, Long.class);
            }
        } catch (Exception e) {
            log.warn("WebSocket 连接拒绝：JWT 校验失败，error={}", e.getMessage());
            closeSession(session, "token 校验失败");
            return;
        }

        if (merchantId == null && userId == null) {
            log.warn("WebSocket 连接拒绝：token 中无有效身份标识");
            closeSession(session, "无效的 token");
            return;
        }

        String sid = (merchantId != null)
                ? "shop_" + merchantId + "_" + session.getId()
                : "user_" + (userId == null ? "anon" : userId) + "_" + session.getId();

        sessionMap.put(sid, session);
        if (merchantId != null) {
            merchantSessions.computeIfAbsent(merchantId, k -> new CopyOnWriteArraySet<>()).add(sid);
            SID_MERCHANT.put(sid, merchantId);
        }

        log.info("WebSocket 连接建立: sid={}, merchantId={}, userId={}, sessionId={}, 当前在线会话数: {}",
                sid, merchantId, userId, session.getId(), sessionMap.size());
    }

    @OnMessage
    public void onMessage(String message, @PathParam("sid") String sid) {
        log.info("WebSocket 收到客户端消息: sid={}, message={}", sid, message);
    }

    /**
     * 连接断开时调用的方法
     */
    @OnClose
    public void onClose(@PathParam("sid") String sid) {
        sessionMap.remove(sid);
        Long merchantId = SID_MERCHANT.remove(sid);
        if (merchantId != null) {
            CopyOnWriteArraySet<String> sessions = merchantSessions.get(merchantId);
            if (sessions != null) {
                sessions.remove(sid);
                if (sessions.isEmpty()) {
                    merchantSessions.remove(merchantId);
                }
            }
        }
        log.info("连接断开: sid={}", sid);
    }

    public void sendToAllClient(String message) {
        Collection<Session> sessions = sessionMap.values();
        for (Session session : sessions) {
            if (session.isOpen()) {
                session.getAsyncRemote().sendText(message, result -> {
                    if (!result.isOK()) {
                        log.error("群发异步发送失败: sessionId={}", session.getId(), result.getException());
                    }
                });
            }
        }
        log.info("已向 {} 个客户端群发消息", sessions.size());
    }

    public void sendToOneClient(String sid, String message) {
        Session session = sessionMap.get(sid);
        if (session != null && session.isOpen()) {
            session.getAsyncRemote().sendText(message, result -> {
                if (!result.isOK()) {
                    log.error("单播异步发送失败: sid={}", sid, result.getException());
                    sessionMap.remove(sid);
                }
            });
            log.info("向客户端 {} 发送消息: {}", sid, message);
        } else {
            log.warn("客户端 {} 不在线或会话已关闭", sid);
        }
    }

    public void sendToMerchant(Long merchantId, String message) {
        CopyOnWriteArraySet<String> sids = merchantSessions.get(merchantId);
        if (sids == null || sids.isEmpty()) {
            log.warn("商家 {} 没有在线会话", merchantId);
            return;
        }
        for (String sid : sids) {
            Session session = sessionMap.get(sid);
            if (session != null && session.isOpen()) {
                session.getAsyncRemote().sendText(message, result -> {
                    if (!result.isOK()) {
                        log.error("商家推送异步发送失败: sid={}", sid, result.getException());
                    }
                });
            } else {
                sessionMap.remove(sid);
                sids.remove(sid);
            }
        }
        log.info("已向商家 {} 的 {} 个会话推送消息", merchantId, sids.size());
    }

    private String getQueryParam(Session session, String paramName) {
        String queryString = session.getQueryString();
        if (queryString == null || queryString.isEmpty()) {
            return null;
        }
        for (String pair : queryString.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && paramName.equals(kv[0])) {
                return kv[1];
            }
        }
        return null;
    }

    private void closeSession(Session session, String reason) {
        try {
            session.close(new jakarta.websocket.CloseReason(
                    jakarta.websocket.CloseReason.CloseCodes.VIOLATED_POLICY, reason));
        } catch (Exception e) {
            log.error("关闭 WebSocket 会话失败", e);
        }
    }
}