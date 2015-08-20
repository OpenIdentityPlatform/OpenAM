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
* Copyright 2015 ForgeRock AS.
*/

package org.forgerock.openam.authentication.modules.fr.oath;

import static org.forgerock.openam.authentication.modules.fr.oath.validators.CodeLengthValidator.*;

import com.sun.identity.idm.AMIdentity;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Hex;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.Reject;

/**
 * Provides functionality to generate URIs for registering OATH devices with an OATH authentication module.
 *
 * @since 13.0.0
 */
public class AuthenticatorAppRegistrationURIBuilder {

    private final AMIdentity id;
    private final String secretHex;
    private final int codeLength;
    private final String issuer;

    /**
     * Construct a builder which can be used to generate URIs for registering OATH devices with an OATH
     * authentication module.
     *
     * @param id The AMIdentity to be referred to in the URI. Must not be null.
     * @param secretHex The shared secret, in hex, which is to be shared between the OATH authentication
     *                  module and the OATH device. Must not be null or an empty string.
     * @param codeLength The length of the OTP. Must be set to 6 or 8, as per Authenticator app.
     * @param issuer Reference to the user's name. Must not be blank or null.
     */
    public AuthenticatorAppRegistrationURIBuilder(AMIdentity id, String secretHex, int codeLength, String issuer) {
        Reject.ifNull(id, "id cannot be null");
        Reject.ifNull(secretHex, "secretHex cannot be null");
        Reject.ifTrue(StringUtils.isBlank(issuer), "issuer cannot be empty");
        Reject.ifTrue((codeLength < MIN_CODE_LENGTH), "code length must be " + MIN_CODE_LENGTH + " or greater");
        if (secretHex.length() == 0) {
            throw new IllegalArgumentException("secretHex cannot be an empty String.");
        }

        this.issuer = issuer;
        this.id = id;
        this.secretHex = secretHex;
        this.codeLength = codeLength;
    }

    /**
     * Obtain a URI for registering an OATH device with an OATH authentication module.
     *
     * @param counter The initial value of the counter used with the HOTP algorithm.
     *
     * @return The URI.
     *
     * @throws DecoderException If it was not possible to decode the secretHex provided on builder construction,
     * from hex to plain text.
     */
    public String getAuthenticatorAppRegistrationUriForHOTP(int counter) throws DecoderException {
        String appRegistrationUri = getAppRegistrationUri(OTPType.HOTP);
        appRegistrationUri += "&counter=" + counter;
        return appRegistrationUri;
    }

    /**
     * Obtain a URI for registering an OATH device with an OATH authentication module.
     *
     * @param period The period of time in seconds during which a code generated using the TOTP algorithm
     *               will be valid.
     *
     * @return The URI.
     *
     * @throws DecoderException If it was not possible to decode the secretHex provided on builder construction,
     * from hex to plain text.
     */
    public String getAuthenticatorAppRegistrationUriForTOTP(int period) throws DecoderException {
        String appRegistrationUri = getAppRegistrationUri(OTPType.TOTP);
        appRegistrationUri += "&period=" + period;
        return appRegistrationUri;
    }

    private String getAppRegistrationUri(OTPType otpType) throws DecoderException {
        String appRegistrationUri;

        byte[] secretPlainTextBytes = Hex.decodeHex(secretHex.toCharArray());

        Base32 base32 = new Base32();
        String secretBase32 = new String(base32.encode(secretPlainTextBytes));

        String userName = id.getName();
        String realm = extractHumanReadableRealmString(id.getRealm());

        appRegistrationUri = "otpauth://" + otpType.getIdentifier() + "/" + issuer + ":" + realm + userName +
                "?secret=" + secretBase32 + "&issuer=" + issuer + "&digits=" + codeLength;

        return appRegistrationUri;
    }

    /**
     * A less complicated String detailing just the realm information. This is both more human-friendly,
     * and also fits better on a mobile app's screen.
     *
     * @param realm The full realm from the {@link AMIdentity}.
     * @return The extracted and re-rendered realm information.
     */
    private String extractHumanReadableRealmString(String realm) {
        if (realm == null || !realm.contains("o=")) {
            return "";
        }

        List<String> tokens = new ArrayList<String>();
        StringTokenizer tokenizer = new StringTokenizer(realm, ",");
        while (tokenizer.hasMoreTokens()) {
            tokens.add(tokenizer.nextToken());
        }

        String extractedRealmString = "";
        for (String token : tokens) {
            if (token.contains("o=")) {
                extractedRealmString += "/" + token.substring(token.indexOf("=") + 1);
            }
        }
        extractedRealmString += "/";

        return extractedRealmString;
    }

    private enum OTPType {
        HOTP("hotp"), TOTP("totp");

        private final String identifier;

        OTPType(String identifier) {
            this.identifier = identifier;
        }

        public String getIdentifier() {
            return identifier;
        }
    }

}
