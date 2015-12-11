package org.forgerock.openam.core;

import static org.forgerock.openam.utils.RealmUtils.cleanRealm;
import static org.forgerock.openam.utils.RealmUtils.concatenateRealmPath;

import org.forgerock.util.Reject;

/**
 *
 */
public class RealmInfo {

    private final String absoluteRealm;
    private String realmSubPath;
    private String overrideRealm;

    public RealmInfo(String absoluteRealm, String realmSubPath, String overrideRealm) {
        Reject.ifNull(absoluteRealm);
        this.absoluteRealm = absoluteRealm;
        this.realmSubPath = realmSubPath;
        this.overrideRealm = overrideRealm;
    }

    public RealmInfo(String absoluteRealm) {
        this(absoluteRealm, null, null);
    }

    public String getAbsoluteRealm() {
        return absoluteRealm;
    }

    public String getRealmSubPath() {
        return realmSubPath;
    }

    public RealmInfo appendUriRealm(String realmSubPath) {
        Reject.ifNull(realmSubPath);
        return new RealmInfo(
                concatenateRealmPath(absoluteRealm, cleanRealm(realmSubPath)),
                concatenateRealmPath(this.realmSubPath, cleanRealm(realmSubPath)),
                overrideRealm);
    }

    //TODO ensure document immutablility
    public RealmInfo withOverrideRealm(String overrideRealm) {
        overrideRealm = cleanRealm(overrideRealm);
        return new RealmInfo(overrideRealm, realmSubPath, overrideRealm);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RealmInfo realmInfo = (RealmInfo) o;

        if (!absoluteRealm.equals(realmInfo.absoluteRealm)) return false;
        if (realmSubPath != null ? !realmSubPath.equals(realmInfo.realmSubPath) : realmInfo.realmSubPath != null)
            return false;
        return overrideRealm != null ? overrideRealm.equals(realmInfo.overrideRealm) : realmInfo.overrideRealm == null;

    }

    @Override
    public int hashCode() {
        int result = absoluteRealm.hashCode();
        result = 31 * result + (realmSubPath != null ? realmSubPath.hashCode() : 0);
        result = 31 * result + (overrideRealm != null ? overrideRealm.hashCode() : 0);
        return result;
    }

    public RealmInfo withAbsoluteRealm(String absoluteRealm) {
        absoluteRealm = cleanRealm(absoluteRealm);
        return new RealmInfo(absoluteRealm, realmSubPath, overrideRealm);
    }
}
