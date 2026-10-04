package net.keplerian.telemetry.model;

/**
 * KSD のフライトログの1件。
 * event: 種類（LiftOff, MaxQ, BoosterJettison, StageSeparation, FairingDeployment,
 * EngineIgnition, EngineCutoff, OrbitInsertion。KSD 側で増えることがある）、
 * time: 打ち上げからの経過秒、altitude: 高度 (m)、velocity: 速度 (m/s)。
 */
public record FlightLogEntry(String event, Double time, Double altitude, Double velocity) {}
