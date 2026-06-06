package com.qiujie.service;

import com.alibaba.fastjson.JSON;
import com.qiujie.entity.FileTask;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FileTaskSseService {

    private static final long SSE_TIMEOUT = 30 * 60 * 1000L;

    // userId -> 该用户的所有活跃 SseEmitter
    private final ConcurrentHashMap<Integer, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Integer userId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        emitters.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(emitter);

        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(e -> remove(userId, emitter));

        // 发送初始事件确认连接
        try {
            emitter.send(SseEmitter.event().name("connected").data("OK"));
        } catch (IOException e) {
            remove(userId, emitter);
        }
        return emitter;
    }

    public void emit(FileTask task) {
        Integer userId = task.getOperatorId();
        if (userId == null) return;
        Set<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null || userEmitters.isEmpty()) return;

        String data = JSON.toJSONString(task);
        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event().name("task-update").data(data));
            } catch (IOException e) {
                emitter.completeWithError(e);
                remove(userId, emitter);
            }
        }
    }

    private void remove(Integer userId, SseEmitter emitter) {
        Set<SseEmitter> set = emitters.get(userId);
        if (set != null) {
            set.remove(emitter);
            if (set.isEmpty()) {
                emitters.remove(userId);
            }
        }
    }
}
