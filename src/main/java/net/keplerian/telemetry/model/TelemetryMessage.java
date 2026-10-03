package net.keplerian.telemetry.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * selectedId: KSD で選択中のオブジェクト ID（未選択なら null）
 * selectedState: 選択中の宇宙機の飛行状態（宇宙機以外の選択・未選択なら null）
 */
public record TelemetryMessage(double currentTime, Long selectedId, SelectedState selectedState, List<SpaceObjectInput> spaceObjects) {}
