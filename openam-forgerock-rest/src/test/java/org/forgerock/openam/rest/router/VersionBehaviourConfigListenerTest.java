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
import org.forgerock.openam.rest.DefaultVersionBehaviour;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Collections;

import static org.mockito.Mockito.*;

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
        when(serviceConfig.getAttributes()).thenReturn(Collections.singletonMap(
                VersionBehaviourConfigListener.VERSION_BEHAVIOUR_ATTRIBUTE, Collections.singleton("NONE")));
        when(mgr.addListener(listener)).thenReturn("listener1");

        //when
        listener.register(mgr);

        //then
        verify(router).setVersioning(DefaultVersionBehaviour.NONE);
    }

    @Test(dependsOnMethods = "testRegister")
    public void testGlobalConfigChangedLatest() throws Exception {
        //given
        when(serviceConfig.getAttributes()).thenReturn(Collections.singletonMap(
                VersionBehaviourConfigListener.VERSION_BEHAVIOUR_ATTRIBUTE, Collections.singleton("LATEST")));

        //when
        listener.globalConfigChanged(null, null, null, null, 1);

        //then
        verify(router).setVersioning(DefaultVersionBehaviour.LATEST);
    }

    @Test(dependsOnMethods = "testRegister")
    public void testGlobalConfigChangedOldest() throws Exception {
        //given
        when(serviceConfig.getAttributes()).thenReturn(Collections.singletonMap(
                VersionBehaviourConfigListener.VERSION_BEHAVIOUR_ATTRIBUTE, Collections.singleton("OLDEST")));

        //when
        listener.globalConfigChanged(null, null, null, null, 1);

        //then
        verify(router).setVersioning(DefaultVersionBehaviour.OLDEST);
    }


    @Test(dependsOnMethods = "testRegister")
    public void testGlobalConfigChangedInvalid() throws Exception {
        //given
        when(serviceConfig.getAttributes()).thenReturn(Collections.singletonMap(
                VersionBehaviourConfigListener.VERSION_BEHAVIOUR_ATTRIBUTE, Collections.singleton("FRED")));

        //when
        listener.globalConfigChanged(null, null, null, null, 1);

        //then
        verifyNoMoreInteractions(router);
    }

}