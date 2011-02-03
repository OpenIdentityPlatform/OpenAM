/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: IssuingDistributionPointExtension.java,v 1.2 2008/06/25 05:52:46 qcheng Exp $
 *
 */

package com.iplanet.security.x509;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.*;


import sun.security.util.BitArray;
import sun.security.util.DerInputStream;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;
import sun.security.util.ObjectIdentifier;
import sun.security.x509.AVA;
import sun.security.x509.Extension;
import sun.security.x509.GeneralNames;
import sun.security.x509.PKIXExtensions;
import sun.security.x509.RDN;

/**
 * A critical CRL extension that identifies the CRL distribution point
 * for a particular CRL
 *
 * <pre>
 * issuingDistributionPoint ::= SEQUENCE {
 *         distributionPoint       [0] DistributionPointName OPTIONAL,
 *         onlyContainsUserCerts   [1] BOOLEAN DEFAULT FALSE,
 *         onlyContainsCACerts     [2] BOOLEAN DEFAULT FALSE,
 *         onlySomeReasons         [3] ReasonFlags OPTIONAL,
 *         indirectCRL             [4] BOOLEAN DEFAULT FALSE }
 *
 * DistributionPointName ::= CHOICE {
 *         fullName                [0]     GeneralNames,
 *         nameRelativeToCRLIssuer [1]     RelativeDistinguishedName }
 *
 * ReasonFlags ::= BIT STRING {
 *         unused                  (0),
 *         keyCompromise           (1),
 *         cACompromise            (2),
 *         affiliationChanged      (3),
 *         superseded              (4),
 *         cessationOfOperation    (5),
 *         certificateHold         (6) }
 *
 * GeneralNames ::= SEQUENCE SIZE (1..MAX) OF GeneralName
 *
 * GeneralName ::= CHOICE {
 *         otherName                       [0]     OtherName,
 *         rfc822Name                      [1]     IA5String,
 *         dNSName                         [2]     IA5String,
 *         x400Address                     [3]     ORAddress,
 *         directoryName                   [4]     Name,
 *         ediPartyName                    [5]     EDIPartyName,
 *         uniformResourceIdentifier       [6]     IA5String,
 *         iPAddress                       [7]     OCTET STRING,
 *         registeredID                    [8]     OBJECT IDENTIFIER}
 *
 * OtherName ::= SEQUENCE {
 *         type-id    OBJECT IDENTIFIER,
 *         value      [0] EXPLICIT ANY DEFINED BY type-id }
 *
 * EDIPartyName ::= SEQUENCE {
 *         nameAssigner            [0]     DirectoryString OPTIONAL,
 *         partyName               [1]     DirectoryString }
 *
 * RelativeDistinguishedName ::=
 *         SET OF AttributeTypeAndValue
 *
 * AttributeTypeAndValue ::= SEQUENCE {
 *         type     AttributeType,
 *         value    AttributeValue }
 *
 * AttributeType ::= OBJECT IDENTIFIER
 *
 * AttributeValue ::= ANY DEFINED BY AttributeType
 * </pre>
 */
public class IssuingDistributionPointExtension extends Extension {


    /**
     * Identifier for this attribute, to be used with the
     * get, set, delete methods of Certificate, x509 type.
     */
    public static final String IDENT = 
    				"x509.info.extensions.IssuingDistributionPoint";

    // reason flag bits
    public final static int KEY_COMPROMISE         = 1;
    public final static int CA_COMPROMISE          = 2;
    public final static int AFFILIATION_CHANGED    = 3;
    public final static int SUPERSEDED             = 4;
    public final static int CESSATION_OF_OPERATION = 5;
    public final static int CERTIFICATE_HOLD       = 6;
    
    private static final String[] REASON_STRINGS = {
	null,
	"key compromise",
	"CA compromise",
	"affiliation changed",
	"superseded",
	"cessation of operation",
	"certificate hold"
    };

    /**
     * Attribute name.
     */
    public static final String NAME = "IssuingDistributionPoint";

    // context specific tag values
    private static final byte TAG_DIST_PT = 0;
    private static final byte TAG_ONLY_USER_CERTS = 1;
    private static final byte TAG_ONLY_CA_CERTS = 2;
    private static final byte TAG_REASONS = 3;
    private static final byte TAG_INDIRECT_CRL = 4;

    private static final byte TAG_FULL_NAME = 0;
    private static final byte TAG_REL_NAME = 1;


    // only one of fullName and relativeName can be set
    private GeneralNames fullName = null;
    private RDN relativeName = null;

