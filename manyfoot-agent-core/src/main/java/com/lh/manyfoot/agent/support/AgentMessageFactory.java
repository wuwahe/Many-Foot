package com.lh.manyfoot.agent.support;

import com.lh.manyfoot.agent.context.AgentAttachment;
import com.lh.manyfoot.agent.context.AgentContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.InvalidMimeTypeException;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 将 AgentContext 中的附件转换为 Spring AI 消息。
 */
@Slf4j
public final class AgentMessageFactory {

    private AgentMessageFactory() {
    }

    /**
     * 构建用户消息：图片作为 Media 传入模型，普通文件只作为路径说明保留在文本中。
     */
    public static UserMessage buildUserMessage(String input, AgentContext context) {
        return buildUserMessage(input, context, context != null && context.isMultimodalInputEnabled());
    }

    /**
     * 构建用户消息：只有当前模型明确支持多模态时，才把图片作为 Media 传入模型。
     */
    public static UserMessage buildUserMessage(String input, AgentContext context, boolean multimodalInputEnabled) {
        String text = appendAttachmentInstructions(input, context);
        if (!multimodalInputEnabled) {
            logMediaSkipped(context);
            return UserMessage.builder().text(text).build();
        }
        List<Media> imageMedia = buildImageMedia(context);
        if (imageMedia.isEmpty()) {
            return UserMessage.builder().text(text).build();
        }
        return UserMessage.builder()
                .text(text)
                .media(imageMedia)
                .build();
    }

    private static void logMediaSkipped(AgentContext context) {
        if (context != null && context.hasImageAttachments()) {
            log.info("当前模型未声明支持多模态输入，图片附件仅以路径文本提供: sessionId={}, imageCount={}",
                    context.getSessionId(), context.getImageAttachments().size());
        }
    }

    private static List<Media> buildImageMedia(AgentContext context) {
        if (context == null || !context.hasImageAttachments()) {
            return List.of();
        }
        return context.getImageAttachments().stream()
                .filter(AgentMessageFactory::isReadableImage)
                .map(AgentMessageFactory::buildMedia)
                .flatMap(java.util.Optional::stream)
                .toList();
    }

    private static java.util.Optional<Media> buildMedia(AgentAttachment attachment) {
        try {
            MimeType mimeType = MimeTypeUtils.parseMimeType(attachment.getMimeType());
            return java.util.Optional.of(Media.builder()
                    .mimeType(mimeType)
                    .data(new FileSystemResource(attachment.getHostPath()))
                    .name(attachment.getFilename())
                    .build());
        } catch (InvalidMimeTypeException e) {
            log.warn("图片附件 MIME 类型非法，跳过多模态输入: sandboxPath={}, mimeType={}",
                    attachment.getSandboxPath(), attachment.getMimeType());
            return java.util.Optional.empty();
        }
    }

    private static boolean isReadableImage(AgentAttachment attachment) {
        if (attachment.getHostPath() == null || attachment.getMimeType() == null) {
            return false;
        }
        Path path = Path.of(attachment.getHostPath());
        boolean readable = Files.isRegularFile(path) && Files.isReadable(path);
        if (!readable) {
            log.warn("图片附件不可读，跳过多模态输入: sandboxPath={}, hostPath={}",
                    attachment.getSandboxPath(), attachment.getHostPath());
        }
        return readable;
    }

    private static String appendAttachmentInstructions(String input, AgentContext context) {
        if (context == null || context.getAttachments() == null || context.getAttachments().isEmpty()) {
            return input;
        }
        StringBuilder builder = new StringBuilder(input == null ? "" : input);
        builder.append("\n\n附件上下文：");
        if (!context.getImageAttachments().isEmpty()) {
            builder.append("\n图片附件已随本消息作为多模态输入提供，同时沙箱路径如下：");
            context.getImageAttachments().forEach(attachment -> builder
                    .append("\n- [image] ")
                    .append(attachment.getSandboxPath())
                    .append(" (")
                    .append(attachment.getMimeType())
                    .append(')'));
        }
        if (!context.getFileAttachments().isEmpty()) {
            builder.append("\n普通文件不能直接作为多模态输入，必要时请通过 MCP/沙箱工具读取这些路径：");
            context.getFileAttachments().forEach(attachment -> builder
                    .append("\n- [file] ")
                    .append(attachment.getSandboxPath())
                    .append(" (")
                    .append(attachment.getMimeType())
                    .append(')'));
        }
        return builder.toString();
    }
}
