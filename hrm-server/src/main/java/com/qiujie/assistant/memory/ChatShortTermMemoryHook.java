package com.qiujie.assistant.memory;

/**
 * 预留：BEFORE_MODEL Hook 上下文组装。
 * <p>
 * 当前 Spring AI Alibaba 1.0.0.2 未提供 MessagesModelHook API。
 * L3 运行时截断已在 ChatService.chat() 中内联实现——
 * loadRecentMessages() + token 估算 + subList 硬截断。
 * 等 Hook API 稳定后接入。
 *
 * @author qiujie
 * @since 2026/06/30
 */
// @Component
// @HookPositions({HookPosition.BEFORE_MODEL})
// public class ChatShortTermMemoryHook extends MessagesModelHook { ... }
public class ChatShortTermMemoryHook {
}