    private boolean onlyContainsUserCerts = false;
    private boolean onlyContainsCACerts = false;

    // onlySomeReasons or null
    private boolean[] reasonFlags = null;
    
    private boolean indirectCRL = false;

    
    // cached hashCode value
    private volatile int hashCode;


    /**
     * Create a IssuingDistributionPointExtension.
     *
     * @param fullName the GeneralNames of the distribution point; may be null
     * @param onlyContainsUserCerts the 'onlyContainsUserCerts' attribute
     * @param onlyContainsCACerts the 'onlyContainsCACerts' attribute
     * @param reasonFlags the 'reasonFlags' attribute
     * @param indirectCRL the 'indirectCRL' attribute
     * @param critical true if this is a critical extension
     * @throws IOException on error
     */
    public IssuingDistributionPointExtension(GeneralNames fullName,
                                             boolean onlyContainsUserCerts,
                                             boolean onlyContainsCACerts,
                                             boolean[] reasonFlags,
                                             boolean indirectCRL,
                                             boolean critical)
	    throws IOException {
        this.extensionId = PKIXExtensions.IssuingDistributionPoint_Id;
        this.critical = critical;

        this.fullName = fullName;
        this.onlyContainsUserCerts = onlyContainsUserCerts;
        this.onlyContainsCACerts = onlyContainsCACerts;
	this.reasonFlags = reasonFlags;
        this.indirectCRL = indirectCRL;

        encodeThis();
    }

    /**
     * Create a IssuingDistributionPointExtension.
     *
     * @param relativeName the RelativeDistinguishedName of the distribution 
     *        point; may not be null
     * @param onlyContainsUserCerts the 'onlyContainsUserCerts' attribute
     * @param onlyContainsCACerts the 'onlyContainsCACerts' attribute
     * @param reasonFlags the 'reasonFlags' attribute
     * @param indirectCRL the 'indirectCRL' attribute
     * @param critical true if this is a critical extension
     * @throws IOException on error
     */
    public IssuingDistributionPointExtension(RDN relativeName,
                                             boolean onlyContainsUserCerts,
                                             boolean onlyContainsCACerts,
                                             boolean[] reasonFlags,
                                             boolean indirectCRL,
                                             boolean critical)
	    throws IOException {
        this.extensionId = PKIXExtensions.IssuingDistributionPoint_Id;
        this.critical = critical;

        this.relativeName = relativeName;
        this.onlyContainsUserCerts = onlyContainsUserCerts;
        this.onlyContainsCACerts = onlyContainsCACerts;
	this.reasonFlags = reasonFlags;
        this.indirectCRL = indirectCRL;

        encodeThis();
    }


    /**
     * Create the extension from the passed DER encoded value of the same.
     *
     * @param value Array of DER encoded bytes of the actual value.
     * @exception IOException on error.
     */
    public IssuingDistributionPointExtension(Object value)
	    throws IOException {
        this.extensionId = PKIXExtensions.IssuingDistributionPoint_Id;
        this.critical = true;

	if (!(value instanceof byte[])) {
	    throw new IOException("Illegal argument type");
	}
	
	extensionValue = (byte[])value;
        DerValue val = new DerValue(extensionValue);
	if (val.tag != DerValue.tag_Sequence) {
	    throw new IOException("Invalid encoding for " +
				  "IssuingDistributionPointExtension.");
	}

	if (val.data == null || val.data.available() == 0) {
            return;
        }

        DerValue opt = val.data.getDerValue();

        if (opt.isContextSpecific(TAG_DIST_PT) && opt.isConstructed()) {
            DerValue distPnt = opt.data.getDerValue();
            if (distPnt.isContextSpecific(TAG_FULL_NAME)
                && distPnt.isConstructed()) {
                distPnt.resetTag(DerValue.tag_Sequence);
                fullName = new GeneralNames(distPnt);
            } else if (distPnt.isContextSpecific(TAG_REL_NAME) 
                       && distPnt.isConstructed()) {
                distPnt.resetTag(DerValue.tag_Set);
                
                relativeName = new RDN(derValueToAVAs(distPnt));
            } else {
                throw new IOException("Invalid encoding for " +
                                      "IssuingDistributionPointExtension.");
            }


            if (val.data.available() == 0) {
                return;
            }
            opt = val.data.getDerValue();
        }


        if (opt.isContextSpecific(TAG_ONLY_USER_CERTS)) {
            opt.resetTag(DerValue.tag_Boolean);
            onlyContainsUserCerts = opt.getBoolean();

            if (val.data.available() == 0) {
                return;
            }
            opt = val.data.getDerValue();

        }

        if (opt.isContextSpecific(TAG_ONLY_CA_CERTS)) {
            opt.resetTag(DerValue.tag_Boolean);
            onlyContainsCACerts = opt.getBoolean();

            if (onlyContainsUserCerts && onlyContainsCACerts) {
                throw new IOException("onlyContainsUserCerts and " +
                                      "onlyContainsCACerts can't both be true");
            }

            if (val.data.available() == 0) {
                return;
            }
            opt = val.data.getDerValue();
        }

        if (opt.isContextSpecific(TAG_REASONS) && !opt.isConstructed()) {
            opt.resetTag(DerValue.tag_BitString);
            reasonFlags = (opt.getUnalignedBitString()).toBooleanArray();

            if (val.data.available() == 0) {
                return;
            }
            opt = val.data.getDerValue();
        }

        if (opt.isContextSpecific(TAG_INDIRECT_CRL)) {
            opt.resetTag(DerValue.tag_Boolean);
            indirectCRL = opt.getBoolean();

            if (val.data.available() == 0) {
                return;
            }
        }

        throw new IOException("Invalid encoding for " +
                              "IssuingDistributionPointExtension.");
    }

