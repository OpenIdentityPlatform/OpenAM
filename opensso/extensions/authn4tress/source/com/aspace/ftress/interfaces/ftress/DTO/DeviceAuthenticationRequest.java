/**
 * DeviceAuthenticationRequest.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.aspace.ftress.interfaces.ftress.DTO;

public class DeviceAuthenticationRequest  extends com.aspace.ftress.interfaces.ftress.DTO.AuthenticationRequest  implements java.io.Serializable {
    private int authenticationMode;

    private java.lang.String challenge;

    private com.aspace.ftress.interfaces.ftress.DTO.device.DeviceSearchCriteria deviceCriteria;

    private java.lang.String oneTimePassword;

    private com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode;

    public DeviceAuthenticationRequest() {
    }

    public DeviceAuthenticationRequest(
           com.aspace.ftress.interfaces.ftress.DTO.AuthenticationRequestParameter[] auditedParameters,
           boolean authenticateNoSession,
           com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationTypeCode,
           com.aspace.ftress.interfaces.ftress.DTO.AuthenticationRequestParameter[] parameters,
           int authenticationMode,
           java.lang.String challenge,
           com.aspace.ftress.interfaces.ftress.DTO.device.DeviceSearchCriteria deviceCriteria,
           java.lang.String oneTimePassword,
           com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode) {
        super(
            auditedParameters,
            authenticateNoSession,
            authenticationTypeCode,
            parameters);
        this.authenticationMode = authenticationMode;
        this.challenge = challenge;
        this.deviceCriteria = deviceCriteria;
        this.oneTimePassword = oneTimePassword;
        this.userCode = userCode;
    }


    /**
     * Gets the authenticationMode value for this DeviceAuthenticationRequest.
     * 
     * @return authenticationMode
     */
    public int getAuthenticationMode() {
        return authenticationMode;
    }


    /**
     * Sets the authenticationMode value for this DeviceAuthenticationRequest.
     * 
     * @param authenticationMode
     */
    public void setAuthenticationMode(int authenticationMode) {
        this.authenticationMode = authenticationMode;
    }


    /**
     * Gets the challenge value for this DeviceAuthenticationRequest.
     * 
     * @return challenge
     */
    public java.lang.String getChallenge() {
        return challenge;
    }


    /**
     * Sets the challenge value for this DeviceAuthenticationRequest.
     * 
     * @param challenge
     */
    public void setChallenge(java.lang.String challenge) {
        this.challenge = challenge;
    }


    /**
     * Gets the deviceCriteria value for this DeviceAuthenticationRequest.
     * 
     * @return deviceCriteria
     */
    public com.aspace.ftress.interfaces.ftress.DTO.device.DeviceSearchCriteria getDeviceCriteria() {
        return deviceCriteria;
    }


    /**
     * Sets the deviceCriteria value for this DeviceAuthenticationRequest.
     * 
     * @param deviceCriteria
     */
    public void setDeviceCriteria(com.aspace.ftress.interfaces.ftress.DTO.device.DeviceSearchCriteria deviceCriteria) {
        this.deviceCriteria = deviceCriteria;
    }


    /**
     * Gets the oneTimePassword value for this DeviceAuthenticationRequest.
     * 
     * @return oneTimePassword
     */
    public java.lang.String getOneTimePassword() {
        return oneTimePassword;
    }


    /**
     * Sets the oneTimePassword value for this DeviceAuthenticationRequest.
     * 
     * @param oneTimePassword
     */
    public void setOneTimePassword(java.lang.String oneTimePassword) {
        this.oneTimePassword = oneTimePassword;
    }


    /**
     * Gets the userCode value for this DeviceAuthenticationRequest.
     * 
     * @return userCode
     */
    public com.aspace.ftress.interfaces.ftress.DTO.UserCode getUserCode() {
        return userCode;
    }


    /**
     * Sets the userCode value for this DeviceAuthenticationRequest.
     * 
     * @param userCode
     */
    public void setUserCode(com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode) {
        this.userCode = userCode;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof DeviceAuthenticationRequest)) return false;
        DeviceAuthenticationRequest other = (DeviceAuthenticationRequest) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = super.equals(obj) && 
            this.authenticationMode == other.getAuthenticationMode() &&
            ((this.challenge==null && other.getChallenge()==null) || 
             (this.challenge!=null &&
              this.challenge.equals(other.getChallenge()))) &&
            ((this.deviceCriteria==null && other.getDeviceCriteria()==null) || 
             (this.deviceCriteria!=null &&
              this.deviceCriteria.equals(other.getDeviceCriteria()))) &&
            ((this.oneTimePassword==null && other.getOneTimePassword()==null) || 
             (this.oneTimePassword!=null &&
              this.oneTimePassword.equals(other.getOneTimePassword()))) &&
            ((this.userCode==null && other.getUserCode()==null) || 
             (this.userCode!=null &&
              this.userCode.equals(other.getUserCode())));
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
        _hashCode += getAuthenticationMode();
        if (getChallenge() != null) {
            _hashCode += getChallenge().hashCode();
        }
        if (getDeviceCriteria() != null) {
            _hashCode += getDeviceCriteria().hashCode();
        }
        if (getOneTimePassword() != null) {
            _hashCode += getOneTimePassword().hashCode();
        }
        if (getUserCode() != null) {
            _hashCode += getUserCode().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(DeviceAuthenticationRequest.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "DeviceAuthenticationRequest"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("authenticationMode");
        elemField.setXmlName(new javax.xml.namespace.QName("", "authenticationMode"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("challenge");
        elemField.setXmlName(new javax.xml.namespace.QName("", "challenge"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/encoding/", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("deviceCriteria");
        elemField.setXmlName(new javax.xml.namespace.QName("", "deviceCriteria"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://device.DTO.ftress.interfaces.ftress.aspace.com", "DeviceSearchCriteria"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("oneTimePassword");
        elemField.setXmlName(new javax.xml.namespace.QName("", "oneTimePassword"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/encoding/", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("userCode");
        elemField.setXmlName(new javax.xml.namespace.QName("", "userCode"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"));
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
