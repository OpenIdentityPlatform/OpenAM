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
package org.forgerock.openam.services.push;

import static org.forgerock.json.JsonValue.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.mock;

import com.iplanet.sso.SSOException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class PushNotificationServiceTest {

    private PushNotificationServiceConfigHelperFactory mockHelperFactory;
    private PushNotificationServiceConfigHelper mockHelper;
    private PushNotificationServiceConfig config;
    private PushNotificationDelegateFactory mockDelegateFactory;
    private PushNotificationDelegate mockDelegate;
    private PushNotificationDelegate mockOldDelegate;
    private static PushNotificationDelegate mockTestDelegate;

    private Debug mockDebug;
    private PushNotificationService notificationService;

    @BeforeMethod
    public void theSetUp() throws SMSException, SSOException, PushNotificationException { //you need this
        this.mockHelperFactory = mock(PushNotificationServiceConfigHelperFactory.class);
        this.mockHelper = mock(PushNotificationServiceConfigHelper.class);
        this.config = new PushNotificationServiceConfig.Builder()
                .withApiKey("apiKey")
                .withEndpoint("endpoint")
                .withSenderId("senderId")
                .build();
        this.mockDelegateFactory = mock(PushNotificationDelegateFactory.class);
        this.mockDelegate = mock(PushNotificationDelegate.class);
        this.mockOldDelegate = mock(PushNotificationDelegate.class);
        mockTestDelegate = mock(PushNotificationDelegate.class);

        ConcurrentMap<String, PushNotificationDelegate> pushRealmMap = new ConcurrentHashMap<>();
        pushRealmMap.put("realm", mockDelegate);
        pushRealmMap.put("oldRealm", mockOldDelegate);
        ConcurrentMap<String, PushNotificationDelegateFactory> pushFactoryMap = new ConcurrentHashMap<>();
        pushFactoryMap.put("factoryClass", mockDelegateFactory);

        given(mockHelper.getConfig()).willReturn(config);
        given(mockHelperFactory.getConfigHelperFor("realm2")).willReturn(mockHelper);
        given(mockHelperFactory.getConfigHelperFor("realm4")).willThrow(new SMSException("Error reading service"));

        this.mockDebug = mock(Debug.class);
        this.notificationService = new PushNotificationService(mockDebug, mockHelperFactory,
                pushRealmMap, pushFactoryMap);
    }

    @Test
    public void shouldSendMessage() throws PushNotificationException {
        //given
        PushMessage pushMessage = new PushMessage("identity", json(object()), null);

        //when
        notificationService.send(pushMessage, "realm");

        //then
        verify(mockDelegate, times(1)).send(pushMessage);
    }

    @Test
    public void shouldLoadDelegateAndSendMessage() throws PushNotificationException {
        //given
        PushMessage pushMessage = new PushMessage("identity", json(object()), null);
        given(mockHelper.getFactoryClass())
                .willReturn("org.forgerock.openam.services.push.PushNotificationServiceTest$TestDelegateFactory");

        //when
        notificationService.send(pushMessage, "realm2");

        //then
        verify(mockTestDelegate, times(1)).startServices();
        verify(mockTestDelegate, times(1)).send(pushMessage);
        verifyNoMoreInteractions(mockTestDelegate);
    }

    @Test (expectedExceptions = PushNotificationException.class)
    public void shouldFailWhenDelegateCannotLoad() throws PushNotificationException {
        //given
        PushMessage pushMessage = new PushMessage("identity", json(object()), null);
        given(mockHelper.getFactoryClass()).willReturn("invalid factory");

        //when
        notificationService.send(pushMessage, "realm2");

        //then
    }

    @Test (expectedExceptions = PushNotificationException.class)
    public void shouldFailWhenConfigNotFound() throws PushNotificationException {
        //given
        PushMessage pushMessage = new PushMessage("identity", json(object()), null);

        //when
        notificationService.send(pushMessage, "realm4");

        //then
    }

    @Test (expectedExceptions = PushNotificationException.class)
    public void shouldFailWhenDelegateFactoryIsBroken() throws PushNotificationException {
        //given
        PushMessage pushMessage = new PushMessage("identity", json(object()), null);
        given(mockHelper.getFactoryClass())
                .willReturn("org.forgerock.openam.services.push.PushNotificationServiceTest$TestBrokenDelegateFactory");

        //when
        notificationService.send(pushMessage, "realm2");

        //then
    }

    @Test (expectedExceptions = PushNotificationException.class)
    public void shouldFailWhenConfigCannotBeCreated() throws PushNotificationException {
        //given
        PushMessage pushMessage = new PushMessage("identity", json(object()), null);
        given(mockHelper.getFactoryClass())
                .willReturn("org.forgerock.openam.services.push.PushNotificationServiceTest$TestDelegateFactory");
        given(mockHelper.getConfig()).willThrow(new PushNotificationException("Build sanity check failed"));

        //when
        notificationService.send(pushMessage, "realm2");

        //then
    }

    /**
     * TEST IMPLEMENTATIONS.
     */

    public static class TestDelegateFactory implements PushNotificationDelegateFactory {
        @Override
        public PushNotificationDelegate produceDelegateFor(PushNotificationServiceConfig config)
                throws PushNotificationException {
            return mockTestDelegate;
        }
    }

    public static class TestBrokenDelegateFactory implements PushNotificationDelegateFactory {
        @Override
        public PushNotificationDelegate produceDelegateFor(PushNotificationServiceConfig config)
                throws PushNotificationException {
            throw new PushNotificationException("Broken implementation.");
        }
    }

    /**
     * INNER CLASS.
     */

    @Test
    public void shouldKeepExistingDelegate() throws PushNotificationException {
        //given
        given(mockOldDelegate.isRequireNewDelegate(config)).willReturn(false);

        //when
        notificationService.new PushNotificationDelegateUpdater().replaceDelegate("oldRealm", mockDelegate, config);

        //then
        verify(mockOldDelegate, times(1)).isRequireNewDelegate(config);
        verify(mockOldDelegate, times(1)).updateDelegate(config);
        verifyNoMoreInteractions(mockOldDelegate);
    }

    @Test
    public void shouldCloseAndReplaceOldDelegate() throws PushNotificationException, IOException {
        //given
        given(mockOldDelegate.isRequireNewDelegate(config)).willReturn(true);

        //when
        notificationService.new PushNotificationDelegateUpdater().replaceDelegate("oldRealm", mockDelegate, config);

        //then
        verify(mockOldDelegate, times(1)).isRequireNewDelegate(config);
        verify(mockOldDelegate, times(1)).close();
        verify(mockDelegate, times(1)).startServices();
        verifyNoMoreInteractions(mockOldDelegate);
    }

}
