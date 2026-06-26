package com.SoundFork.SoundFork.project.controller;

import com.SoundFork.SoundFork.common.dto.PageResponse;
import com.SoundFork.SoundFork.project.dto.ProjectResponse;
import com.SoundFork.SoundFork.project.dto.UpdateProjectRequest;
import com.SoundFork.SoundFork.project.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
@Slf4j
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "genre", required = false) String genre,
            @RequestParam(value = "cover", required = false) MultipartFile cover,
            Authentication authentication
    ) {
        return projectService.create(title, description, genre, cover, authentication.getName());
    }

    @PostMapping("/{id}/cover")
    @ResponseStatus(HttpStatus.OK)
    public ProjectResponse uploadCover(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        return projectService.uploadCover(id, file, authentication.getName());
    }


    @GetMapping("/{id}")
    public ProjectResponse getById(@PathVariable Long id) {
        return projectService.getById(id);
    }

    @PutMapping("/{id}")
    public ProjectResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProjectRequest request,
            Authentication authentication
    ) {
        return projectService.update(id, request, authentication.getName());
    }

    @PostMapping("/{id}/fork")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse fork(@PathVariable Long id, Authentication authentication) {
        return projectService.fork(id, authentication.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        projectService.delete(id, authentication.getName());
    }

    @GetMapping
    public PageResponse<ProjectResponse> getAll(
            @PageableDefault(size = 12, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return projectService.getAll(pageable);
    }

    @GetMapping("/author/{authorId}")
    public PageResponse<ProjectResponse> getByAuthor(
            @PathVariable Long authorId,
            @PageableDefault(size = 12, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return projectService.getByAuthorId(authorId, pageable);
    }

}

