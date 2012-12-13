/**
 * UPAuthenticatorClone.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.aspace.ftress.interfaces.ftress.DTO;

public class UPAuthenticatorClone  extends com.aspace.ftress.interfaces.ftress.DTO.UPAuthenticator  implements java.io.Serializable {
    private com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode originalAuthenticationTypeCode;

    public UPAuthenticatorClone() {
    }

    public UPAuthenticatorClone(
           com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationTypeCode,
           com.aspace.ftress.interfaces.ftress.DTO.ChannelsBlocked channelsBlocked,
           com.aspace.ftress.interfaces.ftress.DTO.AuthenticationStatistics statistics,
           java.lang.String status,
           com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode,
           java.util.Calendar validFrom,
           java.util.Calendar validTo,
           int expiryThreshold,
           java.lang.String password,
           int passwordResetStatus,
           java.util.Calendar passwordResetStatusDate,
           java.lang.String username,
           com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode originalAuthenticationTypeCode) {
        super(
            authenticationTypeCode,
            channelsBlocked,
            statistics,
            status,
            userCode,
            validFrom,
            validTo,
            expiryThreshold,
            password,
            passwordResetStatus,
            passwordResetStatusDate,
            username);
        this.originalAuthenticationTypeCode = originalAuthenticationTypeCode;
    }


    /**
     * Gets the originalAuthenticationTypeCode value for this UPAuthenticatorClone.
     * 
     * @return originalAuthenticationTypeCode
     */
    public com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode getOriginalAuthenticationTypeCode() {
        return originalAuthenticationTypeCode;
    }


    /**
     * Sets the originalAuthenticationTypeCode value for this UPAuthenticatorClone.
     * 
     * @param originalAuthenticationTypeCode
     */
    public void setOriginalAuthenticationTypeCode(com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode originalAuthenticationTypeCode) {
        this.originalAuthenticationTypeCode = originalAuthenticationTypeCode;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof UPAuthenticatorClone)) return false;
        UPAuthenticatorClone other = (UPAuthenticatorClone) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = super.equals(obj) && 
            ((this.originalAuthenticationTypeCode==null && other.getOriginalAuthenticationTypeCode()==null) || 
             (this.originalAuthenticationTypeCode!=null &&
              this.originalAuthenticationTypeCode.equals(other.getOriginalAuthenticationTypeCode())));
        __equalsCalc = null;
        return _equals;
    }

    private boolean __hashCodeCalc = false;
    public synchronized int hashCode() {
        if (__hashCodeCalc) {
            return 0;
        }
        __hashCodeCalc = true;
        int _hashCode = super.hashCode();
        if (getOriginalAuthenticationTypeCode() != null) {
            _hashCode += getOriginalAuthenticationTypeCode().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(UPAuthenticatorClone.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UPAuthenticatorClone"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("originalAuthenticationTypeCode");
        elemField.setXmlName(new javax.xml.namespace.QName("", "originalAuthenticationTypeCode"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTypeCode"));
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
