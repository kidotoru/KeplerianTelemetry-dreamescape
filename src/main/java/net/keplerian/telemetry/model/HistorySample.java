package net.keplerian.telemetry.model;

/**
 * 選択中の宇宙機の履歴の1点。
 * t: 選択（記録開始）からのゲーム内経過秒、surfaceSpeed: 対地速度 (m/s)、altitude: 高度 (m)。
 */
public record HistorySample(double t, double surfaceSpeed, double altitude) {}
