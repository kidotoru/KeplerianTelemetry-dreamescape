package net.keplerian.telemetry.model;

import java.util.Collection;

/** selectedId: KSD で選択中のオブジェクト ID（未選択なら null） */
public record TelemetryResponse(Long currentTime, Long selectedId, Collection<SpaceObject> objects) {}
