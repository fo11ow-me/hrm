package com.qiujie.dto.assistant;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class AssistantChatRequest {

    private Long conversationId;

    private String message;

    private String scene = "employee_self_service";
}
