package net.keplerian.telemetry.model;

import java.util.Collection;

/**
 * selectedId: KSD で選択中のオブジェクト ID（未選択なら null）
 * selectedInOrbit: 選択中の宇宙機が周回軌道上か（宇宙機以外の選択・未選択なら null）。selectedId と同じ時点の値
 */
public record TelemetryResponse(Long currentTime, Long selectedId, Boolean selectedInOrbit, Collection<SpaceObject> objects) {}
