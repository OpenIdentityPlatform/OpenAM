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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ProfilePersisterTest {

    private ProfilePersister profilePersister;

    private DevicePrintDao devicePrintDao;
    private AMIdentityWrapper amIdentity;

    @BeforeMethod
    public void setUp() {

        devicePrintDao = mock(DevicePrintDao.class);
        amIdentity = mock(AMIdentityWrapper.class);

        profilePersister = new ProfilePersister(2, devicePrintDao, amIdentity);
    }

    @Test
    public void shouldAddNewDevicePrintToProfiles() throws IOException, IdRepoException, SSOException {

        //Given
        Map<String, Object> devicePrintProfile = new HashMap<String, Object>();

        Map<String, Object> profileOne = new HashMap<String, Object>();
        profileOne.put("uuid", "UUID1");
        profileOne.put("lastSelectedDate", new Date().getTime() - 172800);
        Map<String, Object> profileTwo = new HashMap<String, Object>();
        profileTwo.put("uuid", "UUID2");
        profileTwo.put("lastSelectedDate", new Date().getTime() - 86400);

        List<Map<String, Object>> profiles = new ArrayList<Map<String, Object>>();
        profiles.add(profileOne);
        profiles.add(profileTwo);
        given(devicePrintDao.getProfiles(amIdentity)).willReturn(profiles);

        //When
        profilePersister.saveDevicePrint(devicePrintProfile);

        //Then
        ArgumentCaptor<List> profilesCaptor = ArgumentCaptor.forClass(List.class);
        verify(devicePrintDao).saveProfiles(eq(amIdentity), profilesCaptor.capture());
        List<Map<String, Object>> savedProfiles = profilesCaptor.getValue();
        assertThat(savedProfiles).hasSize(2);
        assertThat(savedProfiles.get(0)).containsEntry("uuid", "UUID2");
        assertThat(savedProfiles.get(1)).doesNotContainEntry("uuid", "UUID1")
                .containsEntry("selectionCounter", 1)
                .containsKey("lastSelectedDate")
                .containsKey("devicePrint");
    }
}
