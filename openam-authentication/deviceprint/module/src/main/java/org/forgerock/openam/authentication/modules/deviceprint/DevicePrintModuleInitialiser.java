/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */
/*
 * Portions Copyrighted 2013 Syntegrity.
 * Portions Copyrighted 2013 ForgeRock Inc.
 */

package org.forgerock.openam.authentication.modules.deviceprint;

import com.sun.identity.authentication.modules.hotp.HOTPParams;
import com.sun.identity.authentication.modules.hotp.HOTPService;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.AMResourceBundleCache;
import org.forgerock.openam.authentication.modules.deviceprint.comparators.ColocationComparator;
import org.forgerock.openam.authentication.modules.deviceprint.comparators.DevicePrintComparator;
import org.forgerock.openam.authentication.modules.deviceprint.comparators.MultiValueAttributeComparator;
import org.forgerock.openam.authentication.modules.deviceprint.extractors.DevicePrintExtractorFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Helper class to initialise the dependencies for the DevicePrint Auth Module.
 */
public class DevicePrintModuleInitialiser {

    private static final String AUTH_MODULE_NAME = "amAuthDevicePrintModule";
    private static final Debug DEBUG = Debug.getInstance("amAuthDevicePrint");

    private static final String FROM_ADDRESS = "sunAMAuthHOTPSMTPFromAddress";
    private static final String GATEWAY_SMS_IMPL_CLASS = "sunAMAuthHOTPSMSGatewayImplClassName";
    private static final String CODE_VALIDITY_DURATION = "sunAMAuthHOTPPasswordValidityDuration";
    private static final String CODE_LENGTH = "sunAMAuthHOTPPasswordLength";
    private static final String CODE_DELIVERY = "sunAMAuthHOTPasswordDelivery";
    private static final String ATTRIBUTE_PHONE = "openamTelephoneAttribute";
    private static final String ATTRIBUTE_CARRIER = "openamSMSCarrierAttribute";
    private static final String ATTRIBUTE_EMAIL = "openamEmailAttribute";

    /**
     * Constructs an instance of the HOTPService.
     *
     * @param locale The user's locale.
     * @param amCache An instance of the AMResourceBundleCache.
     * @param userName The user's name.
     * @param amIdentityRepo An instance of the AMIdentityRepository.
     * @param options The authentication modules settings.
     * @return The HOTPService instance.
     */
    public HOTPService getHOTPService(Locale locale, AMResourceBundleCache amCache, String userName,
            AMIdentityRepository amIdentityRepo, Map<String, String> options) {

        String gatewaySMSImplClass = CollectionHelper.getMapAttr(options, GATEWAY_SMS_IMPL_CLASS);
        String codeValidityDuration = CollectionHelper.getMapAttr(options, CODE_VALIDITY_DURATION);
        String codeLength = CollectionHelper.getMapAttr(options, CODE_LENGTH);
        String codeDelivery = CollectionHelper.getMapAttr(options, CODE_DELIVERY);

        String telephoneAttribute = CollectionHelper.getMapAttr(options, ATTRIBUTE_PHONE);
        String carrierAttribute = CollectionHelper.getMapAttr(options, ATTRIBUTE_CARRIER);
        String emailAttribute = CollectionHelper.getMapAttr(options, ATTRIBUTE_EMAIL);
        if (DEBUG.messageEnabled()) {
            DEBUG.message("HOTP.init() : " + "telephone attribute=" + telephoneAttribute);
            DEBUG.message("HOTP.init() : " + "carrier attribute=" + carrierAttribute);
            DEBUG.message("HOTP.init() : " + "email attribute=" + emailAttribute);
        }

        ResourceBundle bundle = amCache.getResBundle(AUTH_MODULE_NAME, locale);
        if (DEBUG.messageEnabled()) {
            DEBUG.message("HOTP.init() : " + "HOTP resouce bundle locale=" + locale);
        }

        HOTPParams hotpParams = new HOTPParams(gatewaySMSImplClass, Long.parseLong(codeValidityDuration),
                telephoneAttribute, carrierAttribute, emailAttribute, codeDelivery, options,
                Integer.parseInt(codeLength), bundle.getString("messageSubject"), bundle.getString("messageContent"),
                FROM_ADDRESS);

        return new HOTPService(amIdentityRepo, userName, hotpParams);
    }

    /**
     * Constructs an instance of the DevicePrintAuthenticationService.
     *
     * @param amIdentity The user's AM Identity.
     * @param httpServletRequest The authentication Http Request.
     * @param hotpService An instance of the HOTPService.
     * @param options The authentication modules settings.
     * @return The DevicePrintAuthenticationService instance.
     */
    public DevicePrintAuthenticationService getDevicePrintAuthenticationService(AMIdentity amIdentity,
            HttpServletRequest httpServletRequest, HOTPService hotpService, Map<String, String> options) {

        AMIdentityWrapper amIdentityWrapper = new AMIdentityWrapper(amIdentity);
        UserProfilesDao userProfilesDao = new UserProfilesDao(amIdentityWrapper);
        DevicePrintAuthenticationConfig config = new DevicePrintAuthenticationConfig(options);
        DevicePrintExtractorFactory extractorFactory = new DevicePrintExtractorFactory();
        MultiValueAttributeComparator multiValueAttributeComparator = new MultiValueAttributeComparator();
        ColocationComparator colocationComparator = new ColocationComparator();
        DevicePrintComparator devicePrintComparator = new DevicePrintComparator(multiValueAttributeComparator,
                colocationComparator);
        DevicePrintService devicePrintService = new DevicePrintService(config, userProfilesDao, extractorFactory,
                devicePrintComparator);

        DevicePrintAuthenticationService devicePrintAuthenticationService = new DevicePrintAuthenticationService(
                httpServletRequest, hotpService, devicePrintService, config);

        return devicePrintAuthenticationService;
    }
}
