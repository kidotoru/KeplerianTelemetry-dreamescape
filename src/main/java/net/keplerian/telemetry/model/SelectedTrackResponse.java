package net.keplerian.telemetry.model;

import java.util.List;

/**
 * GET /api/track のレスポンス。
 * inOrbit: 選択中の宇宙機が周回軌道上か（最新値。宇宙機以外の選択・未選択なら null）
 */
public record SelectedTrackResponse(Long selectedId, Boolean inOrbit, List<TrackSample> samples) {}
