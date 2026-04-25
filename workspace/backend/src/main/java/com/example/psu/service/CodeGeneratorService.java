package com.example.psu.service;

import com.example.psu.entity.JsonSchema;
import com.example.psu.entity.PromptFragment;
import com.example.psu.repository.JsonSchemaRepository;
import com.example.psu.repository.PromptFragmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 代码生成服务
 */
@Service
public class CodeGeneratorService {
    
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
        // 获取最新的Schema
        JsonSchema latestSchema = jsonSchemaRepository.findTopByPsuIdOrderByVersionDesc(psuId)
                .orElseThrow(() -> new RuntimeException("Schema not found for PSU: " + psuId));
        
        // 生成各个部分的代码
        String validationCode = generateParameterValidationCode(latestSchema);
        String assemblyCode = generateParameterAssemblyCode(latestSchema, latestSchema); // 简化实现
        String promptCode = generatePromptAssemblyCode(psuId);
        
        // 组合成完整代码
        return """
            // Complete business code for PSU: """ + psuId + """
            
            """ + validationCode + """
            
            """ + assemblyCode + """
            
            """ + promptCode + """
            """;
    }
}
