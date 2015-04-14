/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2015 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted 2015 ForgeRock AS"
 */

package org.forgerock.openam.utils.qr;

import org.forgerock.util.Reject;

/**
 * Provides functionality to generate QR codes within the AM UI.
 *
 * @since 13.0.0
 */
public final class GenerationUtils {

    /**
     * Minimum version number to be capable of QR encoding the Authenticator App Registration URI's number of
     * characters at {@link ErrorCorrectionLevel} "LOW".
    */
    private static final int MINIMUM_VERSION_NUMBER_FOR_AUTH_APP = 10;

    private GenerationUtils() {}

    /**
     * This function provides the Javascript required for the UI to generate a QR code and place it on the screen.
     * There must be an element in the DOM with the id "qr" for this to work. This element is where the QR code
     * will appear.
     *
     * @param textToEncode The text to be encoded as a QR code. Must not be null.
     * @param versionNumber The version number of the QR code. Must be in the range 1 - 40.
     *                      This indicates the maximum amount of data storage provided by the QR code.
     *                      The amount of data that can be stored at a certain level varies dependent
     *                      on the errorCorrectionLevel selected. See http://en.wikipedia.org/wiki/QR_code
     *                      § "Storage" for more information.
     * @param errorCorrectionLevel Represents the error correction level provided by a QR code.
     *                             See {@link ErrorCorrectionLevel} for more information.
     *
     * @return The Javascript required for the UI to generate a QR code and place it on the screen.
     */
    public static String getQRCodeGenerationJavascript(String textToEncode, int versionNumber,
                                                       ErrorCorrectionLevel errorCorrectionLevel) {
        Reject.ifNull("textToEncode cannot be null.");

        if (versionNumber < 1 || versionNumber > 40) {
            throw new IllegalArgumentException("versionNumber must be in the range 1 - 40.");
        }

        return "document.getElementById('qr').innerHTML = create_qrcode('" + textToEncode + "', " + versionNumber
                + ", '" + errorCorrectionLevel.getLetterCode() + "')\n";
    }

    /**
     * Convenience method to provide the Javascript required for the UI to generate a QR code and place it on the
     * screen, with the data being an Authenticator App registration URL.
     *
     * @param textToEncode The Authenticator App registration URL.
     *
     * @return The Javascript required for the UI to generate the relevant QR code and place it on the screen.
     */
    public static String getQRCodeGenerationJavascriptForAuthenticatorAppRegistration(String textToEncode) {
        return getQRCodeGenerationJavascript(textToEncode, MINIMUM_VERSION_NUMBER_FOR_AUTH_APP,
                ErrorCorrectionLevel.LOW);
    }

}
