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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.deviceprint;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.IdRepoException;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

public class DevicePrintDaoTest {

    private DevicePrintDao devicePrintDao;

    @BeforeMethod
    public void setUp() {
        devicePrintDao = new DevicePrintDao();
    }

    @Test
    public void shouldGetProfiles() throws IOException, IdRepoException, SSOException {

        //Given
        AMIdentityWrapper amIdentity = mock(AMIdentityWrapper.class);

        Set ldapProfiles = Collections.singleton("{}");

        given(amIdentity.getAttribute("devicePrintProfiles")).willReturn(ldapProfiles);

        //When
        List<Map<String, Object>> profiles = devicePrintDao.getProfiles(amIdentity);

        //Then
        assertThat(profiles).hasSize(1);
    }

    @Test
    public void shouldSaveProfiles() throws IOException, IdRepoException, SSOException {

        //Given
        AMIdentityWrapper amIdentity = mock(AMIdentityWrapper.class);

        List<Map<String, Object>> profiles = new ArrayList<Map<String, Object>>();
        Map<String, Object> profileOne = new HashMap<String, Object>();
        profileOne.put("uuid", "UUID1");
        profileOne.put("lastSelectedDate", new Date(new Date().getTime() - 172800));
        Map<String, Object> profileTwo = new HashMap<String, Object>();
        profileTwo.put("uuid", "UUID2");
        profileTwo.put("lastSelectedDate", new Date(new Date().getTime() - 86400));
        profiles.add(profileOne);
        profiles.add(profileTwo);

        //When
        devicePrintDao.saveProfiles(amIdentity, profiles);

        //Then
        ArgumentCaptor<Map> ldapProfilesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(amIdentity).setAttributes(ldapProfilesCaptor.capture());
        verify(amIdentity).store();
        Map ldapProfiles = ldapProfilesCaptor.getValue();
        assertThat(((Set) ldapProfiles.get("devicePrintProfiles"))).hasSize(2);
    }
}
