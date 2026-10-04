package net.keplerian.telemetry.model;

import java.util.Collection;
import java.util.List;

/**
 * selectedId: KSD で選択中のオブジェクト ID（未選択なら null）
 * selectedInOrbit: 選択中の宇宙機が周回軌道上か（宇宙機以外の選択・未選択なら null）。selectedId と同じ時点の値
 * selectedLaunchTime / selectedFlightLog: 選択中の宇宙機の打ち上げ時刻（Unix 秒）とフライトログ（宇宙機以外の選択・未選択なら null）
 */
public record TelemetryResponse(Long currentTime, Long selectedId, Boolean selectedInOrbit,
                                Double selectedLaunchTime, List<FlightLogEntry> selectedFlightLog,
                                Collection<SpaceObject> objects) {}
