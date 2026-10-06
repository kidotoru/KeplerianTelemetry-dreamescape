package net.keplerian.telemetry.store;

import net.keplerian.telemetry.model.HistorySample;
import net.keplerian.telemetry.model.SelectedHistoryResponse;
import net.keplerian.telemetry.model.KeplerianElements;
import net.keplerian.telemetry.model.SelectedState;
import net.keplerian.telemetry.model.SpaceObject;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Objects;

/**
 * KSD で選択中の宇宙機の対地速度・高度・遠点高度・近点高度の履歴。
 * 選択が切り替わったら破棄して記録し直す。直近 WINDOW_SECONDS（ゲーム内時刻）ぶんだけ保持する。
 */
@Component
public class SelectedHistory {

    /** 保持する時間幅（ゲーム内秒）。これより古い点は捨てる */
    public static final double WINDOW_SECONDS = 1000.0;

    private Long selectedId = null;
    private double startTime = 0.0;
    private final Deque<HistorySample> samples = new ArrayDeque<>();

    /**
     * Telemetry を1回受け取るごとに、store へ反映した後で呼ぶ（遠点・近点は store の軌道要素から求める）。
     * @param selectedId KSD で選択中のオブジェクト ID（未選択なら null）
     * @param currentTime ゲーム内時刻（Unix 秒）
     * @param state 選択中の宇宙機の状態。宇宙機以外が選択されているときは null（記録しない）
     */
    public synchronized void record(Long selectedId, double currentTime, SelectedState state, TelemetryStore store) {
        if (!Objects.equals(selectedId, this.selectedId)) {
            this.selectedId = selectedId;
            samples.clear();
        }
        if (selectedId == null || state == null || state.surfaceSpeed() == null || state.altitude() == null) {
            return;
        }

        if (samples.isEmpty()) {
            startTime = currentTime;
        }
        double t = currentTime - startTime;
        if (!samples.isEmpty()) {
            double lastT = samples.peekLast().t();
            if (t < lastT) {
                // ゲーム内時刻が巻き戻った（Time skip など）。履歴は意味を失うので記録し直す
                samples.clear();
                startTime = currentTime;
                t = 0.0;
            } else if (t == lastT) {
                // 一時停止中など時刻が進んでいない
                return;
            }
        }

        Double apoapsis = null;
        Double periapsis = null;
        SpaceObject obj = store.get(selectedId).orElse(null);
        SpaceObject parent = (obj != null && obj.parentId() != null) ? store.get(obj.parentId()).orElse(null) : null;
        if (obj != null && obj.kep() != null && parent != null && parent.radius() != null) {
            KeplerianElements k = obj.kep();
            // KSD は軌道上でなくても（打ち上げ中も）毎フレーム位置・速度から軌道要素を求め直している。
            // 双曲線では a の符号が経路によって揺れるので、近点半径は |a|×|1-e| で出す
            periapsis = Math.abs(k.a()) * Math.abs(1.0 - k.e()) - parent.radius();
            if (k.e() < 1.0) {
                apoapsis = Math.abs(k.a()) * (1.0 + k.e()) - parent.radius();
            }
        }

        samples.addLast(new HistorySample(t, state.surfaceSpeed(), state.altitude(), apoapsis, periapsis));
        while (samples.peekFirst().t() < t - WINDOW_SECONDS) {
            samples.removeFirst();
        }
    }

    public synchronized SelectedHistoryResponse snapshot() {
        return new SelectedHistoryResponse(selectedId, new ArrayList<>(samples));
    }
}
