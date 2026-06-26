package com.SoundFork.SoundFork.version.service;

public class VersionNotFoundException extends RuntimeException {

    public VersionNotFoundException(Long id) {
        super("Version with id " + id + " not found");
    }
}
