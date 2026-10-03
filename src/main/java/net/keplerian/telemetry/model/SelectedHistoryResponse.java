package net.keplerian.telemetry.model;

import java.util.List;

/** GET /api/history のレスポンス */
public record SelectedHistoryResponse(Long selectedId, List<HistorySample> samples) {}
