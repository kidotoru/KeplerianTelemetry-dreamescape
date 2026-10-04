package net.keplerian.telemetry.model;

import java.util.List;

/** GET /api/track のレスポンス */
public record SelectedTrackResponse(Long selectedId, List<TrackSample> samples) {}
