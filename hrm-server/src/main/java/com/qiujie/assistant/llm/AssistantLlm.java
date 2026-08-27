package com.qiujie.assistant.llm;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.lang.Nullable;

import java.util.List;

/**
 * AI 助手 LLM 调用边界——领域侧端口（Port）。
 * <p>
 * 隐藏底层 {@code ChatClient} 的构建、Prompt 组装与失败降级语义，
 * 让 {@code assistant} 模块的调用方只关心两个语义入口：
 * </p>
 * <ul>
 *   <li>{@link #chat}——主对话（可带 system 记忆上下文与工具），失败返回 {@code null}</li>
 *   <li>{@link #summarize}——记忆摘要（模板 Prompt），失败返回空串</li>
 * </ul>
 * <p>
 * 两个方法的降级约定刻意不同（null vs 空串），以区分「对话失败需用户兜底提示」
 * 与「摘要失败静默降级、不更新记忆」两种语义。生产实现见 {@link SpringAiAssistantLlm}。
 * </p>
 *
 * @author qiujie
 */
public interface AssistantLlm {

    /**
     * 主对话调用。
     *
     * @param historyMessages 历史消息（可为空）
     * @param userMessage     当前用户消息
     * @param systemContext   记忆 system 上下文（可为空）
     * @param tools           工具对象（可为空，如 {@code ChatTools}）
     * @return LLM 回答文本；调用失败或返回空白时返回 {@code null}
     */
    @Nullable
    String chat(List<Message> historyMessages, String userMessage,
                @Nullable String systemContext, Object... tools);

    /**
     * 记忆摘要调用（L1/L2）。
     *
     * @param prompt 已由 PromptTemplate 渲染的 Prompt
     * @return 摘要文本；调用失败或返回空白时返回空串（降级语义：不更新记忆）
     */
    String summarize(Prompt prompt);
}
