package com.SoundFork.SoundFork.mergerequest.service;

public class MergeRequestNotFoundException extends RuntimeException {

    public MergeRequestNotFoundException(Long id) {
        super("Merge request with id " + id + " not found");
    }
}
