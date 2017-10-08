/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openam.idrepo.ldap;

import com.sun.identity.idm.IdRepoListener;
import org.forgerock.openam.utils.MapHelper;
import org.powermock.api.mockito.PowerMockito;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.testng.Assert.fail;


public class PSearchRepoTest  extends IdRepoTestBase {

    private static final String PSEARCH_SETTINGS = "/config/psearchsettings.properties";
    private static final String GENERIC_DS_LDIF = "/ldif/generic.ldif";

    @BeforeClass
    public void setUp() throws Exception {
        idrepo.initialize(MapHelper.readMap(PSEARCH_SETTINGS));
    }

    @Override
    protected String getLDIFPath() {
        return GENERIC_DS_LDIF;
    }

    @Test
    public void addListenerNoPSearch() {
        idrepo.addListener(null, idRepoListener);
        assertThat(idrepo.getPsearchMap()).hasSize(0);
    }

    @Test(dependsOnMethods = "addListenerNoPSearch", expectedExceptions = IllegalStateException.class)
    public void exceptionListenerAlreadyExists() {
        IdRepoListener newIdRepoListener = PowerMockito.mock(IdRepoListener.class);
        idrepo.addListener(null, newIdRepoListener);
    }

}
