package net.keplerian.telemetry.model;

/**
 * 選択中の宇宙機の履歴の1点。
 * t: 選択（記録開始）からのゲーム内経過秒、surfaceSpeed: 対地速度 (m/s)、altitude: 高度 (m)、
 * apoapsis / periapsis: 遠点・近点の高度 (m。親天体の表面から。軌道が地表と交差していれば近点は負)、
 * eccentricity: 離心率。
 * 軌道要素が得られないとき（発射台で待機中など）は3つとも null、双曲線軌道（遠点なし）のときは apoapsis だけ null。
 */
public record HistorySample(double t, double surfaceSpeed, double altitude, Double apoapsis, Double periapsis, Double eccentricity) {}
