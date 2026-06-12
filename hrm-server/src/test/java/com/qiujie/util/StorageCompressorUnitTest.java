package com.qiujie.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StorageCompressor 单元测试
 */
class StorageCompressorUnitTest {

    @Test
    @DisplayName("可压缩类型判定")
    void isCompressible() {
        assertTrue(StorageCompressor.isCompressible("txt"));
        assertTrue(StorageCompressor.isCompressible("csv"));
        assertTrue(StorageCompressor.isCompressible("json"));
        assertTrue(StorageCompressor.isCompressible("xml"));
        assertTrue(StorageCompressor.isCompressible("svg"));
        assertTrue(StorageCompressor.isCompressible("log"));
        assertFalse(StorageCompressor.isCompressible("pdf"));
        assertFalse(StorageCompressor.isCompressible("jpg"));
        assertFalse(StorageCompressor.isCompressible("docx"));
    }

    @Test
    @DisplayName("zstd 魔数检测")
    void isZstdCompressed() {
        // 已压缩的数据
        byte[] compressed = StorageCompressor.compress("hello world".getBytes());
        assertTrue(StorageCompressor.isZstdCompressed(compressed));

        // 普通文本
        assertFalse(StorageCompressor.isZstdCompressed("hello world".getBytes()));

        // 空数组不应抛异常
        assertFalse(StorageCompressor.isZstdCompressed(new byte[0]));
    }

    @Test
    @DisplayName("压缩与解压循环：字节级一致")
    void compressDecompressRoundTrip() {
        String original = "This is a test string for compression. ".repeat(200);
        byte[] originalBytes = original.getBytes();
        byte[] compressed = StorageCompressor.compress(originalBytes);
        assertTrue(compressed.length < originalBytes.length, "压缩后体积应小于原始");

        byte[] decompressed = StorageCompressor.decompress(compressed);
        assertArrayEquals(originalBytes, decompressed);
    }

    @Test
    @DisplayName("小于 1KB 的文件不压缩")
    void skipCompressSmallFile() {
        byte[] small = "abc".getBytes();
        StorageCompressor.CompressionResult result = StorageCompressor.tryCompress(small, "csv");
        assertFalse(result.compressed);
        assertArrayEquals(small, result.bytes);
    }

    @Test
    @DisplayName("非可压缩类型不压缩")
    void skipCompressUncompressibleType() {
        byte[] data = "x".repeat(2000).getBytes();
        StorageCompressor.CompressionResult result = StorageCompressor.tryCompress(data, "pdf");
        assertFalse(result.compressed);
        assertArrayEquals(data, result.bytes);
    }

    @Test
    @DisplayName("压缩后体积反增：使用原始数据")
    void fallbackWhenCompressionLarger() {
        // 随机数据通常无法有效压缩
        byte[] random = new byte[2048];
        new java.util.Random().nextBytes(random);
        StorageCompressor.CompressionResult result = StorageCompressor.tryCompress(random, "txt");
        if (result.compressed) {
            assertTrue(result.bytes.length < random.length);
        } else {
            assertArrayEquals(random, result.bytes);
        }
    }

    @Test
    @DisplayName("decompressIfCompressed: 已压缩数据自动解压")
    void decompressIfCompressedAuto() {
        byte[] original = "auto decompress test ".repeat(100).getBytes();
        byte[] compressed = StorageCompressor.compress(original);
        byte[] output = StorageCompressor.decompressIfCompressed(compressed);
        assertArrayEquals(original, output);
    }

    @Test
    @DisplayName("decompressIfCompressed: 未压缩数据原样返回")
    void decompressIfCompressedPlainData() {
        byte[] plain = "plain text".getBytes();
        byte[] output = StorageCompressor.decompressIfCompressed(plain);
        assertSame(plain, output, "未压缩数据应返回原引用");
    }
}
