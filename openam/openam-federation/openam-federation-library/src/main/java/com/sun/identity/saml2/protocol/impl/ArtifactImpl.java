/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ArtifactImpl.java,v 1.2 2008/06/25 05:47:58 qcheng Exp $
 *
 */



package com.sun.identity.saml2.protocol.impl;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.saml2.protocol.Artifact;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;

/**
 * This class implements interface <code>Artifact</code>. It models 
 * type <code>urn:oasis:names:tc:SAML:2.0:artifact-04 Artifact</code>.
 * <p>
 * <pre>
 * SAML_artifact := B64(TypeCode EndpointIndex RemainingArtifact)
 * TypeCode      := Byte1Byte2
 * EndpointIndex := Byte1Byte2
 *
 * TypeCode          := 0x0004
 * RemainingArtifact := SourceID messageHandle
 * SourceID          := 20-byte_sequence
 * MessageHandle     := 20-byte_sequence
 * </pre>
 */
public class ArtifactImpl implements Artifact {

    private String artifact = null;
    private String messageHandle = null;
    private String sourceID = null;
    private byte[] typeCode = null;
    private int endpointIndex = -1;

    final static int ARTIFACT_4_LENGTH = 44;
    final static byte ARTIFACT_4_TYPE_CODE_BYTE1 = 0x00;
    final static byte ARTIFACT_4_TYPE_CODE_BYTE2 = 0x04;
    final static byte[] ARTIFACT_4_TYPE_CODE = {ARTIFACT_4_TYPE_CODE_BYTE1,
						ARTIFACT_4_TYPE_CODE_BYTE2};

