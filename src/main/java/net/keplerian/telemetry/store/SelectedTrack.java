package net.keplerian.telemetry.store;

import net.keplerian.telemetry.model.Orientation;
import net.keplerian.telemetry.model.SelectedState;
import net.keplerian.telemetry.model.SelectedTrackResponse;
import net.keplerian.telemetry.model.SpaceObject;
import net.keplerian.telemetry.model.TrackSample;
import net.keplerian.telemetry.model.Vector3;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Objects;

/**
 * KSD で選択中の宇宙機の過去の軌跡。KSD の実績軌道と同じく軌道投入前（打ち上げ・弾道飛行）だけを扱い、
 * 位置は親天体に固定した座標（地表に対する軌跡）で持つ。
 * 選択の切り替え・軌道投入・ゲーム内時刻の巻き戻りで破棄する。
 */
@Component
public class SelectedTrack {

    /** 保持する最大点数（1秒に1点なので、等倍なら1時間ぶん） */
    public static final int MAX_SAMPLES = 3_600;

    /** 前の点からこれ未満しか動いていなければ追加しない（射点で待機している間など） (m) */
    private static final double MIN_STEP_METERS = 1.0;

    private Long selectedId = null;
    private final Deque<TrackSample> samples = new ArrayDeque<>();

    /**
     * Telemetry を1回受け取るごとに、store へ反映した後で呼ぶ。
     * @param state 選択中の宇宙機の状態。宇宙機以外が選択されているときは null（記録しない）
     */
    public synchronized void record(Long selectedId, double currentTime, SelectedState state, TelemetryStore store) {
        if (!Objects.equals(selectedId, this.selectedId)) {
            this.selectedId = selectedId;
            samples.clear();
        }
        if (selectedId == null || state == null) {
            return;
        }
        if (Boolean.TRUE.equals(state.inOrbit())) {
            // 軌道投入後は不要
            samples.clear();
            return;
        }

        SpaceObject obj = store.get(selectedId).orElse(null);
        if (obj == null || obj.parentId() == null || obj.cart() == null || obj.cart().pos() == null) {
            return;
        }
        SpaceObject parent = store.get(obj.parentId()).orElse(null);
        if (parent == null || parent.cart() == null || parent.cart().pos() == null) {
            return;
        }
        Vector3 fixed = toBodyFixed(obj.cart().pos(), parent.cart().pos(), parent.orientation());
        if (fixed == null) {
            return;
        }

        TrackSample last = samples.peekLast();
        if (last != null) {
            if (currentTime < last.t()) {
                // ゲーム内時刻が巻き戻った（Time skip など）。軌跡は意味を失うので記録し直す
                samples.clear();
            } else if (last.parentId() == obj.parentId() && distance(last.fixed(), fixed) < MIN_STEP_METERS) {
                return;
            }
        }

        samples.addLast(new TrackSample(currentTime, obj.parentId(), fixed));
        while (samples.size() > MAX_SAMPLES) {
            samples.removeFirst();
        }
    }

    public synchronized SelectedTrackResponse snapshot() {
        return new SelectedTrackResponse(selectedId, new ArrayList<>(samples));
    }

    // 親天体からの相対位置を、親天体の向き（経度0・東経90度・北極の3軸）で表した成分にする
    private static Vector3 toBodyFixed(Vector3 pos, Vector3 parentPos, Orientation o) {
        if (o == null || o.primeMeridian() == null || o.east() == null || o.north() == null) {
            return null;
        }
        Vector3 rel = new Vector3(pos.x() - parentPos.x(), pos.y() - parentPos.y(), pos.z() - parentPos.z());
        return new Vector3(dot(rel, o.primeMeridian()), dot(rel, o.east()), dot(rel, o.north()));
    }

    private static double dot(Vector3 a, Vector3 b) {
        return a.x() * b.x() + a.y() * b.y() + a.z() * b.z();
    }

    private static double distance(Vector3 a, Vector3 b) {
        return Math.sqrt((a.x() - b.x()) * (a.x() - b.x()) + (a.y() - b.y()) * (a.y() - b.y()) + (a.z() - b.z()) * (a.z() - b.z()));
    }
}
