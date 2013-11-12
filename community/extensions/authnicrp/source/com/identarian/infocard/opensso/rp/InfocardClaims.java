/* The contents of this file are subject to the terms
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
 * $Id: InfocardClaims.java,v 1.3 2009/09/26 20:36:07 ppetitsm Exp $
 *
 * Copyright 2008 Sun Microsystems Inc. All Rights Reserved
 * Portions Copyrighted 2008 Patrick Petit Consulting
 */
package com.identarian.infocard.opensso.rp;

import java.util.HashSet;
import java.util.Set;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.util.encoders.Base64;

/**
 * 
 * @author Patrick
 */
public class InfocardClaims {

    /*
     * Well known claims
     */
    public static final String ISIP_CLAIM_SUFFIX = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/";

    public static final String PPID = "privatepersonalidentifier";
	public static final String SURNAME = "surname";
	public static final String GIVEN_NAME = "givenname";
	public static final String EMAIL_ADDRESS = "emailaddress";
	public static final String STREET_ADDRESS = "streetaddress";
	public static final String LOCALITY = "locality";
	public static final String STATE_OR_PROVINCE = "stateorprovince";
	public static final String POSTAL_CODE = "postalcode";
	public static final String COUNTRY = "country";
	public static final String HOME_PHONE = "homephone";
	public static final String OTHER_PHONE = "otherphone";
	public static final String MOBILE_PHONE = "mobilephone";
	public static final String DATE_OF_BIRTH = "dateofbirth";
	public static final String GENDER = "gender";
	public static final String WEB_PAGE = "webpage";
    public static final Set<String> ISIP_CLAIMS = new HashSet<String>() {
        {
            add(PPID);
            add(SURNAME);
            add(GIVEN_NAME);
            add(EMAIL_ADDRESS);
            add(STREET_ADDRESS);
            add(LOCALITY);
            add(STATE_OR_PROVINCE);
            add(POSTAL_CODE);
            add(COUNTRY);
            add(HOME_PHONE);
            add(OTHER_PHONE);
            add(MOBILE_PHONE);
            add(DATE_OF_BIRTH);
            add(GENDER);
            add(WEB_PAGE);
        }
    };

    /*
     * Extended Claim URIs issued from ICF Claim Catalogue approved claims
     *
     * @see https://wiki.informationcard.net/index.php/Claim_Catalog
     */

    /*
     * A claim that the security token is issued according to the requirements
     * of the U.S. federal Identity Credential and Access Management (ICAM)
     * Assurance Level 1 by an identity provider certified to do so.
     */
    public static final String ICF_CLAIM_SUFFIX = "http://schemas.informationcard.net/@ics";
    /*
     * A claim that the security token is issued according to the requirements
     * of the U.S. federal Identity Credential and Access Management (ICAM)
     * Assurance Level 1 by an identity provider certified to do so.
     */
    public static final String ICAM_ASSURANCE_LEVEL1 = "/icam-assurance-level-1/2009-06";

    /*
     * As above but level 2
     */
    public static final String ICAM_ASSURANCE_LEVEL2 = "/icam-assurance-level-2/2009-06";

    /*
     * As above but level 3
     */
    public static final String ICAM_ASSURANCE_LEVEL3 = "/icam-assurance-level-3/2009-06";

    /*
     * True if the subject is 21 or over years of age.
     */
    public static final String AGE_21_OVER = "/age-21-or-over/2008-12" ;

    /*
     * A possibly empty list of the other claims contained in the token that
     * the Identity Provider asserts that it has verified. This enables a token
     * to convey both verified and unverified claims and for the Relying Party
     * to know which of the claims are verified. (The absence of this claim within
     * a token conveys no information about the token and its absence should not
     * be interpreted otherwise; only the presence of this claim conveys information
     * from the Identity Provider about the claims in the token.)
     */
    public static final String VERIFIED_CLAIMS = "/verified-claims/2008-11" ;

    /*
     * The verification method claim provides a URI representing the verification
     * method employed for verifying the verified claims enumerated in the
     * verified-claims/2008-11 claim. The claim value may utilize any of the
     * verification method URIs. Other URI values may also be defined and used.
     *
     * @see https://informationcard.net/wiki/index.php/Claim_Catalog#Verification_Methods
     */
    public static final String VERIFICATION_METHOD = "/verification-method/2008-12";
    
    /*
     * True if the subject is a COPPA-certified adult who has been verified
     * using one of the COPPA-specified methods. Some of these methods are
     * documented in the COPPA Rules.
     *
     * @see http://www.ftc.gov/os/1999/10/64fr59888.pdf
     */
    public static final String COPPA_CERTIFIED_ADULTE = "/coppa-certified-adult/2008-12";

    /*
     * True if the subject is 18 or over years of age.
     */
    public static final String AGE_OVER_18 = "/age-18-or-over/2008-11";

