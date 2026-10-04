package net.keplerian.telemetry.model;

/**
 * 選択中の宇宙機の過去の軌跡の1点（軌道投入前のみ記録する）。
 * t: ゲーム内時刻（Unix 秒）、parentId: 記録時の親天体 ID、
 * fixed: 親天体からの相対位置を、記録時の親天体の向き（primeMeridian, east, north）で表した成分（m、天体固定座標）。
 */
public record TrackSample(double t, long parentId, Vector3 fixed) {}
