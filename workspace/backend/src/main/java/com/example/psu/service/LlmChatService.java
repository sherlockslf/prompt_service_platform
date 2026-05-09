package com.example.psu.service;

import com.example.psu.exception.RequestValidationUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 大模型调用服务（DashScope/Qwen）。
 */
@Service
public class LlmChatService {

    private final SystemConfigService systemConfigService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${llm.provider:qwen}")
    private String provider;

    @Value("${llm.base-url:https://dashscope.aliyuncs.com/api/v1}")
    private String baseUrl;

    @Value("${llm.model:qwen-max}")
    private String model;

    @Value("${llm.timeout:30000}")
    private int timeoutMs;

    public LlmChatService(SystemConfigService systemConfigService, ObjectMapper objectMapper) {
        this.systemConfigService = systemConfigService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().build();
    }

    public String chatOnce(String prompt) {
        RequestValidationUtils.requireNonBlank(prompt, "prompt");
        String apiKey = systemConfigService.getAnyDashscopeApiKey();
        String endpoint = normalizeBaseUrl(baseUrl) + "/services/aigc/text-generation/generation";

        try {
            String body = objectMapper.writeValueAsString(
                java.util.Map.of(
                    "model", model,
                    "input", java.util.Map.of("prompt", prompt),
                    "parameters", java.util.Map.of("result_format", "message")
                )
            );

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofMillis(Math.max(timeoutMs, 1000)))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("大模型调用失败，HTTP " + response.statusCode() + ": " + response.body());
            }
            return extractModelText(response.body());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("大模型调用异常: " + ex.getMessage(), ex);
        } catch (IOException ex) {
            throw new RuntimeException("大模型调用异常: " + ex.getMessage(), ex);
        }
    }

    public String getProvider() {
        return provider;
    }

    public String getModel() {
        return model;
    }

    private String extractModelText(String json) throws IOException {
        JsonNode root = objectMapper.readTree(json);
        JsonNode output = root.path("output");
        JsonNode text = output.path("text");
        if (!text.isMissingNode() && !text.isNull()) {
            return text.asText();
        }
        JsonNode content = output.path("choices").path(0).path("message").path("content");
        if (!content.isMissingNode() && !content.isNull()) {
            if (content.isTextual()) {
                return content.asText();
            }
            if (content.isArray() && content.size() > 0) {
                JsonNode first = content.get(0);
                if (first.has("text")) {
                    return first.path("text").asText();
                }
            }
        }
        return json;
    }

    private String normalizeBaseUrl(String url) {
        String trimmed = (url == null ? "" : url.trim());
        if (trimmed.endsWith("/")) {
            return trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