    /**
     * Accessor methods
     */

    public static String getAGE_21_OVER_URI() {
        return ICF_CLAIM_SUFFIX + AGE_21_OVER;
    }

    public static String getAGE_OVER_18_URI() {
        return ICF_CLAIM_SUFFIX + AGE_OVER_18;
    }

    public static String getCOPPA_CERTIFIED_ADULTE_URI() {
        return ICF_CLAIM_SUFFIX + COPPA_CERTIFIED_ADULTE;
    }

    public static String getICAM_ASSURANCE_LEVEL1_URI() {
        return ICF_CLAIM_SUFFIX + ICAM_ASSURANCE_LEVEL1;
    }

    public static String getICAM_ASSURANCE_LEVEL2_URI() {
        return ICF_CLAIM_SUFFIX + ICAM_ASSURANCE_LEVEL2;
    }

    public static String getICAM_ASSURANCE_LEVEL3_URI() {
        return ICF_CLAIM_SUFFIX + ICAM_ASSURANCE_LEVEL3;
    }

    public static String getICF_CLAIM_SUFFIX_URI() {
        return ICF_CLAIM_SUFFIX + ICF_CLAIM_SUFFIX;
    }

    public static String getVERIFICATION_METHOD_URI() {
        return ICF_CLAIM_SUFFIX + VERIFICATION_METHOD;
    }

    public static String getVERIFIED_CLAIMS_URI() {
        return ICF_CLAIM_SUFFIX + VERIFIED_CLAIMS;
    }

     public static String getCOUNTRY_URI() {
        return ISIP_CLAIM_SUFFIX + COUNTRY;
    }

    public static String getDATE_OF_BIRTH_URI() {
        return ISIP_CLAIM_SUFFIX + DATE_OF_BIRTH;
    }

    public static String getEMAIL_ADDRESS_URI() {
        return ISIP_CLAIM_SUFFIX + EMAIL_ADDRESS;
    }

    public static String getGENDER_URI() {
        return ISIP_CLAIM_SUFFIX + GENDER;
    }

    public static String getGIVEN_NAME_URI() {
        return ISIP_CLAIM_SUFFIX + GIVEN_NAME;
    }

    public static String getHOME_PHONE_URI() {
        return ISIP_CLAIM_SUFFIX + HOME_PHONE;
    }

    public static String getLOCALITY_URI() {
        return ISIP_CLAIM_SUFFIX + LOCALITY;
    }

    public static String getMOBILE_PHONE_URI() {
        return ISIP_CLAIM_SUFFIX + MOBILE_PHONE;
    }

    public static String getOTHER_PHONE_URI() {
        return ISIP_CLAIM_SUFFIX + OTHER_PHONE;
    }

    public static String getPOSTAL_CODE_URI() {
        return ISIP_CLAIM_SUFFIX + POSTAL_CODE;
    }

    public static String getPPID_URI() {
        return ISIP_CLAIM_SUFFIX + PPID;
    }

    public static String getSTATE_OR_PROVINCE_URI() {
        return ISIP_CLAIM_SUFFIX + STATE_OR_PROVINCE;
    }

    public static String getSTREET_ADDRESS_URI() {
        return ISIP_CLAIM_SUFFIX + STREET_ADDRESS;
    }

    public static String getSURNAME_URI() {
        return ISIP_CLAIM_SUFFIX + SURNAME;
    }

    public static String getWEB_PAGE_URI() {
        return ISIP_CLAIM_SUFFIX + WEB_PAGE;
    }

    public static String canonicalizeClaimUri(String claimUri) {

        String var = claimUri.substring(7).replace('/', '.');
        return var;
    }
    
    public static String canonicalizeClaimValue(String claimValue) {

        String var = claimValue.substring(1,claimValue.length()-1).replace(',', ' ');
        return var;
    }

    public static String friendlyPPID(String ppid) {
		// code map
		char[] ss = { 'Q', 'L', '2', '3', '4', '5', '6', '7', '8', '9', 'A',
				'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'M', 'N', 'P',
				'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z' };

		// base 64 decoding
		byte[] b = Base64.decode(ppid.getBytes());

		// sha1 decoding
		SHA1Digest digEng = new SHA1Digest();
		digEng.update(b, 0, b.length);
		byte[] b1 = new byte[digEng.getDigestSize()];
		digEng.doFinal(b1, 0);

		// convert the bytes to ints
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < 10; i++) {
			int ii = byte2int(b1[i]) % 32;
			if (i == 3 || i == 7) {
				sb.append("-");
			}
			// mapping of the int to mapping code
			sb.append(ss[ii]);
		}
		return sb.toString();
	}

	static public int byte2int(byte b) {
		return ((b < 0) ? (0x100 + b) : b);
	}
}
