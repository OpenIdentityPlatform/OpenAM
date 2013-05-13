/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 ForgeRock, Inc. All Rights Reserved
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
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Portions Copyrighted [2010-2012] [ForgeRock AS]
 *
 */

package com.sun.identity.sm.model;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import com.iplanet.dpro.session.service.SessionService;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;

import com.sun.identity.sm.ldap.CTSPersistentStore;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.forgerock.i18n.LocalizableMessage;
import com.iplanet.dpro.session.exceptions.StoreException;
import com.sun.identity.shared.encode.Base64;
import org.forgerock.openam.session.ha.i18n.AmsessionstoreMessages;
import org.forgerock.openam.session.model.AMRootEntity;
import org.opends.server.protocols.ldap.LDAPAttribute;
import org.opends.server.types.RawAttribute;

/**
 * This class encapsulates a distinguished name and its attribute values.
 *
 * @author steve
 */
public class AMRecordDataEntry {

    /**
     * Debug Logging
     */
    private static Debug DEBUG = SessionService.sessionDebug;

    /**
     * AMRecordDataEntry Object Properties
     */
    private String dn;
    private Map<String, Set<String>> attributeValues;
    private AMRootEntity record;

    /**
     * Static LDAP Construct definitions.
     */
    public final static String AUX_DATA = "auxData";
    public final static String DATA = "data";
    public final static String SERIALIZED_INTERNAL_SESSION_BLOB = "serializedInternalSessionBlob";
    public final static String EXTRA_BYTE_ATTR = "extraByteAttr";
    public final static String EXTRA_STRING_ATTR = "extraStringAttr";
    public final static String OPERATION = "operation";
    public final static String SEC_KEY = "sKey";
    public final static String SERVICE = "service";
    public final static String PRI_KEY = "pKey";
    public final static String EXP_DATE = "expirationDate";
    public final static String STATE = "state";

    private static SimpleDateFormat formatter = null;
    public static List<LDAPAttribute> objectClasses;

    static {
        initialize();
    }

