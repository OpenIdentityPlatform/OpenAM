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

package org.forgerock.openam.rest.router;

import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import java.util.Collections;
import java.util.HashMap;
import org.forgerock.openam.rest.DefaultVersionBehaviour;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class VersionBehaviourConfigListenerTest {

    private VersionBehaviourConfigListener listener;
    private VersionedRouter<?> router;
    private ServiceConfig serviceConfig;
    private ServiceConfigManager mgr;

    @BeforeClass
    public void setUp() throws Exception {
        router = mock(VersionedRouter.class);
        listener = new VersionBehaviourConfigListener(router);
        serviceConfig = mock(ServiceConfig.class);
        mgr = mock(ServiceConfigManager.class);
    }

    @Test
    public void testRegister() throws Exception {
        //given
        when(mgr.getGlobalConfig(null)).thenReturn(serviceConfig);
        HashMap hm = new HashMap();
        hm.put(VersionBehaviourConfigListener.WARNING_BEHAVIOUR_ATTRIBUTE, Collections.singleton("False"));
        hm.put(VersionBehaviourConfigListener.VERSION_BEHAVIOUR_ATTRIBUTE, Collections.singleton("None"));
        when(serviceConfig.getAttributes()).thenReturn(hm);

        when(mgr.addListener(listener)).thenReturn("listener1");

        //when
        listener.register(mgr);

        //then
        verify(router).setVersioning(DefaultVersionBehaviour.NONE);
        verify(router).setHeaderWarningEnabled(false);
    }

    @Test(dependsOnMethods = "testRegister")
    public void testGlobalConfigChangedLatest() throws Exception {
        //given
        HashMap hm = new HashMap();
        hm.put(VersionBehaviourConfigListener.WARNING_BEHAVIOUR_ATTRIBUTE, Collections.singleton("False"));
        hm.put(VersionBehaviourConfigListener.VERSION_BEHAVIOUR_ATTRIBUTE, Collections.singleton("Latest"));
        when(serviceConfig.getAttributes()).thenReturn(hm);

        //when
        listener.globalConfigChanged(null, null, null, null, 1);

        //then
        verify(router).setVersioning(DefaultVersionBehaviour.LATEST);
        verify(router, times(2)).setHeaderWarningEnabled(false); //first time in testRegister
    }

    @Test(dependsOnMethods = "testRegister")
    public void testGlobalConfigChangedOldest() throws Exception {
        //given
        HashMap hm = new HashMap();
        hm.put(VersionBehaviourConfigListener.WARNING_BEHAVIOUR_ATTRIBUTE, Collections.singleton("True"));
        hm.put(VersionBehaviourConfigListener.VERSION_BEHAVIOUR_ATTRIBUTE, Collections.singleton("Oldest"));
        when(serviceConfig.getAttributes()).thenReturn(hm);

        //when
        listener.globalConfigChanged(null, null, null, null, 1);

        //then
        verify(router).setVersioning(DefaultVersionBehaviour.OLDEST);
        verify(router).setHeaderWarningEnabled(true);
    }


    @Test(dependsOnMethods = "testRegister")
    public void testGlobalConfigChangedInvalid() throws Exception {
        //given
        when(serviceConfig.getAttributes()).thenReturn(Collections.singletonMap(
                VersionBehaviourConfigListener.VERSION_BEHAVIOUR_ATTRIBUTE, Collections.singleton("Fred")));

        //when
        listener.globalConfigChanged(null, null, null, null, 1);

        //then
        verifyNoMoreInteractions(router);
    }

}