    /**
     * Return the name of this attribute.
     */
    public String getName() {
        return NAME;
    }

    /**
     * Return the full distribution point name or null if not set.
     */
    public GeneralNames getFullName() {
	return fullName;
    }
    
    /**
     * Return the relative distribution point name or null if not set.
     */
    public RDN getRelativeName() {
	return relativeName;
    }
    
    /**
     * Return the onlyContainsUserCerts attribute
     */
    public boolean getOnlyContainsUserCerts() {
	return onlyContainsUserCerts;
    }

    /**
     * Return the onlyContainsCACerts attribute
     */
    public boolean getOnlyContainsCACerts() {
	return onlyContainsCACerts;
    }

    /**
     * Return the reason flags or null if not set.
     */
    public boolean[] getOnlySomeReasons() {
	return reasonFlags;
    }
    
    /**
     * Return the indirectCRL attribute
     */
    public boolean getIndirectCRL() {
	return indirectCRL;
    }


    /**
     * Sets the full distribution point name.
     */
    public void setFullName(GeneralNames fullName) {
        this.fullName = fullName;
        if( fullName != null ) {
            this.relativeName = null;
        }
    }

    /**
     * Sets the relative distribution point name.
     */
    public void setRelativeName(RDN relativeName) {
        this.relativeName = relativeName;
        if( relativeName != null ) {
            this.fullName = null;
        }
    }

    /**
     * Sets the onlyContainsUserCerts attribute.
     */
    public void setOnlyContainsUserCerts(boolean onlyContainsUserCerts) {
        this.onlyContainsUserCerts = onlyContainsUserCerts;
    }

    /**
     * Sets the onlyContainsCACerts attribute.
     */
    public void setOnlyContainsCACerts(boolean onlyContainsCACerts) {
        this.onlyContainsCACerts = onlyContainsCACerts;
    }

    /**
     * Sets the reason flags for this distribution point.
     */
    public void setOnlySomeReasons(boolean[] reasonFlags) {
        this.reasonFlags = reasonFlags;
    }

    /**
     * Sets the indirectCRL attribute.
     */
    public void setIndirectCRL(boolean indirectCRL) {
        this.indirectCRL = indirectCRL;
    }

    /**
     * Write the extension to the DerOutputStream.
     *
     * @param out the DerOutputStream to write the extension to.
     * @exception IOException on encoding errors.
     */
    public void encode(OutputStream out) throws IOException {
        DerOutputStream tmp = new DerOutputStream();
        if (this.extensionValue == null) {
            this.extensionId = PKIXExtensions.IssuingDistributionPoint_Id;
            this.critical = true;
            encodeThis();
        }
        super.encode(tmp);
        out.write(tmp.toByteArray());
    }

