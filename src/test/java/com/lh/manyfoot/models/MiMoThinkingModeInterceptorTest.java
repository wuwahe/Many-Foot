package com.lh.manyfoot.models;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestExecution;

import java.io.IOException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link MiMoThinkingModeInterceptor} 单元测试。
 */
class MiMoThinkingModeInterceptorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MiMoThinkingModeInterceptor interceptor = new MiMoThinkingModeInterceptor();

    @Test
    void intercept_mimoRequest_shouldInjectThinkingDisableParams() throws IOException {
        // Given: 原始请求 JSON
        String originalJson = "{\"model\":\"mimo-v2.5\",\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}]}";
        byte[] originalBody = originalJson.getBytes();

        org.springframework.http.HttpRequest request = mock(org.springframework.http.HttpRequest.class);
        when(request.getURI()).thenReturn(URI.create("https://token-plan-cn.xiaomimimo.com/v1/chat/completions"));

        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(null);

        // When
        interceptor.intercept(request, originalBody, execution);

        // Then: 验证执行时传入的 body 已被修改
        org.mockito.ArgumentCaptor<byte[]> bodyCaptor = org.mockito.ArgumentCaptor.forClass(byte[].class);
        verify(execution).execute(any(), bodyCaptor.capture());

        JsonNode modifiedBody = objectMapper.readTree(bodyCaptor.getValue());

        // 验证 thinking: {type: "disabled"}
        assertTrue(modifiedBody.has("thinking"));
        assertEquals("disabled", modifiedBody.get("thinking").get("type").asText());

        // 验证 enable_thinking: false
        assertFalse(modifiedBody.get("enable_thinking").asBoolean());

        // 验证 chat_template_kwargs
        assertTrue(modifiedBody.has("chat_template_kwargs"));
        JsonNode chatTemplateKwargs = modifiedBody.get("chat_template_kwargs");
        assertFalse(chatTemplateKwargs.get("enable_thinking").asBoolean());
        assertFalse(chatTemplateKwargs.get("thinking").asBoolean());

        // 验证原有字段保留
        assertEquals("mimo-v2.5", modifiedBody.get("model").asText());
        assertTrue(modifiedBody.get("messages").isArray());
    }

    @Test
    void intercept_nonMimoRequest_shouldNotModifyBody() throws IOException {
        // Given
        String originalJson = "{\"model\":\"gpt-4\",\"messages\":[]}";
        byte[] originalBody = originalJson.getBytes();

        org.springframework.http.HttpRequest request = mock(org.springframework.http.HttpRequest.class);
        when(request.getURI()).thenReturn(URI.create("https://api.openai.com/v1/chat/completions"));

        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(null);

        // When
        interceptor.intercept(request, originalBody, execution);

        // Then: 验证执行时传入的 body 仍是原始的
        org.mockito.ArgumentCaptor<byte[]> bodyCaptor = org.mockito.ArgumentCaptor.forClass(byte[].class);
        verify(execution).execute(any(), bodyCaptor.capture());

        assertEquals(originalJson, new String(bodyCaptor.getValue()));
    }

    @Test
    void intercept_similarButNonMimoHost_shouldNotModifyBody() throws IOException {
        // Given: 相似域名不能被误判为 MiMo，避免向非目标服务注入私有参数。
        String originalJson = "{\"model\":\"gpt-4\",\"messages\":[]}";
        byte[] originalBody = originalJson.getBytes();

        org.springframework.http.HttpRequest request = mock(org.springframework.http.HttpRequest.class);
        when(request.getURI()).thenReturn(URI.create("https://evil-xiaomimimo.com/v1/chat/completions"));

        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(null);

        // When
        interceptor.intercept(request, originalBody, execution);

        // Then
        org.mockito.ArgumentCaptor<byte[]> bodyCaptor = org.mockito.ArgumentCaptor.forClass(byte[].class);
        verify(execution).execute(any(), bodyCaptor.capture());

        assertEquals(originalJson, new String(bodyCaptor.getValue()));
    }

    @Test
    void intercept_invalidJson_shouldPassThroughWithoutException() throws IOException {
        // Given: 非法 JSON
        byte[] invalidBody = "not json".getBytes();

        org.springframework.http.HttpRequest request = mock(org.springframework.http.HttpRequest.class);
        when(request.getURI()).thenReturn(URI.create("https://token-plan-cn.xiaomimimo.com/v1/chat/completions"));

        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(null);

        // When: 不应抛出异常
        interceptor.intercept(request, invalidBody, execution);

        // Then: 验证执行时传入的仍是原始 body
        org.mockito.ArgumentCaptor<byte[]> bodyCaptor = org.mockito.ArgumentCaptor.forClass(byte[].class);
        verify(execution).execute(any(), bodyCaptor.capture());

        assertEquals("not json", new String(bodyCaptor.getValue()));
    }
}
