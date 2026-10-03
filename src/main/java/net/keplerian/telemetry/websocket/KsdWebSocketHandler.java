package net.keplerian.telemetry.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.keplerian.telemetry.model.ObjectInfoMessage;
import net.keplerian.telemetry.model.SpaceObjectInput;
import net.keplerian.telemetry.model.TelemetryMessage;
import net.keplerian.telemetry.store.SelectedHistory;
import net.keplerian.telemetry.store.TelemetryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Pattern;

@Component
public class KsdWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(KsdWebSocketHandler.class);

    private static final String QUERY_OBJECTS = "{\"messageType\":\"QueryObjects\"}";
    private static final String QUERY_TELEMETRY = "{\"messageType\":\"QueryTelemetry\"}";

    // -nan(ind) / nan / -inf など C++ 非数値リテラルを null に置換する。文字列値の内側には一致しない
    private static final Pattern NON_NUMERIC_PATTERN =
            Pattern.compile("(?<![\"\\w])-?(?:nan|inf(?:inity)?)(?:\\([^)]*\\))?(?![\"\\w])", Pattern.CASE_INSENSITIVE);

    private final ObjectMapper objectMapper;
    private final TelemetryStore store;
    private final SelectedHistory selectedHistory;
    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    public KsdWebSocketHandler(ObjectMapper objectMapper, TelemetryStore store, SelectedHistory selectedHistory) {
        this.objectMapper = objectMapper;
        this.store = store;
        this.selectedHistory = selectedHistory;
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        sessions.add(session);
        log.info("KSD connected: {}", session.getId());
        send(session, QUERY_OBJECTS);
        log.debug("Sent QueryObjects to {}", session.getId());
        requestTelemetry();
    }

    /**
     * 1秒ごとに KSD へテレメトリを要求する。選択中の宇宙機の履歴（SelectedHistory）を、
     * ダッシュボードが開かれているかどうかに関係なく途切れずに記録するため
     */
    @Scheduled(fixedRate = 1000)
    public void requestTelemetry() {
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) continue;
            try {
                send(session, QUERY_TELEMETRY);
                log.debug("Sent QueryTelemetry (scheduled) to {}", session.getId());
            } catch (Exception e) {
                log.warn("Failed to send QueryTelemetry to {}: {}", session.getId(), e.getMessage());
            }
        }
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) {
        try {
            log.debug("Raw payload: {}", message.getPayload());
            String sanitized = NON_NUMERIC_PATTERN.matcher(message.getPayload()).replaceAll("null");
            JsonNode root = objectMapper.readTree(sanitized);

            String messageType = root.path("messageType").asText("");
            switch (messageType) {
                case "Telemetry"  -> handleTelemetry(objectMapper.treeToValue(root, TelemetryMessage.class));
                case "ObjectList" -> handleObjectInfo(objectMapper.treeToValue(root, ObjectInfoMessage.class));
                default           -> log.warn("Unknown messageType '{}' from {}", messageType, session.getId());
            }
        } catch (Exception e) {
            log.error("Failed to process message from {}: {}", session.getId(), e.getMessage());
            log.error("Failing payload: {}", message.getPayload());
        }
    }

    // WebSocketSession は同時送信に対応していない（スケジューラと接続確立時のスレッドから送りうる）
    private static void send(WebSocketSession session, String payload) throws java.io.IOException {
        synchronized (session) {
            session.sendMessage(new TextMessage(payload));
        }
    }

    private void handleTelemetry(TelemetryMessage msg) {
        store.setCurrentTime((long) msg.currentTime());
        store.setSelectedId(msg.selectedId());
        selectedHistory.record(msg.selectedId(), msg.currentTime(), msg.selectedState());
        for (SpaceObjectInput o : msg.spaceObjects()) {
            store.putTelemetry(o.id(), o.cart(), o.kep(), o.orbitRev(), o.orbitLegs());
        }
        log.debug("Updated {} objects at t={}", msg.spaceObjects().size(), msg.currentTime());
    }

    private void handleObjectInfo(ObjectInfoMessage msg) {
        for (var info : msg.spaceObjects()) {
            store.putInfo(info);
        }
        log.info("Stored info for {} objects", msg.spaceObjects().size());
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        sessions.remove(session);
        log.info("KSD disconnected: {} ({})", session.getId(), status);
    }

    @Override
    public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable exception) {
        log.error("Transport error [{}]: {}", session.getId(), exception.getMessage());
    }
}
