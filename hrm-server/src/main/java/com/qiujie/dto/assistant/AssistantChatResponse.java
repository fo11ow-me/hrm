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
}
