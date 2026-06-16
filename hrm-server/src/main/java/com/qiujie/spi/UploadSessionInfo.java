package com.qiujie.spi;

/**
 * 上传会话摘要信息，传递给 UploadCompletionHandler。
 */
public class UploadSessionInfo {

    private final String uploadId;
    private final String fileName;
    private final String fileExt;
    private final Long fileSize;
    private final String fileHash;
    private final Integer staffId;
    private final int chunkCount;

    public UploadSessionInfo(String uploadId, String fileName, String fileExt,
                              Long fileSize, String fileHash, Integer staffId, int chunkCount) {
        this.uploadId = uploadId;
        this.fileName = fileName;
        this.fileExt = fileExt;
        this.fileSize = fileSize;
        this.fileHash = fileHash;
        this.staffId = staffId;
        this.chunkCount = chunkCount;
    }

    public String getUploadId() { return uploadId; }
    public String getFileName() { return fileName; }
    public String getFileExt() { return fileExt; }
    public Long getFileSize() { return fileSize; }
    public String getFileHash() { return fileHash; }
    public Integer getStaffId() { return staffId; }
    public int getChunkCount() { return chunkCount; }
}
