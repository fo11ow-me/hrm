package com.qiujie.dto.assistant;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
public class AssistantChatResponse {

    private Long conversationId;

    private String answer;

    private String intent;

    private List<String> suggestions = new ArrayList<>();

    private List<AssistantReference> references = new ArrayList<>();

    /** LLM 是否成功润色 */
    private boolean llmEnhanced;

    /** 可执行操作，null 表示无操作 */
    private AssistantAction action;
}