     // Encode this extension value
    private void encodeThis() throws IOException {

        if (onlyContainsUserCerts && onlyContainsCACerts) {
            throw new IOException("onlyContainsUserCerts and " +
                                  "onlyContainsCACerts can't both be true");
        }

 	DerOutputStream tagged = new DerOutputStream();

        // NOTE: only one of pointNames and pointRDN can be set
        if ((fullName != null) || (relativeName != null)) {
            DerOutputStream distributionPoint = new DerOutputStream();
            if (fullName != null) {
                DerOutputStream derOut = new DerOutputStream();
                fullName.encode(derOut);
                distributionPoint.writeImplicit(
                    DerValue.createTag(DerValue.TAG_CONTEXT, true, TAG_FULL_NAME),
                    derOut);
            } else if (relativeName != null) {
                DerOutputStream derOut = new DerOutputStream();
                encodeRDN(relativeName, derOut);
                distributionPoint.writeImplicit(
                    DerValue.createTag(DerValue.TAG_CONTEXT, true, TAG_REL_NAME),
                    derOut);
            }
            tagged.write(
                DerValue.createTag(DerValue.TAG_CONTEXT, true, TAG_DIST_PT),
                distributionPoint);
        }

        if (onlyContainsUserCerts) {
            DerOutputStream doOnlyContainsUserCerts = new DerOutputStream();
            doOnlyContainsUserCerts.putBoolean(onlyContainsUserCerts);
            tagged.writeImplicit(DerValue.createTag(DerValue.TAG_CONTEXT, false,
                                                    TAG_ONLY_USER_CERTS),
                                 doOnlyContainsUserCerts);
        }

        if (onlyContainsCACerts) {
            DerOutputStream doOnlyContainsCACerts = new DerOutputStream();
            doOnlyContainsCACerts.putBoolean(onlyContainsCACerts);
            tagged.writeImplicit(DerValue.createTag(DerValue.TAG_CONTEXT, false,
                                                    TAG_ONLY_CA_CERTS),
                                 doOnlyContainsCACerts);
        }
        if (reasonFlags != null) {
	    DerOutputStream reasons = new DerOutputStream();
	    BitArray rf = new BitArray(reasonFlags);
            reasons.putUnalignedBitString(rf);
	    tagged.writeImplicit(
	    	DerValue.createTag(DerValue.TAG_CONTEXT, false, TAG_REASONS), 
		reasons);
	}

        if (indirectCRL) {
            DerOutputStream doIndirectCRL = new DerOutputStream();
            doIndirectCRL.putBoolean(indirectCRL);
            tagged.writeImplicit(DerValue.createTag(DerValue.TAG_CONTEXT, false,
                                                    TAG_INDIRECT_CRL),
                                 doIndirectCRL);
        }
        this.extensionValue = tagged.toByteArray();
    }

    /**
     * Return a string representation for reasonFlag bit 'reason'.
     */
    private static String reasonToString(int reason) {
	if ((reason > 0) && (reason < REASON_STRINGS.length)) {
	    return REASON_STRINGS[reason];
	}
	return "Unknown reason " + reason;
    }

    /**
     * Return the extension as user readable string.
     */
    public String toString() {
	StringBuffer sb = new StringBuffer();
        sb.append(super.toString() + "IssuingDistributionPoint [\n  ");
        if (fullName != null) {
	    sb.append("    fullName:\n    " + fullName + "\n");
	}
        if (relativeName != null) {
            sb.append("    relativeName:\n     " + relativeName + "\n");
	}

        sb.append("    onlyContainsUserCerts:\n     " +
                  onlyContainsUserCerts + "\n");

        sb.append("    onlyContainsCACerts:\n     " +
                  onlyContainsCACerts + "\n");

	if (reasonFlags != null) {
	    sb.append("   ReasonFlags:\n");
	    for (int i = 0; i < reasonFlags.length; i++) {
		if (reasonFlags[i]) {
		    sb.append("    " + reasonToString(i) + "\n");
		}
	    }
	}

        sb.append("    indirectCRL:\n     " +
                  indirectCRL + "\n");


	return sb.toString();
    }

    private static AVA[] derValueToAVAs(DerValue derValue)
        throws IOException {

        DerInputStream dis = new DerInputStream(derValue.toByteArray());
        DerValue[] avaset = dis.getSet(5);

        AVA[] avas = new AVA[avaset.length];
        for (int i = 0; i < avaset.length; i++) {
            DerValue derval = avaset[i];
            avas[i] = new AVA(derval.data.getOID(), derval.data.getDerValue());
        }

        return avas;
    }

    private static void encodeRDN(RDN rdn, DerOutputStream derOut)
        throws IOException {

        List avas = rdn.avas();
        AVA[] avaArray = (AVA[])avas.toArray(new AVA[avas.size()]);
        derOut.putOrderedSetOf(DerValue.tag_Set, avaArray);
    }
}
