package net.keplerian.telemetry.store;

import net.keplerian.telemetry.model.OrbitLeg;
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
    private volatile Long selectedId = null;

    public void setCurrentTime(long currentTime) {
        this.currentTime = currentTime;
    }

    public Long getCurrentTime() {
        return currentTime;
    }

    public void setSelectedId(Long selectedId) {
        this.selectedId = selectedId;
    }

    public Long getSelectedId() {
        return selectedId;
    }

    public void putTelemetry(long id,
                             net.keplerian.telemetry.model.CartesianElements cart,
                             net.keplerian.telemetry.model.KeplerianElements kep,
                             Long orbitRev,
                             List<OrbitLeg> orbitLegs) {
        objects.merge(id,
                new SpaceObject(id, null, null, null, null, cart, kep,
                        orbitLegs != null ? orbitRev : null, orbitLegs),
                (existing, incoming) -> existing.withTelemetry(cart, kep, orbitRev, orbitLegs));
    }

    public void putInfo(SpaceObjectInfo info) {
        objects.merge(info.id(),
                new SpaceObject(info.id(), info.name(), info.type(), info.parentId(), info.radius(), null, null, null, null),
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
