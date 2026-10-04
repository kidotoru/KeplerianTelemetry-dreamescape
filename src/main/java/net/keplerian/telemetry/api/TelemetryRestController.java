package net.keplerian.telemetry.api;

import net.keplerian.telemetry.model.SelectedHistoryResponse;
import net.keplerian.telemetry.model.SelectedState;
import net.keplerian.telemetry.model.SelectedTrackResponse;
import net.keplerian.telemetry.model.SpaceObject;
import net.keplerian.telemetry.model.TelemetryResponse;
import net.keplerian.telemetry.store.SelectedHistory;
import net.keplerian.telemetry.store.SelectedTrack;
import net.keplerian.telemetry.store.TelemetryStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
@RequestMapping("/api")
public class TelemetryRestController {

    private final TelemetryStore store;
    private final SelectedHistory selectedHistory;
    private final SelectedTrack selectedTrack;

    public TelemetryRestController(TelemetryStore store, SelectedHistory selectedHistory, SelectedTrack selectedTrack) {
        this.store = store;
        this.selectedHistory = selectedHistory;
        this.selectedTrack = selectedTrack;
    }

    /**
     * @param orbits true のとき軌道線（orbitLegs）も返す。省略時は orbitRev だけを返す
     */
    @GetMapping("/objects")
    public TelemetryResponse getAll(@RequestParam(defaultValue = "false") boolean orbits) {
        Collection<SpaceObject> objects = store.getAll();
        if (!orbits) {
            objects = objects.stream().map(SpaceObject::withoutOrbitLegs).toList();
        }
        TelemetryStore.Selection selection = store.getSelection();
        SelectedState state = selection.state();
        return new TelemetryResponse(store.getCurrentTime(), selection.id(),
                state != null ? state.inOrbit() : null,
                state != null ? state.launchTime() : null,
                state != null ? state.flightLog() : null,
                objects);
    }

    /**
     * @param orbits true のとき軌道線（orbitLegs）も返す。省略時は orbitRev だけを返す
     */
    /**
     * KSD で選択中の宇宙機の対地速度・高度の履歴（選択からの経過時間つき、直近1000秒ぶん）
     */
    @GetMapping("/history")
    public SelectedHistoryResponse getHistory() {
        return selectedHistory.snapshot();
    }

    /**
     * KSD で選択中の宇宙機の過去の軌跡（軌道投入前のみ、親天体固定座標）
     */
    @GetMapping("/track")
    public SelectedTrackResponse getTrack() {
        return selectedTrack.snapshot();
    }

    @GetMapping("/objects/{id}")
    public ResponseEntity<SpaceObject> getById(@PathVariable long id,
                                               @RequestParam(defaultValue = "false") boolean orbits) {
        return store.get(id)
                .map(o -> orbits ? o : o.withoutOrbitLegs())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
