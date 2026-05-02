package com.example.psu.service;

import com.example.psu.entity.JsonSchema;
import com.example.psu.entity.PromptFragment;
import com.example.psu.repository.JsonSchemaRepository;
import com.example.psu.repository.PromptFragmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * 代码生成服务
 */
@Service
public class CodeGeneratorService {
    private static final String LANGUAGE_JAVA = "java";
    private static final String LANGUAGE_PYTHON = "python";
    
    @Autowired
    private JsonSchemaRepository jsonSchemaRepository;
    
    @Autowired
    private PromptFragmentRepository promptFragmentRepository;
    
    /**
     * 生成参数校验代码
     * @param schema Schema实体
     * @return 生成的校验代码
     */
    public String generateParameterValidationCode(JsonSchema schema) {
        // 根据JSON Schema生成参数校验代码
        return """
            // Generated parameter validation code based on JSON Schema
            public class ParameterValidator {
                
                public void validateInput(Object input) {
                    // Validation logic based on schema: """ + schema.getId() + """
                    // Schema content: """ + schema.getSchemaContent() + """
                }
            }
            """;
    }
    
    /**
     * 生成参数拼装代码
     * @param inputSchema 输入Schema
     * @param outputSchema 输出Schema
     * @return 生成的拼装代码
     */
    public String generateParameterAssemblyCode(JsonSchema inputSchema, JsonSchema outputSchema) {
        return """
            // Generated parameter assembly code
            public class ParameterAssembler {
                
                public Object assembleOutput(Object response) {
                    // Assembly logic based on input schema: """ + inputSchema.getId() + """
                    // And output schema: """ + outputSchema.getId() + """
                    return response;
                }
            }
            """;
    }
    
    /**
     * 生成Prompt组装代码
     * @param psuId PSU ID
     * @return 生成的Prompt组装代码
     */
    public String generatePromptAssemblyCode(Long psuId) {
        List<PromptFragment> fragments = promptFragmentRepository.findByPsuIdOrderBySortOrderAsc(psuId);
        
        StringBuilder code = new StringBuilder();
        code.append("// Generated prompt assembly code\n");
        code.append("public class PromptAssembler {\n");
        code.append("    \n");
        code.append("    public String assemblePrompt(Object input) {\n");
        code.append("        StringBuilder sb = new StringBuilder();\n");
        
        for (PromptFragment fragment : fragments) {
            code.append("        // Fragment: ").append(fragment.getFragmentKey()).append("\n");
            code.append("        sb.append(\"").append(fragment.getContent().replace("\"", "\\\"")).append("\\n\");\n");
        }
        
        code.append("        return sb.toString().trim();\n");
        code.append("    }\n");
        code.append("}\n");
        
        return code.toString();
    }
    
    /**
     * 生成完整业务代码
     * @param psuId PSU ID
     * @return 生成的完整业务代码
     */
    public String generateCompleteBusinessCode(Long psuId) {
        // 默认兼容历史调用，未指定语言时按Java产物生成。
        return generateCompleteBusinessCode(psuId, LANGUAGE_JAVA);
    }

    /**
     * 按语言生成完整业务代码
     * @param psuId PSU ID
     * @param language 语言（java/python）
     * @return 生成的完整业务代码
     */
    public String generateCompleteBusinessCode(Long psuId, String language) {
        // 获取最新的Schema
        JsonSchema latestSchema = jsonSchemaRepository.findTopByPsuIdOrderByVersionDesc(psuId)
                .orElseThrow(() -> new RuntimeException("Schema not found for PSU: " + psuId));

        String normalizedLanguage = normalizeLanguage(language);
        // 按语言分流代码模板，统一走同一入口避免前端分支复杂化。
        if (LANGUAGE_PYTHON.equals(normalizedLanguage)) {
            return generatePythonBusinessCode(psuId, latestSchema);
        }
        return generateJavaBusinessCode(psuId, latestSchema);
    }

    private String generateJavaBusinessCode(Long psuId, JsonSchema latestSchema) {
        String validationCode = generateParameterValidationCode(latestSchema);
        String assemblyCode = generateParameterAssemblyCode(latestSchema, latestSchema);
        String promptCode = generatePromptAssemblyCode(psuId);
        return """
            // Complete business code for PSU: """ + psuId + """

            """ + validationCode + """

            """ + assemblyCode + """

            """ + promptCode + """
            """;
    }

    private String generatePythonBusinessCode(Long psuId, JsonSchema latestSchema) {
        List<PromptFragment> fragments = promptFragmentRepository.findByPsuIdOrderBySortOrderAsc(psuId);
        StringBuilder promptBuilder = new StringBuilder();
        for (PromptFragment fragment : fragments) {
            promptBuilder.append(fragment.getContent()).append("\\n");
        }
        // 先输出最小可运行Python模板，后续可继续增强类型映射与SDK化能力。
        String schemaContent = escapeForPythonTripleQuote(latestSchema.getSchemaContent());
        String promptTemplate = escapeForPythonTripleQuote(promptBuilder.toString());
        return String.format(
            "# Complete business code for PSU: %d%n"
                + "# Generated by CodeGeneratorService (python template)%n%n"
                + "import json%n"
                + "import time%n"
                + "from typing import Dict, Any%n%n"
                + "SCHEMA_ID = \"%s\"%n"
                + "SCHEMA_CONTENT = r'''%s'''%n"
                + "PROMPT_TEMPLATE = r'''%s'''%n%n"
                + "def validate_input(data: Dict[str, Any]) -> None:%n"
                + "    # TODO: replace with strict json schema validation%n"
                + "    if not isinstance(data, dict):%n"
                + "        raise ValueError(\"input must be dict\")%n%n"
                + "def assemble_prompt(data: Dict[str, Any]) -> str:%n"
                + "    # TODO: replace with strict variable rendering logic%n"
                + "    return PROMPT_TEMPLATE.strip()%n%n"
                + "def call_resolve_api(data: Dict[str, Any]) -> Dict[str, Any]:%n"
                + "    # TODO: replace with actual resolve endpoint call%n"
                + "    begin = time.time()%n"
                + "    prompt = assemble_prompt(data)%n"
                + "    return {%n"
                + "        \"prompt\": prompt,%n"
                + "        \"latencyMs\": int((time.time() - begin) * 1000),%n"
                + "        \"meta\": {\"psuId\": %d, \"schemaId\": SCHEMA_ID}%n"
                + "    }%n%n"
                + "if __name__ == \"__main__\":%n"
                + "    sample = {\"message\": \"hello\"}%n"
                + "    validate_input(sample)%n"
                + "    result = call_resolve_api(sample)%n"
                + "    print(json.dumps(result, ensure_ascii=False, indent=2))%n",
            psuId,
            latestSchema.getId(),
            schemaContent,
            promptTemplate,
            psuId
        );
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
}
