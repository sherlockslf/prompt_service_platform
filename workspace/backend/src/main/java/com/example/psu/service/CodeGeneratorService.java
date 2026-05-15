package com.example.psu.service;

import com.example.psu.entity.JsonSchema;
import com.example.psu.entity.PromptCompositionRevision;
import com.example.psu.entity.VersionReview;
import com.example.psu.enums.ReviewStatus;
import com.example.psu.exception.BusinessException;
import com.example.psu.exception.ErrorCode;
import com.example.psu.repository.JsonSchemaRepository;
import com.example.psu.repository.PromptCompositionRevisionRepository;
import com.example.psu.repository.VersionReviewRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class CodeGeneratorService {
    private static final String LANGUAGE_JAVA = "java";
    private static final String LANGUAGE_PYTHON = "python";

    private final JsonSchemaRepository jsonSchemaRepository;
    private final VersionReviewRepository versionReviewRepository;
    private final PromptCompositionRevisionRepository promptCompositionRevisionRepository;
    private final ObjectMapper objectMapper;

    public CodeGeneratorService(
        JsonSchemaRepository jsonSchemaRepository,
        VersionReviewRepository versionReviewRepository,
        PromptCompositionRevisionRepository promptCompositionRevisionRepository,
        ObjectMapper objectMapper
    ) {
        this.jsonSchemaRepository = jsonSchemaRepository;
        this.versionReviewRepository = versionReviewRepository;
        this.promptCompositionRevisionRepository = promptCompositionRevisionRepository;
        this.objectMapper = objectMapper;
    }

    public String generateCompleteBusinessCode(Long psuId) {
        return generateCompleteBusinessCode(psuId, null, LANGUAGE_JAVA);
    }

    public String generateCompleteBusinessCode(Long psuId, String language) {
        return generateCompleteBusinessCode(psuId, null, language);
    }

    public String generateCompleteBusinessCode(Long psuId, Integer versionNo, String language) {
        Map<String, String> bundle = generateBusinessCodeBundle(psuId, versionNo, language);
        return renderBundleText(psuId, versionNo, bundle);
    }

    public Map<String, String> generateBusinessCodeBundle(Long psuId, Integer versionNo, String language) {
        String normalizedLanguage = normalizeLanguage(language);
        GenerationContext context = loadContext(psuId, versionNo);
        if (LANGUAGE_PYTHON.equals(normalizedLanguage)) {
            return Map.of("generated_psu_" + context.psuId + "_v" + context.versionNo + ".py", generatePythonBusinessCode(context));
        }
        return buildJavaBundleFiles(context);
    }

    public byte[] generateBusinessCodeBundleZip(Long psuId, Integer versionNo, String language) {
        Map<String, String> bundle = generateBusinessCodeBundle(psuId, versionNo, language);
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8);
            for (Map.Entry<String, String> entry : bundle.entrySet()) {
                zos.putNextEntry(new ZipEntry(entry.getKey()));
                byte[] content = entry.getValue().getBytes(StandardCharsets.UTF_8);
                zos.write(content);
                zos.closeEntry();
            }
            zos.finish();
            zos.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "生成下载包失败");
        }
    }

    private GenerationContext loadContext(Long psuId, Integer versionNo) {
        if (psuId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "psuId不能为空");
        }
        VersionReview review = resolveReview(psuId, versionNo);
        JsonSchema schema = resolveSchema(psuId, review);
        PromptCompositionRevision compositionRevision = promptCompositionRevisionRepository
            .findByCompositionIdAndRevisionNo(review.getCompositionId(), review.getCompositionRevisionNo())
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "编排快照不存在"));

        GenerationContext context = new GenerationContext();
        context.psuId = psuId;
        context.versionNo = review.getVersionNo();
        context.schema = schema;
        context.promptTemplate = compositionRevision.getContentSnapshot() == null ? "" : compositionRevision.getContentSnapshot();
        context.specJson = parseSpec(compositionRevision.getSpecJsonSnapshot());
        context.injectionPlan = readListMap(context.specJson.get("injectionPlan"));
        context.tokens = extractTemplateTokens(context.promptTemplate);
        return context;
    }

    private VersionReview resolveReview(Long psuId, Integer versionNo) {
        if (versionNo != null) {
            return versionReviewRepository.findByPsuIdAndVersionNo(psuId, versionNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "指定版本不存在: v" + versionNo));
        }
        return versionReviewRepository.findTopByPsuIdAndStatusOrderByReviewedAtDesc(psuId, ReviewStatus.FORMAL)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "未找到可生成代码的正式版本"));
    }

    private JsonSchema resolveSchema(Long psuId, VersionReview review) {
        if (review.getSchemaVersionNo() != null) {
            Optional<JsonSchema> byVersion = jsonSchemaRepository.findByPsuIdAndVersion(psuId, review.getSchemaVersionNo());
            if (byVersion.isPresent()) {
                return byVersion.get();
            }
        }
        return jsonSchemaRepository.findTopByPsuIdOrderByVersionDesc(psuId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Schema不存在"));
    }

    private Map<String, String> buildJavaBundleFiles(GenerationContext context) {
        String packageName = "com.example.generated.psu" + context.psuId + ".v" + context.versionNo;
        LinkedHashMap<String, String> files = new LinkedHashMap<>();
        files.put("src/main/java/" + packageName.replace('.', '/') + "/dto/PsuInputDto.java", generateInputDto(packageName, context.schema));
        files.put("src/main/java/" + packageName.replace('.', '/') + "/validation/PsuInputValidator.java", generateValidator(packageName, context.schema));
        files.put("src/main/java/" + packageName.replace('.', '/') + "/prompt/PromptRenderer.java", generateRenderer(packageName, context.promptTemplate, context.injectionPlan));
        files.put("src/main/java/" + packageName.replace('.', '/') + "/llm/AliyunChatClient.java", generateAliyunClient(packageName));
        files.put("src/main/java/" + packageName.replace('.', '/') + "/service/PsuPromptService.java", generateService(packageName));
        files.put("src/main/java/" + packageName.replace('.', '/') + "/api/PsuPromptApi.java", generateApi(packageName));
        files.put("README-GENERATED.md", generateReadme(context, packageName));

        return files;
    }

    private String renderBundleText(Long psuId, Integer versionNo, Map<String, String> files) {
        StringBuilder sb = new StringBuilder();
        sb.append("// Generated bundle for PSU ").append(psuId);
        if (versionNo != null) {
            sb.append(" version v").append(versionNo);
        }
        sb.append("\n");
        sb.append("// Java 21 template: copy these files into your Spring Boot + Cloud service.\n\n");
        for (Map.Entry<String, String> entry : files.entrySet()) {
            sb.append("===== FILE: ").append(entry.getKey()).append(" =====\n");
            sb.append(entry.getValue()).append("\n\n");
        }
        return sb.toString();
    }

    private String generateInputDto(String packageName, JsonSchema schema) {
        JsonNode root = parseSchemaNode(schema.getSchemaContent());
        JsonNode properties = root.path("properties");
        Set<String> required = readRequired(root);
        StringBuilder fields = new StringBuilder();
        if (properties.isObject()) {
            Iterator<String> names = properties.fieldNames();
            while (names.hasNext()) {
                String name = names.next();
                JsonNode node = properties.get(name);
                String javaType = mapType(node);
                fields.append("    private ").append(javaType).append(" ").append(toFieldName(name)).append(";\n");
                if (required.contains(name)) {
                    fields.append("    // required\n");
                }
            }
        }
        return """
            package %s.dto;

            import lombok.Data;
            import java.util.List;
            import java.util.Map;

            @Data
            public class PsuInputDto {
            %s}
            """.formatted(packageName, fields);
    }

    private String generateValidator(String packageName, JsonSchema schema) {
        JsonNode root = parseSchemaNode(schema.getSchemaContent());
        JsonNode properties = root.path("properties");
        Set<String> required = readRequired(root);
        StringBuilder checks = new StringBuilder();
        for (String field : required) {
            checks.append("        if (data.get(\"").append(field).append("\") == null) {\n");
            checks.append("            throw new IllegalArgumentException(\"missing required field: ").append(field).append("\");\n");
            checks.append("        }\n");
        }
        if (properties.isObject()) {
            Iterator<String> names = properties.fieldNames();
            while (names.hasNext()) {
                String name = names.next();
                JsonNode node = properties.get(name);
                String type = node.path("type").asText("");
                checks.append(typeCheckBlock(name, type));
            }
        }
        return """
            package %s.validation;

            import java.util.List;
            import java.util.Map;

            public final class PsuInputValidator {
                private PsuInputValidator() {}

                public static void validate(Map<String, Object> data) {
                    if (data == null) {
                        throw new IllegalArgumentException("input data must not be null");
                    }
            %s
                }
            }
            """.formatted(packageName, checks);
    }

    private String generateRenderer(String packageName, String template, List<Map<String, Object>> injectionPlan) {
        String escapedTemplate = toJavaUnicodeLiteral(template);
        String planJson = escapeJava(objectToJson(injectionPlan));
        return """
            package %s.prompt;

            import com.fasterxml.jackson.core.type.TypeReference;
            import com.fasterxml.jackson.databind.ObjectMapper;
            import java.util.LinkedHashMap;
            import java.util.List;
            import java.util.Map;
            import java.util.regex.Matcher;
            import java.util.regex.Pattern;

            public class PromptRenderer {
                private static final Pattern TOKEN_PATTERN = Pattern.compile("\\\\{\\\\{\\\\s*([^}]+?)\\\\s*\\\\}\\\\}");
                private static final String TEMPLATE = %s;
                private static final String INJECTION_PLAN_JSON = "%s";
                private static final ObjectMapper MAPPER = new ObjectMapper();

                public String render(Map<String, Object> input) {
                    Map<String, Object> valuesByToken = evaluateRules(input == null ? Map.of() : input);
                    Matcher matcher = TOKEN_PATTERN.matcher(TEMPLATE);
                    StringBuffer result = new StringBuffer();
                    while (matcher.find()) {
                        String token = matcher.group(1).trim();
                        Object value = valuesByToken.containsKey(token) ? valuesByToken.get(token) : readPath(input, token);
                        matcher.appendReplacement(result, Matcher.quoteReplacement(value == null ? "" : String.valueOf(value)));
                    }
                    matcher.appendTail(result);
                    return result.toString();
                }

                @SuppressWarnings("unchecked")
                private Map<String, Object> evaluateRules(Map<String, Object> input) {
                    try {
                        List<Map<String, Object>> rules = MAPPER.readValue(INJECTION_PLAN_JSON, new TypeReference<>() {});
                        Map<String, Object> result = new LinkedHashMap<>();
                        for (Map<String, Object> rule : rules) {
                            String token = str(rule.get("token"));
                            if (token == null || token.isBlank()) token = str(rule.get("path"));
                            if (token == null || token.isBlank()) continue;
                            if (!enabled(rule, input)) continue;
                            Object value = rule.containsKey("value") ? rule.get("value") : readPath(input, str(rule.get("sourcePath")) == null ? token : str(rule.get("sourcePath")));
                            if (value == null && rule.containsKey("defaultValue")) value = rule.get("defaultValue");
                            value = applyTransform(value, str(rule.get("transform")));
                            result.put(token, value);
                        }
                        return result;
                    } catch (Exception ignore) {
                        return Map.of();
                    }
                }

                private boolean enabled(Map<String, Object> rule, Map<String, Object> input) {
                    String whenPath = str(rule.get("whenPath"));
                    if (whenPath == null || whenPath.isBlank()) return true;
                    Object actual = readPath(input, whenPath);
                    if (rule.containsKey("whenEquals")) return java.util.Objects.equals(actual, rule.get("whenEquals"));
                    return actual != null;
                }

                private Object applyTransform(Object value, String op) {
                    if (value == null || op == null || op.isBlank()) return value;
                    return switch (op.toLowerCase()) {
                        case "upper" -> String.valueOf(value).toUpperCase();
                        case "lower" -> String.valueOf(value).toLowerCase();
                        case "trim" -> String.valueOf(value).trim();
                        case "json" -> toJson(value);
                        default -> value;
                    };
                }

                @SuppressWarnings("unchecked")
                private Object readPath(Map<String, Object> source, String path) {
                    if (source == null || path == null || path.isBlank()) return null;
                    Object current = source;
                    String normalized = path.replaceAll("\\\\[(\\\\d+)\\\\]", ".$1");
                    for (String part : normalized.split("\\\\.")) {
                        if (part.isBlank()) continue;
                        if (current instanceof Map<?, ?> map) {
                            current = map.get(part);
                        } else if (current instanceof List<?> list) {
                            int idx = Integer.parseInt(part);
                            if (idx < 0 || idx >= list.size()) return null;
                            current = list.get(idx);
                        } else {
                            return null;
                        }
                        if (current == null) return null;
                    }
                    return current;
                }

                private String toJson(Object value) {
                    try { return MAPPER.writeValueAsString(value); } catch (Exception e) { return String.valueOf(value); }
                }

                private String str(Object v) { return v == null ? null : String.valueOf(v); }
            }
            """.formatted(packageName, escapedTemplate, planJson);
    }

    private String generateAliyunClient(String packageName) {
        return """
            package %s.llm;

            import java.net.URI;
            import java.net.http.HttpClient;
            import java.net.http.HttpRequest;
            import java.net.http.HttpResponse;
            import java.nio.charset.StandardCharsets;

            public class AliyunChatClient {
                private final HttpClient httpClient = HttpClient.newHttpClient();
                private final String apiKey;
                private final String endpoint;
                private final String model;

                public AliyunChatClient(String apiKey, String endpoint, String model) {
                    this.apiKey = apiKey;
                    this.endpoint = endpoint;
                    this.model = model;
                }

                public String chat(String prompt) throws Exception {
                    String body = "{\\"model\\":\\"" + model + "\\",\\"input\\":{\\"messages\\":[{\\"role\\":\\"user\\",\\"content\\":\\"" + escape(prompt) + "\\"}]}}";
                    HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                        .build();
                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                    if (response.statusCode() >= 300) {
                        throw new IllegalStateException("Aliyun request failed: " + response.statusCode() + " / " + response.body());
                    }
                    return response.body();
                }

                private String escape(String text) {
                    return text == null ? "" : text.replace("\\\\", "\\\\\\\\").replace("\\"", "\\\\\\"").replace("\\n", "\\\\n");
                }
            }
            """.formatted(packageName);
    }

    private String generateService(String packageName) {
        return """
            package %s.service;

            import %s.llm.AliyunChatClient;
            import %s.prompt.PromptRenderer;
            import %s.validation.PsuInputValidator;
            import java.util.Map;

            public class PsuPromptService {
                private final PromptRenderer promptRenderer = new PromptRenderer();
                private final AliyunChatClient chatClient;

                public PsuPromptService(AliyunChatClient chatClient) {
                    this.chatClient = chatClient;
                }

                public String execute(Map<String, Object> input) throws Exception {
                    PsuInputValidator.validate(input);
                    String prompt = promptRenderer.render(input);
                    return chatClient.chat(prompt);
                }
            }
            """.formatted(packageName, packageName, packageName, packageName);
    }

    private String generateApi(String packageName) {
        return """
            package %s.api;

            import %s.llm.AliyunChatClient;
            import %s.service.PsuPromptService;
            import org.springframework.beans.factory.annotation.Value;
            import org.springframework.http.ResponseEntity;
            import org.springframework.web.bind.annotation.PostMapping;
            import org.springframework.web.bind.annotation.RequestBody;
            import org.springframework.web.bind.annotation.RequestMapping;
            import org.springframework.web.bind.annotation.RestController;
            import java.util.Map;

            @RestController
            @RequestMapping("/api/generated/psu")
            public class PsuPromptApi {
                private final PsuPromptService promptService;

                public PsuPromptApi(
                    @Value("${llm.api-key:}") String apiKey,
                    @Value("${llm.base-url:https://dashscope.aliyuncs.com/api/v1}") String baseUrl,
                    @Value("${llm.model:qwen-plus}") String model
                ) {
                    String endpoint = baseUrl.endsWith("/")
                        ? baseUrl + "services/aigc/text-generation/generation"
                        : baseUrl + "/services/aigc/text-generation/generation";
                    this.promptService = new PsuPromptService(new AliyunChatClient(apiKey, endpoint, model));
                }

                @PostMapping("/invoke")
                public ResponseEntity<String> invoke(@RequestBody Map<String, Object> input) throws Exception {
                    return ResponseEntity.ok(promptService.execute(input));
                }
            }
            """.formatted(packageName, packageName, packageName);
    }

    private String generateReadme(GenerationContext context, String packageName) {
        return """
            # PSU Generated Java21 Bundle

            - PSU ID: %d
            - Version: v%d
            - Package: `%s`

            ## Quick Integration (Spring Boot + Cloud)

            1. Copy all files under `src/main/java/...` into your target service.
            2. Ensure your service uses Java 21 and Spring Boot 3.x.
            3. Ensure dependencies include `spring-boot-starter-web`, `jackson-databind`, and `lombok`.
            4. Configure environment variables:
            - `ALIYUN_API_KEY`
            - `ALIYUN_CHAT_ENDPOINT` (optional)
            - `ALIYUN_CHAT_MODEL` (optional)
            5. Start service and call `POST /api/generated/psu/invoke`.

            ## Request Example

            ```json
            {
              "message": "hello"
            }
            ```

            ## Notes

            - DTO and validator are generated from schema snapshot.
            - Prompt rendering supports dynamic token replacement and injection plan rules.
            - If you use Spring Cloud Nacos or gateway, route this API the same way as other internal REST endpoints.
            """.formatted(context.psuId, context.versionNo, packageName);
    }

    private String generatePythonBusinessCode(GenerationContext context) {
        String schemaContent = escapeForPythonTripleQuote(context.schema.getSchemaContent());
        String promptTemplate = escapeForPythonTripleQuote(context.promptTemplate);
        return String.format(
            "# Generated for PSU: %d version v%d%n"
                + "import json%n"
                + "from typing import Dict, Any%n%n"
                + "SCHEMA_CONTENT = r'''%s'''%n"
                + "PROMPT_TEMPLATE = r'''%s'''%n%n"
                + "def validate_input(data: Dict[str, Any]) -> None:%n"
                + "    if not isinstance(data, dict):%n"
                + "        raise ValueError('input must be dict')%n%n"
                + "def render_prompt(data: Dict[str, Any]) -> str:%n"
                + "    return PROMPT_TEMPLATE%n%n"
                + "if __name__ == '__main__':%n"
                + "    print(render_prompt({'message':'hello'}))%n",
            context.psuId, context.versionNo, schemaContent, promptTemplate
        );
    }

    private String typeCheckBlock(String name, String schemaType) {
        String expected = switch (schemaType) {
            case "integer", "number" -> "Number";
            case "boolean" -> "Boolean";
            case "array" -> "List";
            case "object" -> "Map";
            default -> "String";
        };
        return """
                    if (data.get("%s") != null && !(data.get("%s") instanceof %s)) {
                        throw new IllegalArgumentException("field %s type invalid, expected %s");
                    }
            """.formatted(name, name, expected, name, expected);
    }

    private String mapType(JsonNode node) {
        String type = node.path("type").asText("");
        return switch (type) {
            case "integer" -> "Integer";
            case "number" -> "Double";
            case "boolean" -> "Boolean";
            case "array" -> "List<Object>";
            case "object" -> "Map<String, Object>";
            default -> "String";
        };
    }

    private Set<String> readRequired(JsonNode root) {
        Set<String> result = new LinkedHashSet<>();
        JsonNode required = root.path("required");
        if (!required.isArray()) {
            return result;
        }
        for (JsonNode item : required) {
            result.add(item.asText());
        }
        return result;
    }

    private JsonNode parseSchemaNode(String schemaContent) {
        try {
            return objectMapper.readTree(schemaContent == null ? "{}" : schemaContent);
        } catch (Exception e) {
            return objectMapper.createObjectNode();
        }
    }

    private Map<String, Object> parseSpec(String specJson) {
        if (specJson == null || specJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(specJson, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private List<Map<String, Object>> readListMap(Object node) {
        if (!(node instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> normalized = new LinkedHashMap<>();
                map.forEach((k, v) -> normalized.put(String.valueOf(k), v));
                result.add(normalized);
            }
        }
        return result;
    }

    private List<String> extractTemplateTokens(String template) {
        if (template == null || template.isBlank()) {
            return List.of();
        }
        Set<String> tokens = new LinkedHashSet<>();
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\{\\{\\s*([^}]+?)\\s*\\}\\}").matcher(template);
        while (matcher.find()) {
            String path = matcher.group(1).trim();
            if (!path.isBlank()) {
                tokens.add(path);
            }
        }
        return new ArrayList<>(tokens);
    }

    private String toJavaUnicodeLiteral(String text) {
        if (text == null) {
            return "\"\"";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\"");
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\\\\\");
                case '"' -> sb.append("\\\\\"");
                case '\n' -> sb.append("\\\\n");
                case '\r' -> {
                }
                case '\t' -> sb.append("\\\\t");
                default -> {
                    if (c < 0x20 || c > 0x7E) {
                        sb.append(String.format("\\\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append("\"");
        return sb.toString();
    }

    private String objectToJson(Object value) {
        try {
            return objectMapper.writeValueAsString(Objects.requireNonNullElse(value, List.of()));
        } catch (Exception e) {
            return "[]";
        }
    }

    private String escapeJava(String text) {
        return text == null ? "" : text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String toFieldName(String raw) {
        if (raw == null || raw.isBlank()) {
            return "field";
        }
        String normalized = raw.replaceAll("[^A-Za-z0-9_]", "_");
        if (Character.isDigit(normalized.charAt(0))) {
            normalized = "_" + normalized;
        }
        return normalized;
    }

    private String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return LANGUAGE_JAVA;
        }
        String normalized = language.trim().toLowerCase(Locale.ROOT);
        if (LANGUAGE_PYTHON.equals(normalized)) {
            return LANGUAGE_PYTHON;
        }
        return LANGUAGE_JAVA;
    }

    private String escapeForPythonTripleQuote(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("'''", "\\'\\'\\'");
    }

    private static class GenerationContext {
        private Long psuId;
        private Integer versionNo;
        private JsonSchema schema;
        private String promptTemplate;
        private Map<String, Object> specJson;
        private List<Map<String, Object>> injectionPlan;
        private List<String> tokens;
    }
}
