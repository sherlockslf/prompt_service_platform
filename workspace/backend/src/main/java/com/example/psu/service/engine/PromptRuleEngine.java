package com.example.psu.service.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PromptRuleEngine {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\{\\{\\s*([^}]+?)\\s*\\}\\}");
    private final ObjectMapper objectMapper;

    public PromptRuleEngine(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public RenderResult render(String template, Map<String, Object> input, List<Map<String, Object>> injectionPlan) {
        String safeTemplate = template == null ? "" : template;
        Map<String, Object> safeInput = input == null ? Map.of() : input;
        List<Map<String, Object>> safePlan = injectionPlan == null ? List.of() : injectionPlan;

        Map<String, Object> resolvedByToken = new LinkedHashMap<>();
        Set<String> requiredMissing = new LinkedHashSet<>();
        for (Map<String, Object> rule : safePlan) {
            if (rule == null) {
                continue;
            }
            if (!isRuleEnabled(rule, safeInput)) {
                continue;
            }
            String token = readString(rule.get("token"));
            if (token == null || token.isBlank()) {
                token = readString(rule.get("path"));
            }
            if (token == null || token.isBlank()) {
                continue;
            }
            Object value = resolveRuleValue(rule, safeInput);
            if (value == null && readBoolean(rule.get("required"), false)) {
                requiredMissing.add(token);
            }
            resolvedByToken.put(token, value);
        }

        Set<String> used = new LinkedHashSet<>();
        Set<String> missing = new LinkedHashSet<>(requiredMissing);
        Matcher matcher = TOKEN_PATTERN.matcher(safeTemplate);
        StringBuffer rendered = new StringBuffer();
        while (matcher.find()) {
            String token = matcher.group(1).trim();
            used.add(token);
            Object value = resolvedByToken.containsKey(token) ? resolvedByToken.get(token) : getValueByPath(safeInput, token);
            if (value == null) {
                missing.add(token);
                matcher.appendReplacement(rendered, "");
            } else if (value instanceof Map || value instanceof List) {
                matcher.appendReplacement(rendered, Matcher.quoteReplacement(writeValueAsString(value)));
            } else {
                matcher.appendReplacement(rendered, Matcher.quoteReplacement(String.valueOf(value)));
            }
        }
        matcher.appendTail(rendered);

        RenderResult result = new RenderResult();
        result.setRenderedPrompt(rendered.toString());
        result.setMissingVars(new ArrayList<>(missing));
        result.setUsedVars(new ArrayList<>(used));
        result.setParamSnapshot(resolvedByToken);
        return result;
    }

    private boolean isRuleEnabled(Map<String, Object> rule, Map<String, Object> input) {
        String whenPath = readString(rule.get("whenPath"));
        if (whenPath == null || whenPath.isBlank()) {
            return true;
        }
        Object actual = getValueByPath(input, whenPath);
        if (rule.containsKey("whenEquals")) {
            return Objects.equals(actual, rule.get("whenEquals"));
        }
        return actual != null;
    }

    private Object resolveRuleValue(Map<String, Object> rule, Map<String, Object> input) {
        Object value;
        if (rule.containsKey("value")) {
            value = rule.get("value");
        } else {
            String sourcePath = readString(rule.get("sourcePath"));
            if (sourcePath == null || sourcePath.isBlank()) {
                sourcePath = readString(rule.get("path"));
            }
            value = getValueByPath(input, sourcePath);
        }
        if (value == null && rule.containsKey("defaultValue")) {
            value = rule.get("defaultValue");
        }
        return applyTransform(value, readString(rule.get("transform")));
    }

    private Object applyTransform(Object value, String transform) {
        if (value == null || transform == null || transform.isBlank()) {
            return value;
        }
        String op = transform.trim().toLowerCase();
        if ("upper".equals(op)) {
            return String.valueOf(value).toUpperCase();
        }
        if ("lower".equals(op)) {
            return String.valueOf(value).toLowerCase();
        }
        if ("trim".equals(op)) {
            return String.valueOf(value).trim();
        }
        if ("json".equals(op)) {
            return writeValueAsString(value);
        }
        return value;
    }

    private String readString(Object value) {
        if (value == null) {
            return null;
        }
        String str = String.valueOf(value);
        return str.isBlank() ? null : str;
    }

    private boolean readBoolean(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private String writeValueAsString(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private Object getValueByPath(Map<String, Object> source, String rawPath) {
        if (source == null || rawPath == null || rawPath.isBlank()) {
            return null;
        }
        String normalized = rawPath.replaceAll("\\[(\\d+)\\]", ".$1");
        String[] parts = normalized.split("\\.");
        Object current = source;
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (current instanceof Map<?, ?> map) {
                current = map.get(part);
            } else if (current instanceof List<?> list) {
                int idx;
                try {
                    idx = Integer.parseInt(part);
                } catch (NumberFormatException e) {
                    return null;
                }
                if (idx < 0 || idx >= list.size()) {
                    return null;
                }
                current = list.get(idx);
            } else {
                return null;
            }
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    public static class RenderResult {
        private String renderedPrompt;
        private List<String> missingVars;
        private List<String> usedVars;
        private Map<String, Object> paramSnapshot;

        public String getRenderedPrompt() {
            return renderedPrompt;
        }

        public void setRenderedPrompt(String renderedPrompt) {
            this.renderedPrompt = renderedPrompt;
        }

        public List<String> getMissingVars() {
            return missingVars;
        }

        public void setMissingVars(List<String> missingVars) {
            this.missingVars = missingVars;
        }

        public List<String> getUsedVars() {
            return usedVars;
        }

        public void setUsedVars(List<String> usedVars) {
            this.usedVars = usedVars;
        }

        public Map<String, Object> getParamSnapshot() {
            return paramSnapshot;
        }

        public void setParamSnapshot(Map<String, Object> paramSnapshot) {
            this.paramSnapshot = paramSnapshot;
        }
    }
}
