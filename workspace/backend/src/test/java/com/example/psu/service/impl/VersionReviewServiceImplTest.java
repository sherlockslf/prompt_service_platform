package com.example.psu.service.impl;

import com.example.psu.dto.request.ReviewRequest;
import com.example.psu.entity.PromptComposition;
import com.example.psu.entity.PromptCompositionRevision;
import com.example.psu.entity.PsuUnit;
import com.example.psu.entity.VersionReview;
import com.example.psu.enums.CompositionStatus;
import com.example.psu.enums.PsuStatus;
import com.example.psu.enums.RejectionType;
import com.example.psu.enums.ReviewStatus;
import com.example.psu.exception.BusinessException;
import com.example.psu.repository.PromptCompositionRepository;
import com.example.psu.repository.PromptCompositionRevisionRepository;
import com.example.psu.repository.PsuRepository;
import com.example.psu.repository.VersionReviewRepository;
import com.example.psu.service.CodeGeneratorService;
import com.example.psu.service.CompositionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class VersionReviewServiceImplTest {

    @Mock
    private VersionReviewRepository versionReviewRepository;

    @Mock
    private PsuRepository psuRepository;

    @Mock
    private PromptCompositionRepository promptCompositionRepository;

    @Mock
    private PromptCompositionRevisionRepository promptCompositionRevisionRepository;

    @Mock
    private CompositionService compositionService;

    @Mock
    private CodeGeneratorService codeGeneratorService;

    @InjectMocks
    private VersionReviewServiceImpl versionReviewService;

    @Test
    void submitVersion_shouldCreatePendingReview() {
        PsuUnit psu = new PsuUnit();
        psu.setId(1L);
        psu.setVersionNo(123);
        psu.setStatus(PsuStatus.DRAFT);

        PromptComposition draft = new PromptComposition();
        draft.setId(10L);
        draft.setPsuId(1L);
        draft.setStatus(CompositionStatus.DRAFT);

        PromptComposition submitted = new PromptComposition();
        submitted.setId(10L);
        submitted.setPsuId(1L);
        submitted.setStatus(CompositionStatus.CANDIDATE);

        PromptCompositionRevision revision = new PromptCompositionRevision();
        revision.setRevisionNo(5);

        when(psuRepository.findById(1L)).thenReturn(Optional.of(psu));
        when(compositionService.getCompositionByPsuId(1L)).thenReturn(Optional.of(draft));
        when(compositionService.submit(1L, 100L)).thenReturn(submitted);
        when(compositionService.getLatestRevision(10L)).thenReturn(Optional.of(revision));
        when(versionReviewRepository.existsByCompositionIdAndCompositionRevisionNoAndStatus(10L, 5, ReviewStatus.CANDIDATE))
            .thenReturn(false);
        when(versionReviewRepository.save(any(VersionReview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VersionReview review = versionReviewService.submitVersion(1L, 100L);

        assertEquals(ReviewStatus.CANDIDATE, review.getStatus());
        assertEquals(123, review.getVersionNo());
        assertEquals(5, review.getCompositionRevisionNo());
    }

    @Test
    void reviewVersion_approved_shouldSetApprovedAndUpdateComposition() {
        VersionReview pending = new VersionReview();
        pending.setId(7L);
        pending.setPsuId(1L);
        pending.setStatus(ReviewStatus.CANDIDATE);

        PsuUnit psu = new PsuUnit();
        psu.setId(1L);
        psu.setStatus(PsuStatus.CANDIDATE);

        ReviewRequest request = new ReviewRequest();
        request.setApproved(true);

        when(versionReviewRepository.findById(7L)).thenReturn(Optional.of(pending));
        when(psuRepository.findById(1L)).thenReturn(Optional.of(psu));
        when(codeGeneratorService.generateCompleteBusinessCode(1L)).thenReturn("// code");
        when(versionReviewRepository.findByPsuIdOrderBySubmittedAtDesc(1L)).thenReturn(List.of(pending));
        when(versionReviewRepository.save(any(VersionReview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VersionReview result = versionReviewService.reviewVersion(7L, request, 9L);

        assertEquals(ReviewStatus.FORMAL, result.getStatus());
        assertEquals("// code", result.getCodeContent());
        verify(compositionService).updateStatus(1L, CompositionStatus.FORMAL, null, null);
    }

    @Test
    void reviewVersion_rejectedBackToBiz_shouldSetDraft() {
        VersionReview pending = new VersionReview();
        pending.setId(8L);
        pending.setPsuId(1L);
        pending.setStatus(ReviewStatus.CANDIDATE);

        ReviewRequest request = new ReviewRequest();
        request.setApproved(false);
        request.setRejectionReason("需要重编排");
        request.setRejectionType(RejectionType.BACK_TO_BIZ);

        PsuUnit psu = new PsuUnit();
        psu.setId(1L);
        psu.setStatus(PsuStatus.CANDIDATE);

        PromptComposition draft = new PromptComposition();
        draft.setId(10L);
        draft.setPsuId(1L);
        draft.setStatus(CompositionStatus.DRAFT);

        when(versionReviewRepository.findById(8L)).thenReturn(Optional.of(pending));
        when(psuRepository.findById(1L)).thenReturn(Optional.of(psu));
        when(compositionService.updateStatus(1L, CompositionStatus.DRAFT, "需要重编排", RejectionType.BACK_TO_BIZ)).thenReturn(draft);
        when(versionReviewRepository.save(any(VersionReview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VersionReview result = versionReviewService.reviewVersion(8L, request, 9L);

        assertEquals(ReviewStatus.ARCHIVED, result.getStatus());
        assertEquals(RejectionType.BACK_TO_BIZ, result.getRejectionType());
        verify(compositionService).createRevisionSnapshot(draft, 9L);
    }

    @Test
    void getCode_shouldThrowWhenNoApprovedReview() {
        when(versionReviewRepository.findTopByPsuIdAndStatusOrderByReviewedAtDesc(1L, ReviewStatus.FORMAL))
            .thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> versionReviewService.getCode(1L));
    }

    @Test
    void getVersionReviews_withPsuId_shouldReturnPage() {
        VersionReview review = new VersionReview();
        review.setId(1L);
        review.setPsuId(1L);
        review.setStatus(ReviewStatus.CANDIDATE);

        Page<VersionReview> page = new PageImpl<>(List.of(review), PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "submittedAt")), 1);
        when(versionReviewRepository.findByPsuId(1L, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "submittedAt"))))
            .thenReturn(page);

        Page<VersionReview> result = versionReviewService.getVersionReviews(1L, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "submittedAt")));

        assertEquals(1, result.getTotalElements());
        assertEquals(ReviewStatus.CANDIDATE, result.getContent().get(0).getStatus());
    }

    @Test
    void getVersionReviews_withoutPsuId_shouldReturnAllPage() {
        VersionReview review1 = new VersionReview();
        review1.setId(1L);
        review1.setPsuId(1L);
        review1.setStatus(ReviewStatus.CANDIDATE);

        VersionReview review2 = new VersionReview();
        review2.setId(2L);
        review2.setPsuId(2L);
        review2.setStatus(ReviewStatus.FORMAL);

        Page<VersionReview> page = new PageImpl<>(List.of(review1, review2), PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "submittedAt")), 2);
        when(versionReviewRepository.findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "submittedAt"))))
            .thenReturn(page);

        Page<VersionReview> result = versionReviewService.getVersionReviews(null, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "submittedAt")));

        assertEquals(2, result.getTotalElements());
    }
}

