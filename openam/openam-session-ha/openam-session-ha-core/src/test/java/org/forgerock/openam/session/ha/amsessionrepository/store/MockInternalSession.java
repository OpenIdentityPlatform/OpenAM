package org.forgerock.openam.session.ha.amsessionrepository.store;

import org.forgerock.openam.session.model.AMRecord;
import org.forgerock.openam.session.model.FAMRecord;

import java.io.Serializable;

/**
 * MockInternalSession Test POJO.
 *
 * @author jeff.schenk@forgerock.com
 * @version 10.1
 * @since <pre>Aug 29, 2012</pre>
 */
public class MockInternalSession implements Serializable {
    private static final long serialVersionUID = 101L;   //  10.1

    private AMRecord amRecord;

    private FAMRecord famRecord;

    public MockInternalSession() {
    }

    public AMRecord getAmRecord() {
        return amRecord;
    }

    public void setAmRecord(AMRecord amRecord) {
        this.amRecord = amRecord;
    }

    public FAMRecord getFamRecord() {
        return famRecord;
    }

    public void setFamRecord(FAMRecord famRecord) {
        this.famRecord = famRecord;
    }

}