    private static void initialize() {
        List<String> valueList = new ArrayList<String>();
        valueList.add(Constants.TOP);
        valueList.add(CTSPersistentStore.FR_FAMRECORD);
        LDAPAttribute ldapAttr = new LDAPAttribute(Constants.OBJECTCLASS, valueList);
        objectClasses = new ArrayList<LDAPAttribute>();
        objectClasses.add(ldapAttr);

        formatter = new SimpleDateFormat("yyyyMMddHHmmss'Z'");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /**
     * Constructs an instance.
     *
     * @param dn              Distinguished name.
     * @param attributeValues attribute values.
     */
    public AMRecordDataEntry(String dn, Map<String, Set<String>> attributeValues)
            throws StoreException {
        this(dn, null, attributeValues);
    }

    /**
     * Constructs an instance obtained from the Directory Store
     * and UnMarshal as required.
     *
     * ** For SAML2 Keys, Primary and Secondary, these need to
     * be decoded from Hexadecimal to their Native String Values,
     * which can be Base64 Encoded Data handled upstream.
     *
     * @param dn              Distinguished name.
     * @param attributeValues attribute values.
     */
    public AMRecordDataEntry(String dn, String op, Map<String, Set<String>> attributeValues)
            throws StoreException {
        this.dn = dn;
        this.attributeValues = attributeValues;
        parseAttributeValues(attributeValues);
        this.record = new AMRecord();

        if (attributeValues.get(AUX_DATA) != null) {
            Set<String> values = attributeValues.get(AUX_DATA);
            for (String value : values) {
                record.setAuxData(value);
            }
        }

        if (attributeValues.get(DATA) != null) {
            Set<String> values = attributeValues.get(DATA);
            for (String value : values) {
                record.setData(value);
            }
        }

        if (attributeValues.get(SERIALIZED_INTERNAL_SESSION_BLOB) != null) {
            Set<String> values = attributeValues.get(SERIALIZED_INTERNAL_SESSION_BLOB);
            for (String value : values) {
                record.setSerializedInternalSessionBlob(Base64.decode(value));
            }
        }

        if (attributeValues.get(SERVICE) != null) {
            Set<String> values = attributeValues.get(SERVICE);
            for (String value : values) {
                record.setService(value);
            }
        }

        if (attributeValues.get(EXP_DATE) != null) {
            Set<String> values = attributeValues.get(EXP_DATE);
            for (String value : values) {
                record.setExpDate(toAMDateFormat(value));
            }
        }

        if (attributeValues.get(PRI_KEY) != null) {
            Set<String> values = attributeValues.get(PRI_KEY);
            for (String value : values) {
                if (record.getService().equalsIgnoreCase(CTSPersistentStore.SAML2)) {
                    record.setPrimaryKey(decodeKey(value));
                } else {
                    record.setPrimaryKey(value);
                }
            } // End of For Each Loop.
        }

        if (attributeValues.get(SEC_KEY) != null) {
            Set<String> values = attributeValues.get(SEC_KEY);
            for (String value : values) {
                if (record.getService().equalsIgnoreCase(CTSPersistentStore.SAML2)) {
                    record.setSecondaryKey(decodeKey(value));
                } else {
                    record.setSecondaryKey(value);
                }
            } // End of For Each Loop.
        }

        if (op != null) {
            record.setOperation(op);
        } else {
            if (attributeValues.get(OPERATION) != null) {
                Set<String> values = attributeValues.get(OPERATION);
                for (String value : values) {
                    record.setOperation(value);
                }
            }
        }

        if (attributeValues.get(STATE) != null) {
            Set<String> values = attributeValues.get(STATE);
            for (String value : values) {
                record.setState(Integer.parseInt(value));
            }
        }

        if (attributeValues.get(EXTRA_BYTE_ATTR) != null) {
            Set<String> values = attributeValues.get(EXTRA_BYTE_ATTR);
            for (String value : values) {
                String key, v;

                if (value.indexOf('=') == -1) {
                    key = v = value;
                    final LocalizableMessage message = AmsessionstoreMessages.EXTRA_ATTRIBUTE_NO_KEY_VALUE_SEPARATOR.get(value, EXTRA_BYTE_ATTR);
                    DEBUG.warning(this.getClass().getSimpleName() + message.toString());
                } else {
                    key = value.substring(0, value.indexOf('='));
                    v = value.substring(value.indexOf('=') + 1);
                }

                ((AMRecord) record).setExtraByteAttrs(key, v);
            }
        }

        if (attributeValues.get(EXTRA_STRING_ATTR) != null) {
            Set<String> values = attributeValues.get(EXTRA_STRING_ATTR);
            for (String value : values) {
                String key, v;

                if (value.indexOf('=') == -1) {
                    key = v = value;
                    final LocalizableMessage message = AmsessionstoreMessages.EXTRA_ATTRIBUTE_NO_KEY_VALUE_SEPARATOR.get(value, EXTRA_STRING_ATTR);
                    DEBUG.warning(this.getClass().getSimpleName()+message.toString());
                } else {
                    key = value.substring(0, value.indexOf('='));
                    v = value.substring(value.indexOf('=') + 1);
                }

                ((AMRecord) record).setExtraStringAttrs(key, v);
            }
        }
    }

    /**
     * Provides a Marshaling Functionality from a Java POJO
     * to an LDAP Capable and ready Object. Basically, setting up the
     * Object to be serialized to the Directory Store.
     *
     * ** For SAML2 Objects their respective Keys, Primary
     * and Secondary have already been encoded by our @see CTSPersistentStore.
     *
     * @param record
     * @throws StoreException
     */
    public AMRecordDataEntry(AMRootEntity record)
            throws StoreException {
        this.record = record;
        this.attributeValues = new HashMap<String, Set<String>>();
        Set<String> set = new HashSet<String>();

        if (record.getAuxData() != null) {
            set.add(record.getAuxData());
            attributeValues.put(AUX_DATA, set);
        }

        if (record.getData() != null) {
            set = new HashSet<String>();
            set.add(record.getData());
            attributeValues.put(DATA, set);
        }

        if (record.getSerializedInternalSessionBlob() != null) {
            set = new HashSet<String>();
            set.add(Base64.encode(record.getSerializedInternalSessionBlob()));
            attributeValues.put(SERIALIZED_INTERNAL_SESSION_BLOB, set);
        }

        if (record.getPrimaryKey() != null) {
            set = new HashSet<String>();
            set.add(record.getPrimaryKey());
            attributeValues.put(PRI_KEY, set);
        }

        set = new HashSet<String>();
        set.add(toDJDateFormat(record.getExpDate()));
        attributeValues.put(EXP_DATE, set);

        if (record.getService() != null) {
            set = new HashSet<String>();
            set.add(record.getService());
            attributeValues.put(SERVICE, set);
        }

        if (record.getSecondaryKey() != null) {
            set = new HashSet<String>();
            set.add(record.getSecondaryKey());
            attributeValues.put(SEC_KEY, set);
        }

        if (record.getOperation() != null) {
            set = new HashSet<String>();
            set.add(record.getOperation());
            attributeValues.put(OPERATION, set);
        }

        set = new HashSet<String>();
        set.add(Integer.toString(record.getState()));
        attributeValues.put(STATE, set);

        if (record instanceof AMRecord) {
            attributeValues.put(EXTRA_BYTE_ATTR, formatMultiValuedAttr(EXTRA_BYTE_ATTR, ((AMRecord) record).getExtraByteAttributes()));
            attributeValues.put(EXTRA_STRING_ATTR, formatMultiValuedAttr(EXTRA_STRING_ATTR, ((AMRecord) record).getExtraStringAttributes()));
        }

        if (record instanceof FAMRecord) {
            attributeValues.put(EXTRA_BYTE_ATTR, formatMultiValuedAttr(EXTRA_BYTE_ATTR, ((FAMRecord) record).getExtraByteAttributes()));
            attributeValues.put(EXTRA_STRING_ATTR, formatMultiValuedAttr(EXTRA_STRING_ATTR, ((FAMRecord) record).getExtraStringAttributes()));
        }
    }

    /**
     * Return the Associated AMRootEntity Object
     * serialized Session Data.
     *
     * @return AMRootEntity
     */
    public AMRootEntity getAMRecord() {
        return record;
    }

    /**
     * Private Helper Method for Multivalued Attributes.
     *
     * @param attr
     * @param values
     * @return Set<String>
     */
    private Set<String> formatMultiValuedAttr(String attr, Map<String, String> values) {
        Set<String> attrValues = new HashSet<String>();

        if (values == null) {
            return null;
        }

        for (Map.Entry<String, String> value : values.entrySet()) {
            StringBuilder v = new StringBuilder();
            v.append(attr).append(Constants.EQUALS).append(value.getKey());
            v.append(Constants.EQUALS).append(value.getValue());
            attrValues.add(v.toString());
        }

        return attrValues;
    }

    private void parseAttributeValues(Map<String, Set<String>> raw) {
        parseAttributeValues(raw.get(EXTRA_BYTE_ATTR));
        parseAttributeValues(raw.get(EXTRA_STRING_ATTR));
    }

    private void parseAttributeValues(Set<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return;
        }

        for (String s : raw) {
            int idx = s.indexOf('=');

            if (idx != -1) {
                String name = s.substring(0, idx);
                String value = s.substring(idx + 1);

                Set<String> set = attributeValues.get(name);

                if (set == null) {
                    set = new HashSet<String>();
                    attributeValues.put(name, set);
                }

                set.add(value);
            }
        }
    }

