/**
 * ALSISession.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.aspace.ftress.interfaces.ftress.DTO;

public class ALSISession  implements java.io.Serializable {
    private com.aspace.ftress.interfaces.ftress.DTO.AuthenticationOccurrence[] authenticationOccurrences;

    private com.aspace.ftress.interfaces.ftress.DTO.GroupCode groupCode;

    private java.util.Calendar lastUsed;

    private com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode[] uniqueAuthenticationTypes;

    private com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode;

    private java.util.Calendar validTo;

    public ALSISession() {
    }

    public ALSISession(
           com.aspace.ftress.interfaces.ftress.DTO.AuthenticationOccurrence[] authenticationOccurrences,
           com.aspace.ftress.interfaces.ftress.DTO.GroupCode groupCode,
           java.util.Calendar lastUsed,
           com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode[] uniqueAuthenticationTypes,
           com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode,
           java.util.Calendar validTo) {
           this.authenticationOccurrences = authenticationOccurrences;
           this.groupCode = groupCode;
           this.lastUsed = lastUsed;
           this.uniqueAuthenticationTypes = uniqueAuthenticationTypes;
           this.userCode = userCode;
           this.validTo = validTo;
    }


    /**
     * Gets the authenticationOccurrences value for this ALSISession.
     * 
     * @return authenticationOccurrences
     */
    public com.aspace.ftress.interfaces.ftress.DTO.AuthenticationOccurrence[] getAuthenticationOccurrences() {
        return authenticationOccurrences;
    }


    /**
     * Sets the authenticationOccurrences value for this ALSISession.
     * 
     * @param authenticationOccurrences
     */
    public void setAuthenticationOccurrences(com.aspace.ftress.interfaces.ftress.DTO.AuthenticationOccurrence[] authenticationOccurrences) {
        this.authenticationOccurrences = authenticationOccurrences;
    }


    /**
     * Gets the groupCode value for this ALSISession.
     * 
     * @return groupCode
     */
    public com.aspace.ftress.interfaces.ftress.DTO.GroupCode getGroupCode() {
        return groupCode;
    }


    /**
     * Sets the groupCode value for this ALSISession.
     * 
     * @param groupCode
     */
    public void setGroupCode(com.aspace.ftress.interfaces.ftress.DTO.GroupCode groupCode) {
        this.groupCode = groupCode;
    }


    /**
     * Gets the lastUsed value for this ALSISession.
     * 
     * @return lastUsed
     */
    public java.util.Calendar getLastUsed() {
        return lastUsed;
    }


    /**
     * Sets the lastUsed value for this ALSISession.
     * 
     * @param lastUsed
     */
    public void setLastUsed(java.util.Calendar lastUsed) {
        this.lastUsed = lastUsed;
    }


    /**
     * Gets the uniqueAuthenticationTypes value for this ALSISession.
     * 
     * @return uniqueAuthenticationTypes
     */
    public com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode[] getUniqueAuthenticationTypes() {
        return uniqueAuthenticationTypes;
    }


    /**
     * Sets the uniqueAuthenticationTypes value for this ALSISession.
     * 
     * @param uniqueAuthenticationTypes
     */
    public void setUniqueAuthenticationTypes(com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode[] uniqueAuthenticationTypes) {
        this.uniqueAuthenticationTypes = uniqueAuthenticationTypes;
    }


    /**
     * Gets the userCode value for this ALSISession.
     * 
     * @return userCode
     */
    public com.aspace.ftress.interfaces.ftress.DTO.UserCode getUserCode() {
        return userCode;
    }


    /**
     * Sets the userCode value for this ALSISession.
     * 
     * @param userCode
     */
    public void setUserCode(com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode) {
        this.userCode = userCode;
    }


    /**
     * Gets the validTo value for this ALSISession.
     * 
     * @return validTo
     */
    public java.util.Calendar getValidTo() {
        return validTo;
    }


    /**
     * Sets the validTo value for this ALSISession.
     * 
     * @param validTo
     */
    public void setValidTo(java.util.Calendar validTo) {
        this.validTo = validTo;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof ALSISession)) return false;
        ALSISession other = (ALSISession) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.authenticationOccurrences==null && other.getAuthenticationOccurrences()==null) || 
             (this.authenticationOccurrences!=null &&
              java.util.Arrays.equals(this.authenticationOccurrences, other.getAuthenticationOccurrences()))) &&
            ((this.groupCode==null && other.getGroupCode()==null) || 
             (this.groupCode!=null &&
              this.groupCode.equals(other.getGroupCode()))) &&
            ((this.lastUsed==null && other.getLastUsed()==null) || 
             (this.lastUsed!=null &&
              this.lastUsed.equals(other.getLastUsed()))) &&
            ((this.uniqueAuthenticationTypes==null && other.getUniqueAuthenticationTypes()==null) || 
             (this.uniqueAuthenticationTypes!=null &&
              java.util.Arrays.equals(this.uniqueAuthenticationTypes, other.getUniqueAuthenticationTypes()))) &&
            ((this.userCode==null && other.getUserCode()==null) || 
             (this.userCode!=null &&
              this.userCode.equals(other.getUserCode()))) &&
            ((this.validTo==null && other.getValidTo()==null) || 
             (this.validTo!=null &&
              this.validTo.equals(other.getValidTo())));
        __equalsCalc = null;
        return _equals;
    }

    private boolean __hashCodeCalc = false;
    public synchronized int hashCode() {
        if (__hashCodeCalc) {
            return 0;
        }
        __hashCodeCalc = true;
        int _hashCode = 1;
        if (getAuthenticationOccurrences() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getAuthenticationOccurrences());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getAuthenticationOccurrences(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getGroupCode() != null) {
            _hashCode += getGroupCode().hashCode();
        }
        if (getLastUsed() != null) {
            _hashCode += getLastUsed().hashCode();
        }
        if (getUniqueAuthenticationTypes() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getUniqueAuthenticationTypes());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getUniqueAuthenticationTypes(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getUserCode() != null) {
            _hashCode += getUserCode().hashCode();
        }
        if (getValidTo() != null) {
            _hashCode += getValidTo().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(ALSISession.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSISession"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("authenticationOccurrences");
        elemField.setXmlName(new javax.xml.namespace.QName("", "authenticationOccurrences"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationOccurrence"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("groupCode");
        elemField.setXmlName(new javax.xml.namespace.QName("", "groupCode"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "GroupCode"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("lastUsed");
        elemField.setXmlName(new javax.xml.namespace.QName("", "lastUsed"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "dateTime"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("uniqueAuthenticationTypes");
        elemField.setXmlName(new javax.xml.namespace.QName("", "uniqueAuthenticationTypes"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTypeCode"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("userCode");
        elemField.setXmlName(new javax.xml.namespace.QName("", "userCode"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("validTo");
        elemField.setXmlName(new javax.xml.namespace.QName("", "validTo"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "dateTime"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
    }

    /**
     * Return type metadata object
     */
    public static org.apache.axis.description.TypeDesc getTypeDesc() {
        return typeDesc;
    }

    /**
     * Get Custom Serializer
     */
    public static org.apache.axis.encoding.Serializer getSerializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanSerializer(
            _javaType, _xmlType, typeDesc);
    }

    /**
     * Get Custom Deserializer
     */
    public static org.apache.axis.encoding.Deserializer getDeserializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanDeserializer(
            _javaType, _xmlType, typeDesc);
    }

}
