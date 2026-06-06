package com.qiujie.service;

import com.qiujie.entity.FileTask;
import com.qiujie.enums.TaskStatusEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FileTaskSseService 单元测试。
 * 验证 SSE 订阅、事件发射和连接生命周期管理。
 *
 * @author qiujie
 */
class FileTaskSseServiceUnitTest {

    private FileTaskSseService sseService;

    @BeforeEach
    void setUp() {
        sseService = new FileTaskSseService();
    }

    @AfterEach
    void tearDown() {
        clearEmitters();
    }

    // ==================== subscribe ====================

    @Test
    void subscribe_ShouldCreateEmitter() {
        SseEmitter emitter = sseService.subscribe(1);
        assertThat(emitter).isNotNull();
        assertThat(getEmittersForUser(1)).hasSize(1);
    }

    @Test
    void subscribe_MultipleTimesForSameUser_ShouldCreateMultipleEmitters() {
        sseService.subscribe(1);
        sseService.subscribe(1);
        assertThat(getEmittersForUser(1)).hasSize(2);
    }

    @Test
    void subscribe_DifferentUsers_ShouldCreateSeparateEmitterSets() {
        sseService.subscribe(1);
        sseService.subscribe(2);
        assertThat(getEmittersForUser(1)).hasSize(1);
        assertThat(getEmittersForUser(2)).hasSize(1);
    }

    // ==================== emit ====================

    @Test
    void emit_ShouldNotThrowForValidTask() {
        sseService.subscribe(1);
        FileTask task = new FileTask().setOperatorId(1).setStatus(TaskStatusEnum.RUNNING);
        sseService.emit(task);
        assertThat(getEmittersForUser(1)).hasSize(1);
    }

    @Test
    void emit_UserHasNoEmitters_ShouldNotThrowException() {
        FileTask task = new FileTask().setOperatorId(1).setStatus(TaskStatusEnum.RUNNING);
        sseService.emit(task);
        // 无异常即为通过
    }

    @Test
    void emit_TaskOperatorIdIsNull_ShouldNotThrowException() {
        sseService.subscribe(1);
        FileTask task = new FileTask().setOperatorId(null).setStatus(TaskStatusEnum.RUNNING);
        sseService.emit(task);
        // 无异常即为通过
    }

    @Test
    void emit_IOExceptionOnSend_ShouldRemoveEmitter() throws Exception {
        sseService.subscribe(1);
        SseEmitter emitter2 = sseService.subscribe(1);
        assertThat(getEmittersForUser(1)).hasSize(2);

        // 通过反射直接调用 remove 模拟 IOException 触发的清理逻辑
        invokeRemove(1, emitter2);

        assertThat(getEmittersForUser(1)).hasSize(1);
    }

    // ==================== cleanup ====================

    @Test
    void remove_ShouldClearEmitterFromMap() throws Exception {
        SseEmitter emitter = sseService.subscribe(1);
        assertThat(getEmittersForUser(1)).hasSize(1);

        invokeRemove(1, emitter);

        assertThat(getEmittersForUser(1)).isNull();
    }

    @Test
    void remove_LastEmitter_ShouldRemoveUserKey() throws Exception {
        SseEmitter emitter = sseService.subscribe(1);
        assertThat(getEmittersForUser(1)).hasSize(1);

        invokeRemove(1, emitter);

        // 用户键也被移除
        assertThat(getEmittersForUser(1)).isNull();
    }

    @Test
    void remove_OneOfMultipleEmitters_ShouldKeepOthers() throws Exception {
        SseEmitter emitter1 = sseService.subscribe(1);
        SseEmitter emitter2 = sseService.subscribe(1);
        assertThat(getEmittersForUser(1)).hasSize(2);

        invokeRemove(1, emitter1);

        assertThat(getEmittersForUser(1)).hasSize(1);
    }

    // ==================== helper methods ====================

    @SuppressWarnings("unchecked")
    private Set<SseEmitter> getEmittersForUser(Integer userId) {
        try {
            Field field = FileTaskSseService.class.getDeclaredField("emitters");
            field.setAccessible(true);
            ConcurrentHashMap<Integer, Set<SseEmitter>> emitters =
                    (ConcurrentHashMap<Integer, Set<SseEmitter>>) field.get(sseService);
            return emitters.get(userId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private void clearEmitters() {
        try {
            Field field = FileTaskSseService.class.getDeclaredField("emitters");
            field.setAccessible(true);
            ConcurrentHashMap<Integer, Set<SseEmitter>> emitters =
                    (ConcurrentHashMap<Integer, Set<SseEmitter>>) field.get(sseService);
            emitters.clear();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void invokeRemove(Integer userId, SseEmitter emitter) throws Exception {
        Method method = FileTaskSseService.class.getDeclaredMethod("remove", Integer.class, SseEmitter.class);
        method.setAccessible(true);
        method.invoke(sseService, userId, emitter);
    }
}
