package com.github.tacowasa059.bowlingplayergame.game;

/** Lifecycle of a single Kaidoro round. */
public enum GamePhase {
    /** No game running. */
    IDLE,
    /** Game running. Red is deployed; blue deploys after the configured delay. */
    RUNNING,
    /** Game finished, result is being shown. Reset with the stop command. */
    ENDED
}
