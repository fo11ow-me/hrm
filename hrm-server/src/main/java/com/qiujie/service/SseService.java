package com.qiujie.service;

import com.alibaba.fastjson.JSON;
import com.qiujie.entity.FileTask;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通用 SSE 服务，支持任务进度推送和业务通知推送。
 *
 * @author qiujie
 */
@Service
public class SseService {

    private static final long SSE_TIMEOUT = 30 * 60 * 1000L;

    private final ConcurrentHashMap<Integer, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Integer userId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        // 线程安全的 Set（写入操作通过 sync 保护，读操作可以并发）
        Set<SseEmitter> set = Collections.newSetFromMap(new ConcurrentHashMap<>());
        Set<SseEmitter> existing = emitters.putIfAbsent(userId, set);
        (existing != null ? existing : set).add(emitter);

        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(e -> remove(userId, emitter));

        try {
            emitter.send(SseEmitter.event().name("connected").data("OK"));
        } catch (IOException e) {
            remove(userId, emitter);
        }
        return emitter;
    }

    /** 推送导入导出任务更新（向后兼容） */
    public void emit(FileTask task) {
        if (task.getOperatorId() == null) return;
        emit(task.getOperatorId(), "task-update", task);
    }

    /** 通用推送。对 emitter 集合做快照遍历，避免并发 remove 干扰。 */
    public void emit(Integer userId, String eventName, Object payload) {
        if (userId == null) return;
        Set<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null || userEmitters.isEmpty()) return;

        String data = JSON.toJSONString(payload);
        // 快照遍历：copy 到数组后迭代，不直接遍历原始 Set
        for (SseEmitter emitter : new ArrayList<>(userEmitters)) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException e) {
                emitter.completeWithError(e);
                remove(userId, emitter);
            }
        }
    }

    /** 原子移除：使用 computeIfPresent 保证 check-then-act 是原子的 */
    private void remove(Integer userId, SseEmitter emitter) {
        emitters.computeIfPresent(userId, (key, set) -> {
            set.remove(emitter);
            return set.isEmpty() ? null : set;  // 返回 null 从 map 中删除该 entry
        });
    }
}
