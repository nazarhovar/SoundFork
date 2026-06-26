package com.SoundFork.SoundFork.track.service;

public class TrackNotFoundException extends RuntimeException {

    public TrackNotFoundException(Long id) {
        super("Track with id " + id + " not found");
    }
}
