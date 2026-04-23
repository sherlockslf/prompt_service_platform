package com.example.psu.service;

import com.example.psu.dto.request.CompositionRenderRequest;
import com.example.psu.dto.request.CompositionSaveRequest;
import com.example.psu.dto.response.CompositionRenderResponse;
import com.example.psu.dto.response.CompositionValidateResponse;
import com.example.psu.entity.PromptComposition;
import com.example.psu.entity.PromptCompositionRevision;
import com.example.psu.enums.CompositionStatus;
import com.example.psu.enums.RejectionType;

import java.util.Optional;

public interface CompositionService {

    Optional<PromptComposition> getCompositionByPsuId(Long psuId);

    PromptComposition saveDraft(Long psuId, CompositionSaveRequest request, Long userId);

    CompositionValidateResponse validate(Long psuId, CompositionSaveRequest request);

    CompositionRenderResponse render(Long psuId, CompositionRenderRequest request);

    PromptComposition submit(Long psuId, Long userId);

    PromptComposition updateStatus(Long psuId, CompositionStatus status, String rejectionReason, RejectionType rejectionType);

    PromptCompositionRevision createRevisionSnapshot(PromptComposition composition, Long operatorId);

    Optional<PromptCompositionRevision> getLatestRevision(Long compositionId);
}
