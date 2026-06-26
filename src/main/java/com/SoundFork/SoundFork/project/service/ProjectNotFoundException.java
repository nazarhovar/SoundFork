package com.SoundFork.SoundFork.project.service;

public class ProjectNotFoundException extends RuntimeException {

    public ProjectNotFoundException(Long id) {
        super("Project with id " + id + " not found");
    }
}
