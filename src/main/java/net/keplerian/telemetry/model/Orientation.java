package net.keplerian.telemetry.model;

/**
 * 天体の向き（地軸の傾きと自転角）。いずれも pos と同じ座標軸の単位ベクトル。
 * north: 北極方向、primeMeridian: 経度0・緯度0 の方向、east: 東経90度・緯度0 の方向。
 */
public record Orientation(Vector3 north, Vector3 primeMeridian, Vector3 east) {}
