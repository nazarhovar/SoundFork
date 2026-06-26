package com.SoundFork.SoundFork.mergerequest.controller;

import com.SoundFork.SoundFork.mergerequest.dto.CreateMergeRequest;
import com.SoundFork.SoundFork.mergerequest.dto.MergeRequestResponse;
import com.SoundFork.SoundFork.mergerequest.service.MergeRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class MergeRequestController {

    private final MergeRequestService mergeRequestService;

    @PostMapping("/projects/{projectId}/merge-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public MergeRequestResponse create(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateMergeRequest request,
            Authentication authentication
    ) {
        log.info("Create merge request: targetProjectId={}, from={}", projectId, authentication.getName());
        return mergeRequestService.create(projectId, request, authentication.getName());
    }

    @GetMapping("/projects/{projectId}/merge-requests")
    public List<MergeRequestResponse> getPending(@PathVariable Long projectId) {
        return mergeRequestService.getPendingByProject(projectId);
    }

    @GetMapping("/merge-requests/{id}")
    public MergeRequestResponse getById(@PathVariable Long id) {
        return mergeRequestService.getById(id);
    }

    @PostMapping("/merge-requests/{id}/approve")
    public MergeRequestResponse approve(@PathVariable Long id, Authentication authentication) {
        log.info("Approve merge request: id={}, by={}", id, authentication.getName());
        return mergeRequestService.approve(id, authentication.getName());
    }

    @PostMapping("/merge-requests/{id}/reject")
    public MergeRequestResponse reject(@PathVariable Long id, Authentication authentication) {
        log.info("Reject merge request: id={}, by={}", id, authentication.getName());
        return mergeRequestService.reject(id, authentication.getName());
    }

    @GetMapping("/merge-requests/pending/count")
    public long getPendingCount(Authentication authentication) {
        return mergeRequestService.getPendingCountForUser(authentication.getName());
    }
}
