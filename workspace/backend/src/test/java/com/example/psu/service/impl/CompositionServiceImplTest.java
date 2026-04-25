package com.example.psu.service.impl;

import com.example.psu.dto.request.CompositionRenderRequest;
import com.example.psu.dto.request.CompositionSaveRequest;
import com.example.psu.dto.response.CompositionRenderResponse;
import com.example.psu.dto.response.CompositionValidateResponse;
import com.example.psu.entity.JsonSchema;
import com.example.psu.entity.PromptComposition;
import com.example.psu.entity.PromptCompositionRevision;
import com.example.psu.enums.CompositionStatus;
import com.example.psu.enums.PsuStatus;
import com.example.psu.exception.BusinessException;
import com.example.psu.repository.JsonSchemaRepository;
import com.example.psu.repository.PromptCompositionRepository;
import com.example.psu.repository.PromptCompositionRevisionRepository;
import com.example.psu.repository.PsuRepository;
import com.example.psu.entity.PsuUnit;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompositionServiceImplTest {

    @Mock
    private PromptCompositionRepository compositionRepository;

    @Mock
    private PromptCompositionRevisionRepository revisionRepository;

    @Mock
    private JsonSchemaRepository jsonSchemaRepository;

    @Mock
    private PsuRepository psuRepository;

    @InjectMocks
    private CompositionServiceImpl compositionService;

    @BeforeEach
    void setUp() {
        compositionService = new CompositionServiceImpl(
            compositionRepository,
            revisionRepository,
            jsonSchemaRepository,
            psuRepository,
            new ObjectMapper()
        );
    }

    @Test
    void saveDraft_shouldThrowWhenCompositionLocked() {
        PsuUnit psu = new PsuUnit();
        psu.setId(1L);
        psu.setStatus(PsuStatus.DRAFT);
        PromptComposition existing = new PromptComposition();
        existing.setPsuId(1L);
        existing.setStatus(CompositionStatus.CANDIDATE);

        CompositionSaveRequest request = new CompositionSaveRequest();
        request.setContent("hello {{userId}}");
        request.setInjectionPlan(List.of(Map.of("path", "userId")));

        JsonSchema schema = new JsonSchema();
        schema.setSchemaContent("{\"type\":\"object\",\"properties\":{\"userId\":{\"type\":\"string\"}}}");

        when(psuRepository.findById(1L)).thenReturn(Optional.of(psu));
        when(compositionRepository.findByPsuId(1L)).thenReturn(Optional.of(existing));
        when(jsonSchemaRepository.findTopByPsuIdOrderByVersionDesc(1L)).thenReturn(Optional.of(schema));

        assertThrows(BusinessException.class, () -> compositionService.saveDraft(1L, request, 2L));
    }

    @Test
    void validate_shouldReportInjectionAndSchemaErrors() {
        CompositionSaveRequest request = new CompositionSaveRequest();
        request.setContent("hello {{userId}} {{order.id}}");
        request.setInjectionPlan(List.of(Map.of("path", "userId")));

        JsonSchema schema = new JsonSchema();
        schema.setSchemaContent("{\"type\":\"object\",\"properties\":{\"userId\":{\"type\":\"string\"}}}");
        when(jsonSchemaRepository.findTopByPsuIdOrderByVersionDesc(1L)).thenReturn(Optional.of(schema));

        CompositionValidateResponse response = compositionService.validate(1L, request);

        assertFalse(response.isOk());
        assertTrue(response.getErrors().stream().anyMatch(e -> "INJECTION_MISSING".equals(e.get("code"))));
        assertTrue(response.getErrors().stream().anyMatch(e -> "VAR_NOT_FOUND".equals(e.get("code"))));
    }

    @Test
    void submit_shouldUpdateStatusAndCreateSnapshot() {
        PsuUnit psu = new PsuUnit();
        psu.setId(1L);
        psu.setStatus(PsuStatus.DRAFT);
        PromptComposition composition = new PromptComposition();
        composition.setId(10L);
        composition.setPsuId(1L);
        composition.setStatus(CompositionStatus.DRAFT);
        composition.setSchemaVersion(2);
        composition.setContent("hello {{userId}}");
        composition.setSpecJson("{\"content\":\"hello {{userId}}\",\"injectionPlan\":[{\"path\":\"userId\"}]}");

        JsonSchema schema = new JsonSchema();
        schema.setSchemaContent("{\"type\":\"object\",\"properties\":{\"userId\":{\"type\":\"string\"}}}");

        when(psuRepository.findById(1L)).thenReturn(Optional.of(psu));
        when(compositionRepository.findByPsuId(1L)).thenReturn(Optional.of(composition));
        when(jsonSchemaRepository.findTopByPsuIdOrderByVersionDesc(1L)).thenReturn(Optional.of(schema));
        when(compositionRepository.save(any(PromptComposition.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(revisionRepository.findTopByCompositionIdOrderByRevisionNoDesc(10L)).thenReturn(Optional.empty());
        when(revisionRepository.save(any(PromptCompositionRevision.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PromptComposition result = compositionService.submit(1L, 99L);

        assertEquals(CompositionStatus.CANDIDATE, result.getStatus());
        assertEquals(99L, result.getUpdatedBy());
    }

    @Test
    void render_shouldReturnMissingVars() {
        PromptComposition composition = new PromptComposition();
        composition.setId(10L);
        composition.setPsuId(1L);
        composition.setContent("hello {{userId}} {{orderId}}");

        CompositionRenderRequest request = new CompositionRenderRequest();
        request.setCompositionId(10L);
        request.setInput(Map.of("userId", "u-1"));

        when(compositionRepository.findById(10L)).thenReturn(Optional.of(composition));

        CompositionRenderResponse response = compositionService.render(1L, request);

        assertTrue(response.getRenderedPrompt().contains("u-1"));
        assertTrue(response.getMissingVars().contains("orderId"));
    }
}
