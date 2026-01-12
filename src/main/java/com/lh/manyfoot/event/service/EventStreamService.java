package com.lh.manyfoot.event.service;

import com.lh.manyfoot.event.domain.ManusEvent;
import com.lh.manyfoot.event.domain.SessionState;
import com.lh.manyfoot.service.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * 事件流服务
 * 负责事件的持久化存储和实时推送
 *
 * @author airx
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "manus.sandbox", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EventStreamService {

    // Redis 键前缀
    private static final String EVENT_STREAM_KEY = "manus:events:stream:";
    private static final String EVENT_SEQUENCE_KEY = "manus:events:seq:";
    private static final String SESSION_STATE_KEY = "manus:session:state:";
    private static final String EVENT_CHANNEL_PREFIX = "manus:events:channel:";

    // 事件保留时间 (7天)
    private static final Duration EVENT_RETENTION = Duration.ofDays(7);

    /**
     * 追加事件到流
     *
     * @param sessionId 会话ID
     * @param event     事件
     * @return 事件ID
     */
    public String appendEvent(String sessionId, ManusEvent event) {
        // 获取序列号
        long sequence = getNextSequence(sessionId);
        event.setSequence(sequence);
        if (event.getEventId() == null) {
            event.setEventId(sessionId + "-" + sequence);
        }
        if (event.getTimestamp() == null) {
            event.setTimestamp(System.currentTimeMillis());
        }
        event.setSessionId(sessionId);

        // 存储到 Redis List
        String key = EVENT_STREAM_KEY + sessionId;
        List<ManusEvent> events = RedisUtils.getCacheObject(key);
        if (events == null) {
            events = new ArrayList<>();
        }
        events.add(event);
//        RedisUtils.setCacheObject(key, events, EVENT_RETENTION);

        // 发布事件通知 (用于实时推送)
        publishEvent(sessionId, event);

        log.debug("事件已追加: sessionId={}, eventId={}, type={}",
            sessionId, event.getEventId(), event.getEventType());

        return event.getEventId();
    }

    /**
     * 获取会话的所有事件
     *
     * @param sessionId 会话ID
     * @return 事件列表
     */
    public List<ManusEvent> getEvents(String sessionId) {
        String key = EVENT_STREAM_KEY + sessionId;
        List<ManusEvent> events = RedisUtils.getCacheObject(key);
        return events != null ? events : Collections.emptyList();
    }

    /**
     * 获取指定序号之后的事件 (用于断线重连)
     *
     * @param sessionId     会话ID
     * @param afterSequence 指定序号
     * @return 事件列表
     */
    public List<ManusEvent> getEventsSince(String sessionId, long afterSequence) {
        List<ManusEvent> allEvents = getEvents(sessionId);
        return allEvents.stream()
            .filter(e -> e.getSequence() != null && e.getSequence() > afterSequence)
            .sorted((a, b) -> Long.compare(a.getSequence(), b.getSequence()))
            .toList();
    }

    /**
     * 获取最新的N条事件
     *
     * @param sessionId 会话ID
     * @param count     数量
     * @return 事件列表
     */
    public List<ManusEvent> getRecentEvents(String sessionId, int count) {
        List<ManusEvent> allEvents = getEvents(sessionId);
        int size = allEvents.size();
        if (size <= count) {
            return new ArrayList<>(allEvents);
        }
        return new ArrayList<>(allEvents.subList(size - count, size));
    }

    /**
     * 订阅事件流
     *
     * @param sessionId 会话ID
     * @param consumer  事件消费者
     */
    public void subscribeEvents(String sessionId, Consumer<ManusEvent> consumer) {
        String channel = EVENT_CHANNEL_PREFIX + sessionId;
        RedisUtils.subscribe(channel, ManusEvent.class, consumer);
        log.info("已订阅事件流: sessionId={}", sessionId);
    }

    /**
     * 发布事件 (通过 Redis Pub/Sub)
     */
    private void publishEvent(String sessionId, ManusEvent event) {
        String channel = EVENT_CHANNEL_PREFIX + sessionId;
        RedisUtils.publish(channel, event);
    }

    /**
     * 保存会话状态
     *
     * @param sessionId 会话ID
     * @param state     会话状态
     */
    public void saveSessionState(String sessionId, SessionState state) {
        String key = SESSION_STATE_KEY + sessionId;
        state.touch();
        RedisUtils.setCacheObject(key, state, EVENT_RETENTION);
        log.debug("会话状态已保存: sessionId={}, phase={}, iteration={}",
            sessionId, state.getCurrentPhase(), state.getCurrentIteration());
    }

    /**
     * 恢复会话状态
     *
     * @param sessionId 会话ID
     * @return 会话状态，如果不存在返回 null
     */
    public SessionState restoreSessionState(String sessionId) {
        String key = SESSION_STATE_KEY + sessionId;
        SessionState state = RedisUtils.getCacheObject(key);
        if (state != null) {
            log.info("会话状态已恢复: sessionId={}, phase={}, iteration={}",
                sessionId, state.getCurrentPhase(), state.getCurrentIteration());
        }
        return state;
    }

    /**
     * 删除会话状态
     *
     * @param sessionId 会话ID
     */
    public void deleteSessionState(String sessionId) {
        RedisUtils.deleteObject(SESSION_STATE_KEY + sessionId);
        RedisUtils.deleteObject(EVENT_STREAM_KEY + sessionId);
        RedisUtils.deleteObject(EVENT_SEQUENCE_KEY + sessionId);
        log.info("会话状态已删除: sessionId={}", sessionId);
    }

    /**
     * 获取下一个序列号
     */
    private long getNextSequence(String sessionId) {
        String key = EVENT_SEQUENCE_KEY + sessionId;
        return RedisUtils.incrAtomicValue(key);
    }

    /**
     * 获取当前序列号
     */
    public long getCurrentSequence(String sessionId) {
        String key = EVENT_SEQUENCE_KEY + sessionId;
        Long seq = RedisUtils.getAtomicValue(key);
        return seq != null ? seq : 0L;
    }

    /**
     * 检查会话是否存在
     */
    public boolean sessionExists(String sessionId) {
        return RedisUtils.hasKey(SESSION_STATE_KEY + sessionId);
    }
}
