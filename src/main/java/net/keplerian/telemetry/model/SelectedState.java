package net.keplerian.telemetry.model;

/**
 * KSD で選択中の宇宙機の飛行状態（Telemetry の selectedState）。
 * surfaceSpeed: 対地速度 (m/s)、altitude: 親天体の基準半径からの高度 (m)、
 * inOrbit: 周回軌道上か（近点高度 > カーマンライン）、launchTime: 打ち上げ時刻（T0、Unix 秒）、
 * flightLog: フライトログ（古い順）。非数値は null。
 */
public record SelectedState(Double surfaceSpeed, Double altitude, Boolean inOrbit,
                            Double launchTime, java.util.List<FlightLogEntry> flightLog) {}
