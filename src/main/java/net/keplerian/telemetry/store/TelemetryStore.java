package net.keplerian.telemetry.store;

import net.keplerian.telemetry.model.OrbitLeg;
import net.keplerian.telemetry.model.SelectedState;
import net.keplerian.telemetry.model.Orientation;
import net.keplerian.telemetry.model.SpaceObject;
import net.keplerian.telemetry.model.SpaceObjectInfo;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TelemetryStore {

    private final Map<Long, SpaceObject> objects = new ConcurrentHashMap<>();
    private volatile Long currentTime = null;
    /** KSD で選択中の ID と、その宇宙機の状態（宇宙機以外なら null）。別々に読むと食い違いうるので1つにまとめて差し替える */
    public record Selection(Long id, SelectedState state) {}
    private volatile Selection selection = new Selection(null, null);

    public void setCurrentTime(long currentTime) {
        this.currentTime = currentTime;
    }

    public Long getCurrentTime() {
        return currentTime;
    }

    public void setSelection(Long selectedId, SelectedState state) {
        this.selection = new Selection(selectedId, state);
    }

    public Selection getSelection() {
        return selection;
    }

    public void putTelemetry(long id,
                             net.keplerian.telemetry.model.CartesianElements cart,
                             net.keplerian.telemetry.model.KeplerianElements kep,
                             Long orbitRev,
                             List<OrbitLeg> orbitLegs,
                             Orientation orientation) {
        objects.merge(id,
                new SpaceObject(id, null, null, null, null, cart, kep,
                        orbitLegs != null ? orbitRev : null, orbitLegs, orientation),
                (existing, incoming) -> existing.withTelemetry(cart, kep, orbitRev, orbitLegs, orientation));
    }

    public void putInfo(SpaceObjectInfo info) {
        objects.merge(info.id(),
                new SpaceObject(info.id(), info.name(), info.type(), info.parentId(), info.radius(), null, null, null, null, null),
                (existing, incoming) -> existing.withInfo(info.name(), info.type(), info.parentId(), info.radius()));
    }

    /**
     * ids に含まれないオブジェクトを削除する。ObjectList は現存するオブジェクトの一覧なので、
     * 載っていないもの（破棄された段・墜落した機体など）は KSD 側にもう存在しない
     */
    public void retainOnly(java.util.Set<Long> ids) {
        objects.keySet().retainAll(ids);
    }

    public Collection<SpaceObject> getAll() {
        return Collections.unmodifiableCollection(objects.values());
    }

    public Optional<SpaceObject> get(long id) {
        return Optional.ofNullable(objects.get(id));
    }
}
