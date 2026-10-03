package net.keplerian.telemetry.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
/** selectedId: KSD で選択中のオブジェクト ID（未選択なら null） */
public record TelemetryMessage(long currentTime, Long selectedId, List<SpaceObjectInput> spaceObjects) {}
