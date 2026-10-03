package net.keplerian.telemetry.model;

/**
 * KSD で選択中の宇宙機の飛行状態（Telemetry の selectedState）。
 * surfaceSpeed: 対地速度 (m/s)、altitude: 親天体の基準半径からの高度 (m)。非数値は null。
 */
public record SelectedState(Double surfaceSpeed, Double altitude) {}
