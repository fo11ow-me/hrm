package com.qiujie.knowledge.controller;

import com.qiujie.dto.ResponseDTO;
import com.qiujie.knowledge.service.KnowledgeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 知识库文档管理控制器。
 */
@RestController
@RequestMapping("/knowledge")
public class KnowledgeController {

    @Autowired
    private KnowledgeService knowledgeService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('system:docs:list')")
    public ResponseDTO list(@RequestParam(defaultValue = "1") Integer current,
                            @RequestParam(defaultValue = "10") Integer size,
                            @RequestParam(required = false) String oldName) {
        return knowledgeService.list(current, size, oldName);
    }

    @GetMapping("/{id}")
    public ResponseDTO query(@PathVariable Long id) {
        return knowledgeService.query(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('system:docs:delete')")
    public ResponseDTO delete(@PathVariable Long id) {
        return knowledgeService.delete(id);
    }

    @PostMapping("/{id}/retry")
    @PreAuthorize("hasAnyAuthority('system:docs:upload')")
    public ResponseDTO retry(@PathVariable Long id) {
        return knowledgeService.retry(id);
    }

    @GetMapping("/{id}/chunks")
    public ResponseDTO chunks(@PathVariable Long id) {
        return knowledgeService.chunks(id);
    }
}
