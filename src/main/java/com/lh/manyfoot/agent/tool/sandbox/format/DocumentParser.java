package com.lh.manyfoot.agent.tool.sandbox.format;

import cn.hutool.core.util.StrUtil;
import org.apache.tika.exception.TikaException;
import org.apache.tika.exception.WriteLimitReachedException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.Property;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * 沙箱文档解析器
 *
 * <p>
 * 基于 Apache Tika 的文档解析器，支持 PDF、Word、Excel、PowerPoint、HTML、Markdown、CSV 等格式。
 * 提取纯文本和基础元数据（标题、作者、内容类型等），并支持按字符数截断。
 *
 * @author airx
 */
public final class DocumentParser {

    private static final int DEFAULT_MAX_CHARS = 12000;
    private static final int HARD_MAX_CHARS = 50000;

    private DocumentParser() {
    }

    public static int normalizeMaxChars(Integer maxChars) {
        if (maxChars == null || maxChars <= 0) {
            return DEFAULT_MAX_CHARS;
        }
        return Math.min(maxChars, HARD_MAX_CHARS);
    }

    public static String parse(String filePath, byte[] documentBytes, int maxChars)
        throws IOException, TikaException, SAXException {

        AutoDetectParser parser = new AutoDetectParser();
        Metadata metadata = new Metadata();
        metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, filePath);
        BodyContentHandler handler = new BodyContentHandler(maxChars);

        boolean truncated = false;
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(documentBytes)) {
            parser.parse(inputStream, handler, metadata, new ParseContext());
        } catch (SAXException e) {
            if (!WriteLimitReachedException.isWriteLimitReached(e)) {
                throw e;
            }
            truncated = true;
        }

        String text = handler.toString().trim();
        StringBuilder sb = new StringBuilder();
        sb.append("文档解析成功\n");
        sb.append("文件路径: ").append(filePath).append("\n");
        sb.append("文件大小: ").append(documentBytes.length).append(" bytes\n");

        appendMetadataIfPresent(sb, metadata, Metadata.CONTENT_TYPE, "内容类型");
        appendMetadataIfPresent(sb, metadata, TikaCoreProperties.TITLE, "标题");
        appendMetadataIfPresent(sb, metadata, TikaCoreProperties.CREATOR, "作者");

        if (truncated) {
            sb.append("提示: 文档正文已按 maxChars 截断，当前限制为 ").append(maxChars).append(" 字符\n");
        }

        sb.append("\n--- 文档正文 ---\n");
        if (StrUtil.isBlank(text)) {
            sb.append("[未提取到可读文本，可能是扫描件、图片型PDF或受保护文档]");
        } else {
            sb.append(text);
        }
        return sb.toString();
    }

    private static void appendMetadataIfPresent(StringBuilder sb, Metadata metadata, String name, String label) {
        String value = metadata.get(name);
        if (StrUtil.isNotBlank(value)) {
            sb.append(label).append(": ").append(value).append("\n");
        }
    }

    private static void appendMetadataIfPresent(StringBuilder sb, Metadata metadata, Property name, String label) {
        String value = metadata.get(name);
        if (StrUtil.isNotBlank(value)) {
            sb.append(label).append(": ").append(value).append("\n");
        }
    }
}
