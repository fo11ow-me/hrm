package com.qiujie.controller;

import com.qiujie.annotation.RateLimit;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.dto.assistant.AssistantChatRequest;
import com.qiujie.service.AssistantService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/assistant")
public class AssistantController {

    @Autowired
    private AssistantService assistantService;

    @ApiOperation("智能问答")
    @PostMapping("/chat")
    @RateLimit(value = 10, timeout = 60, type = RateLimit.LimitType.USER)
    public ResponseDTO chat(@RequestBody AssistantChatRequest request) {
        return assistantService.chat(request);
    }

    @ApiOperation("查询当前用户会话")
    @GetMapping("/conversations")
    public ResponseDTO listConversations() {
        return assistantService.listConversations();
    }

    @ApiOperation("查询会话消息")
    @GetMapping("/conversations/{id}")
    public ResponseDTO queryConversation(@PathVariable Long id) {
        return assistantService.queryConversation(id);
    }

    @ApiOperation("删除会话")
    @DeleteMapping("/conversations/{id}")
    public ResponseDTO deleteConversation(@PathVariable Long id) {
        return assistantService.deleteConversation(id);
    }
}
