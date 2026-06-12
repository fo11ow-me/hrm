package com.qiujie.util;

import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

/**
 * zstd 无损压缩工具，用于文件存储层选择性压缩。
 *
 * @author qiujie
 */
public final class StorageCompressor {

    private StorageCompressor() {
    }

    /** zstd 压缩帧魔数，用于快速识别压缩数据 */
    static final byte[] ZSTD_MAGIC = {(byte) 0x28, (byte) 0xB5, 0x2F, (byte) 0xFD};

    /** 可压缩的文本类文件扩展名 */
    static final Set<String> COMPRESSIBLE_EXTENSIONS = Set.of(
            "txt", "csv", "json", "xml", "svg", "log"
    );

    /** 最小压缩阈值：小于此值的文件不压缩 */
    static final int MIN_COMPRESS_BYTES = 1024;

    /**
     * 判断文件扩展名是否属于可压缩类型。
     *
     * @param extName 小写扩展名（不含点）
     */
    public static boolean isCompressible(String extName) {
        return COMPRESSIBLE_EXTENSIONS.contains(extName);
    }

    /**
     * 通过魔数判断字节数组是否为 zstd 压缩数据。
     */
    public static boolean isZstdCompressed(byte[] data) {
        return data.length >= 4
                && data[0] == ZSTD_MAGIC[0]
                && data[1] == ZSTD_MAGIC[1]
                && data[2] == ZSTD_MAGIC[2]
                && data[3] == ZSTD_MAGIC[3];
    }

    /**
     * zstd 压缩，使用默认压缩级别。
     */
    public static byte[] compress(byte[] data) {
        return Zstd.compress(data);
    }

    /**
     * zstd 解压。
     *
     * @param data zstd 压缩帧数据
     */
    public static byte[] decompress(byte[] data) {
        long originalSize = Zstd.decompressedSize(data);
        if (originalSize > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("文件过大，无法解压到内存");
        }
        return Zstd.decompress(data, (int) originalSize);
    }

    /**
     * 尝试压缩。若不应压缩或压缩无效则返回原始数据。
     *
     * @param rawData 原始文件字节
     * @param extName 小写扩展名
     * @return CompressionResult 最终存储字节和压缩标记
     */
    public static CompressionResult tryCompress(byte[] rawData, String extName) {
        if (rawData.length < MIN_COMPRESS_BYTES || !isCompressible(extName)) {
            return new CompressionResult(rawData, false);
        }
        byte[] compressed = compress(rawData);
        if (compressed.length >= rawData.length) {
            return new CompressionResult(rawData, false);
        }
        return new CompressionResult(compressed, true);
    }

    /**
     * 若为 zstd 压缩数据则解压，否则原样返回。
     */
    public static byte[] decompressIfCompressed(byte[] stored) {
        return isZstdCompressed(stored) ? decompress(stored) : stored;
    }

    /**
     * 流式解压：包裹 InputStream，读取时自动解压。
     */
    public static InputStream decompressStream(InputStream in) {
        try { return new ZstdInputStream(in); }
        catch (IOException e) { throw new RuntimeException(e); }
    }

    public static InputStream decompressStreamIfCompressed(byte[] first4Bytes, InputStream in) {
        if (first4Bytes.length >= 4
                && first4Bytes[0] == ZSTD_MAGIC[0]
                && first4Bytes[1] == ZSTD_MAGIC[1]
                && first4Bytes[2] == ZSTD_MAGIC[2]
                && first4Bytes[3] == ZSTD_MAGIC[3]) {
            try { return new ZstdInputStream(in); }
            catch (IOException e) { throw new RuntimeException(e); }
        }
        return in;
    }

    /**
     * 读取流的前 4 字节以判断是否为 zstd 压缩数据。
     */
    public static byte[] peekMagicBytes(InputStream in) {
        try {
            byte[] header = new byte[4];
            int read = in.read(header);
            if (read < 4) {
                byte[] partial = new byte[read];
                System.arraycopy(header, 0, partial, 0, read);
                return partial;
            }
            return header;
        } catch (Exception e) {
            return new byte[0];
        }
    }

    /**
     * 压缩结果
     */
    public static class CompressionResult {
        public final byte[] bytes;
        public final boolean compressed;

        public CompressionResult(byte[] bytes, boolean compressed) {
            this.bytes = bytes;
            this.compressed = compressed;
        }
    }
}
