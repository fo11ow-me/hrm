package com.qiujie.knowledge.lifecycle;

import com.qiujie.knowledge.entity.KnowledgeDocument;
import com.qiujie.knowledge.enums.DocumentStatusEnum;
import com.qiujie.knowledge.mapper.IngestionJobMapper;
import com.qiujie.knowledge.mapper.KnowledgeDocumentMapper;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.Executor;

/**
 * 文档生命周期门面——生命周期唯一的公共 API。
 * <p>
 * 命令方法只做两件事：事务内写状态 + 注册「事务提交后」的异步任务；
 * ETL、清理、状态机补偿全部在模块内部，调用方不感知异步、不接触线程池。
 */
public final class DocumentLifecycleService {

    /** 上传完成登记。name = mergedKey = MinIO 对象键（物理文件引用键）。 */
    public record RegisterCommand(String name, String oldName, String type,
                                  String fileHash, Long fileSize, Integer staffId) {}

    /** 登记成功：文档 UPLOADED，ETL 将在事务提交后异步执行。 */
    public record RegisterResult(Long documentId, String status) {}

    /** 重试命令。 */
    public record RetryCommand(Long documentId) {}

    /** 重试裁决：accepted=false 时 reason 说明拒绝原因（预期场景用结果表达，不抛异常）。 */
    public record RetryResult(Long documentId, boolean accepted, String reason) {}

    /** 删除命令。 */
    public record DeleteCommand(Long documentId) {}

    /** 删除裁决：幂等——不存在/已删除 → alreadyDeleted=true 仍算成功。 */
    public record DeleteResult(Long documentId, boolean alreadyDeleted) {}

    private final KnowledgeDocumentMapper documentMapper;
    private final IngestionJobMapper jobMapper;
    private final TransactionTemplate transactionTemplate;
    private final Executor ingestExecutor;
    private final IngestionPipeline pipeline;
    private final DocumentPurgeHandler purgeHandler;

    public DocumentLifecycleService(KnowledgeDocumentMapper documentMapper,
                                    IngestionJobMapper jobMapper,
                                    TransactionTemplate transactionTemplate,
                                    Executor ingestExecutor,
                                    IngestionPipeline pipeline,
                                    DocumentPurgeHandler purgeHandler) {
        this.documentMapper = documentMapper;
        this.jobMapper = jobMapper;
        this.transactionTemplate = transactionTemplate;
        this.ingestExecutor = ingestExecutor;
        this.pipeline = pipeline;
        this.purgeHandler = purgeHandler;
    }

    /**
     * 上传完成登记：事务内 INSERT kb_document(status=UPLOADED)，提交后异步 ETL。
     * 事务内调用（分片上传完成回调）加入外层事务、事务外调用（补发摄入）自开事务。
     */
    public RegisterResult register(RegisterCommand cmd) {
        return transactionTemplate.execute(status -> {
            KnowledgeDocument doc = new KnowledgeDocument()
                    .setName(cmd.name())
                    .setOldName(cmd.oldName())
                    .setType(cmd.type())
                    .setFileHash(cmd.fileHash())
                    .setFileSize(cmd.fileSize())
                    .setStatus(DocumentStatusEnum.UPLOADED.name())
                    .setStaffId(cmd.staffId())
                    .setUploadTime(LocalDateTime.now());
            documentMapper.insert(doc);
            Long documentId = doc.getId();
            scheduleAfterCommit(() -> pipeline.run(documentId));
            return new RegisterResult(documentId, DocumentStatusEnum.UPLOADED.name());
        });
    }

    /**
     * 重试摄入：READY/已删除/不存在拒绝（accepted=false + reason）；
     * UPLOADED/FAILED/PROCESSING 接受，提交后异步 ETL（并发互斥由管道内 CAS 认领兜底）。
     */
    public RetryResult retry(RetryCommand cmd) {
        return transactionTemplate.execute(status -> {
            KnowledgeDocument doc = documentMapper.selectById(cmd.documentId());
            if (doc == null) {
                return new RetryResult(cmd.documentId(), false, "文档不存在");
            }
            if (DocumentStatusEnum.READY.name().equals(doc.getStatus())) {
                return new RetryResult(cmd.documentId(), false, "已处理完成的文档无需重试");
            }
            if (Integer.valueOf(1).equals(doc.getIsDeleted())) {
                return new RetryResult(cmd.documentId(), false, "文档已删除");
            }
            Long documentId = doc.getId();
            scheduleAfterCommit(() -> pipeline.run(documentId));
            return new RetryResult(documentId, true, null);
        });
    }

    /**
     * 删除文档（幂等）：事务内 CAS 逻辑删 + 作废在途作业，提交后异步物理清理
     * （引用计数 → MinIO → PG 切片/向量/镜像）。
     */
    public DeleteResult delete(DeleteCommand cmd) {
        return transactionTemplate.execute(status -> {
            KnowledgeDocument doc = documentMapper.selectById(cmd.documentId());
            if (doc == null || documentMapper.markDeleted(cmd.documentId()) == 0) {
                // 不存在或已删除：幂等成功
                return new DeleteResult(cmd.documentId(), true);
            }
            // 作废在途作业，防 worker 在删除后继续写 PG
            jobMapper.cancelActiveJobs(cmd.documentId());
            String storageKey = doc.getName();
            scheduleAfterCommit(() -> purgeHandler.purge(cmd.documentId(), storageKey));
            return new DeleteResult(cmd.documentId(), false);
        });
    }

    /**
     * 事务提交后触发 ETL；无活动事务时立即触发（独立调用与单元测试场景）。
     * 嵌套事务场景下同步注册在最外层事务提交后才执行——修复"new Thread 在外层
     * 事务提交前启动、worker 读不到未提交文档"的永久卡死 bug。
     */
    private void scheduleAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    ingestExecutor.execute(task);
                }
            });
        } else {
            ingestExecutor.execute(task);
        }
    }
}