    // used by the constructors.
    private void parseElement(Element element)
        throws SAML2Exception {
	// make sure that the input xml block is not null
        if (element == null) {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message("ArtifactImpl.parseElement: "
                    + "Input is null.");
            }
            throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("nullInput"));
        }
        // Make sure this is an Artifact.
        String tag = element.getLocalName();
        if ((tag == null) || (!tag.equals("Artifact"))) {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message("ArtifactImpl.parseElement: "
                    + "not Artifact.");
            }
            throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("wrongInput"));
        }

	// obtain encoded artifact value
	String value = XMLUtils.getElementValue((Element) element);
	parseArtifactValue(value);
    }

    private void parseArtifactValue(String value) throws SAML2Exception {

	String method = "ArtifactImpl.parseArtifactValue: ";
	if (value == null || value.length() == 0) {
	    if (SAML2SDKUtils.debug.messageEnabled()) {
		SAML2SDKUtils.debug.message(method + "empty input.");
	    }
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("nullInput"));
	}

	// decode the artifact
        byte raw[] = null;
        try {
            raw = Base64.decode(value);
        } catch (Exception e) {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message("ArtifactImpl.parseElement: exception "
		    + "occured while decoding artifact:", e);
            }
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("wrongInput"));
        }

        // check if the length is 44bytes
        if (raw.length != ARTIFACT_4_LENGTH) {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message("ArtifactImpl.parseElement: the length"
		    + " is not 44:" + raw.length);
            }
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("wrongInput"));
        }
	// check if the typecode is correct
        if ((raw[0] != ARTIFACT_4_TYPE_CODE_BYTE1) ||
            (raw[1] != ARTIFACT_4_TYPE_CODE_BYTE2)) {
	    if (SAML2SDKUtils.debug.messageEnabled()) {
		SAML2SDKUtils.debug.message("ArtifactImpl.parseElement: wrong "
		    + "typecode.");
	    }
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("wrongInput"));
        }
        typeCode = ARTIFACT_4_TYPE_CODE;

        artifact = value;

        byte[] endpointIndexB = new byte[2];
        // get the sourceID and messageHandle
        byte sBytes[] = new byte[SAML2Constants.ID_LENGTH];
        byte mBytes[] = new byte[SAML2Constants.ID_LENGTH];
        System.arraycopy(raw, 2, endpointIndexB, 0, 2);
        System.arraycopy(raw, 4, sBytes, 0, SAML2Constants.ID_LENGTH);
        System.arraycopy(raw, 24, mBytes, 0, SAML2Constants.ID_LENGTH);

        try {
            sourceID = SAML2SDKUtils.byteArrayToString(sBytes);
            messageHandle = SAML2SDKUtils.byteArrayToString(mBytes);
        } catch (Exception e) {
            SAML2SDKUtils.debug.error("ArtifactImpl.parseElement: encoding "
		+ "exception: ", e);
            sourceID = new String(sBytes);
            messageHandle = new String(mBytes);
        }
	endpointIndex = SAML2SDKUtils.twoBytesToInt(endpointIndexB);
    }

    /**
     * Private constructor.
     */
    private ArtifactImpl() {}

    /**
     * Class constructor with <code>Artifact</code> in
     * <code>Element</code> format.
     */
    public ArtifactImpl(org.w3c.dom.Element element)
        throws com.sun.identity.saml2.common.SAML2Exception {
        parseElement(element);
    }

    /**
     * Constructor with <code>Base64</code> encoded <code>Artifact</code>
     * value.
     *
     * @param encodedArtifactValue the Artifact value.
     * @throws SAML2Exception if it fails to instantiate the object.
     */
    public ArtifactImpl(String encodedArtifactValue)
        throws com.sun.identity.saml2.common.SAML2Exception {
	parseArtifactValue(encodedArtifactValue);
    }

    /**
     * Constructor.
     *
     * @param typecode two byte sequence representing <code>TypeCode</code>.
     * @param endpointindex integer value of <code>EndpointIndex</code>. Its
     *		allowed range is between 0 and 65535.
     * @param sourceid String format of 20-byte sequence. Usually obtained
     *		from taking the SHA-1 hash of the identification URL (called
     *		provider ID).
     * @param messagehandle String format of 20-byte sequence identifying
     *		a message. This value is constructed from a cryptographically
     *		strong random or pseudorandom number sequence.
     * @throws SAML2Exception if it fails to instantiate the object.
     */
    public ArtifactImpl(byte[] typecode,
			int endpointindex,
			String sourceid,
			String messagehandle)
	throws SAML2Exception {
	if (typecode != null) {
	    if (typecode.length != 2 ||
		typecode[0] != ARTIFACT_4_TYPE_CODE_BYTE1 ||
		typecode[1] != ARTIFACT_4_TYPE_CODE_BYTE2)
	    {
		SAML2SDKUtils.debug.error("ArtifactImpl: wrong typecode.");
		throw new SAML2Exception(
		    SAML2SDKUtils.bundle.getString("wrongInput"));
	    }
	}
	typeCode = ARTIFACT_4_TYPE_CODE;

	if (sourceid == null) {
            SAML2SDKUtils.debug.error("ArtifactImpl: null sourceID.");
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("nullInput"));
        }
        sourceID = sourceid;

	if (messagehandle == null) {
	    messageHandle = SAML2SDKUtils.generateMessageHandle();
	} else {
	    messageHandle = messagehandle;
	}

        byte idBytes[] = null;
        byte handleBytes[] = null;
        try {
            idBytes = SAML2SDKUtils.stringToByteArray(sourceID);
            handleBytes = SAML2SDKUtils.stringToByteArray(messageHandle);
        } catch (Exception e) {
            SAML2SDKUtils.debug.error("ArtifactImpl: encoding exception: ",e);
            idBytes = sourceID.getBytes();
            handleBytes = messageHandle.getBytes();
        }
        if (idBytes.length != SAML2Constants.ID_LENGTH ||
            handleBytes.length != SAML2Constants.ID_LENGTH) {
            SAML2SDKUtils.debug.error("ArtifactImpl: wrong input length.");
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("wrongInput"));
        }

        byte[] indexArray = SAML2SDKUtils.intToTwoBytes(endpointindex);
        endpointIndex = endpointindex;

        byte raw[] = new byte[ARTIFACT_4_LENGTH];
        raw[0] = ARTIFACT_4_TYPE_CODE_BYTE1;
        raw[1] = ARTIFACT_4_TYPE_CODE_BYTE2;
        raw[2] = indexArray[0];
        raw[3] = indexArray[1];
        for (int i = 0; i < SAML2Constants.ID_LENGTH; i++) {
            raw[4+i] = idBytes[i];
            raw[24+i] = handleBytes[i];
        }
        try {
            artifact = Base64.encode(raw).trim();
        } catch (Exception e) {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message("ArtifactImpl: exception encode"
                        + " input:", e);
            }
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("errorCreateArtifact"));
        }

    }

    /**
     * Returns the artifact.
     *
     * @return the value of the artifact. It's <code>Base64</code>
     *		encoded.
     */
    public String getArtifactValue() {
	return artifact;
    }

    /**
     * Returns the <code>SourceID</code> of the artifact.
     *
     * @return The <code>SourceID</code> of the artifact.
     */
    public String getSourceID() {
	return sourceID;
    }

    /**
     * Returns the <code>MessageHandle</code> of the artifact.
     *		The result will be decoded.
     *
     * @return The <code>MessageHandle</code> of the artifact.
     */
    public String getMessageHandle() {
	return messageHandle;
    }

    /**
     * Returns the <code>TypeCode</code> of the artifact.
     * @return The byte array of the <code>TypeCode</code> for the artifact.
     */
    public byte[] getTypeCode() {
	return typeCode;
    }

    /**
     * Returns the <code>EndpointIndex</code> of the artifact.
     * @return value of the <code>EndpointIndex</code> for the
     *		artifact.
     */
    public int getEndpointIndex() {
	return endpointIndex;
    }

    /**
     * Returns a String representation of the element.
     *
     * @return A string containing the valid XML for this element.
     *		By default name space name is prepended to the element name.
     * @throws SAML2Exception if the object does not conform to the schema.
     */
    public String toXMLString()
	throws SAML2Exception {
	return this.toXMLString(true, false);
    }

    /**
     * Returns a String representation of the element.
     *
     * @param includeNS Determines whether or not the namespace qualifier is
     *		prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *		within the Element.
     * @return A string containing the valid XML for this element
     * @throws SAML2Exception if the object does not conform to the schema.
     */
    public String toXMLString(boolean includeNS, boolean declareNS)
	throws SAML2Exception {
	if (artifact == null || artifact.trim().length() == 0) {
	    if (SAML2SDKUtils.debug.messageEnabled()) {
		SAML2SDKUtils.debug.message("ArtifactImpl.toXMLString: artifact "
		    + "value is empty");
	    }
	    throw new SAML2Exception(
		SAML2SDKUtils.bundle.getString("missingElementValue"));
	}

	StringBuffer xml = new StringBuffer(200);
        String prefix = "";
        if (includeNS) {
            prefix = SAML2Constants.PROTOCOL_PREFIX;
        }

        String uri = "";
        if (declareNS) {
            uri = SAML2Constants.PROTOCOL_DECLARE_STR;
        }

        xml.append("<").append(prefix).append("Artifact").append(uri).
                append(">").append(artifact).append("</").append(prefix).
                append("Artifact>");
        return xml.toString();
    }
}