    public String getDN() {
        return dn;
    }

    public Set<String> getAttributeValues(String attributeName) {
        return attributeValues.get(attributeName);
    }

    public String getAttributeValue(String attributeName) {
        Set<String> val = attributeValues.get(attributeName);

        return ((val != null) && !val.isEmpty()) ? val.iterator().next() : null;
    }

    public List<RawAttribute> getAttrList() {
        List<RawAttribute> attrList =
                new ArrayList<RawAttribute>(attributeValues.size());

        // Set up all Attributes
        for (Map.Entry<String, Set<String>> entry : attributeValues.entrySet()) {
            Set<String> values = entry.getValue();

            if (values != null && !values.isEmpty()) {
                List<String> valueList = new ArrayList<String>();
                valueList.addAll(values);
                attrList.add(new LDAPAttribute(entry.getKey(), valueList));
            }
        }

        return attrList;
    }

    public static List<LDAPAttribute> getObjectClasses() {
        return objectClasses;
    }

    /**
     * OpenDJ generalizedtime format is yyyyMMddHHmmss'Z'
     * OpenAM session failover format is in seconds and uses the same epoch
     * start as System.currentTimeMillis() / 1000 + the session time in seconds
     *
     * @param date
     * @return
     */
    public static Long toAMDateFormat(String date)
            throws StoreException {
        Date expDate = null;

        try {
            expDate = formatter.parse(date);
        } catch (ParseException pe) {
            final LocalizableMessage message = AmsessionstoreMessages.DB_DJ_PARSE.get(date);
            DEBUG.error(message.toString());
            throw new StoreException(message.toString());
        }

        return expDate.getTime() / 1000;
    }

    /**
     * Helper Date Method to get formatted Date.
     * @param date
     * @return
     */
    public static String toDJDateFormat(Long date) {
        Date expDate = new Date(date.longValue() * 1000L);
        return formatter.format(expDate);
    }

    /**
     * Helper Method to Encode our Primary Key, to be accepted by LDAP.
     *
     * @param primaryKey
     * @return String - Hexadecimal Encoded String.
     */
    public static String encodeKey(final String primaryKey) {
        if ((primaryKey == null) || (primaryKey.isEmpty())) {
            return null;
        }
        try {
            return Hex.encodeHexString(primaryKey.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException uee) {
            DEBUG.error("Unsupported Encoding for Key, " + uee.getMessage() + ", returning null.", uee);
        }
        return null;
    }

    /**
     *  Helper Method to Decode our Primary Key.
     *
     * @param hexadecimalEncodedPrimaryKey
     * @return String - Returned as Original Decoded Key.
     */
    public static String decodeKey(final String hexadecimalEncodedPrimaryKey) {
        if ( (hexadecimalEncodedPrimaryKey == null) || (hexadecimalEncodedPrimaryKey.isEmpty()) )
        { return null; }
        try {
            return new String(Hex.decodeHex(hexadecimalEncodedPrimaryKey.toCharArray()));
        } catch (DecoderException de) {
            DEBUG.error("Decoding Exception for Key, " + de.getMessage() + ", returning null.", de);
        }
        return null;
    }

}
