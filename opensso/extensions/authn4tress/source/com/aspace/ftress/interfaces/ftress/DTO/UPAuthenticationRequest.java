/**
 * UPAuthenticationRequest.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.aspace.ftress.interfaces.ftress.DTO;

public class UPAuthenticationRequest  extends com.aspace.ftress.interfaces.ftress.DTO.AuthenticationRequest  implements java.io.Serializable {
    private java.lang.String password;

    private int[] seedPositions;

    private com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode;

    private java.lang.String username;

    public UPAuthenticationRequest() {
    }

    public UPAuthenticationRequest(
           com.aspace.ftress.interfaces.ftress.DTO.AuthenticationRequestParameter[] auditedParameters,
           boolean authenticateNoSession,
           com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationTypeCode,
           com.aspace.ftress.interfaces.ftress.DTO.AuthenticationRequestParameter[] parameters,
           java.lang.String password,
           int[] seedPositions,
           com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode,
           java.lang.String username) {
        super(
            auditedParameters,
            authenticateNoSession,
            authenticationTypeCode,
            parameters);
        this.password = password;
        this.seedPositions = seedPositions;
        this.userCode = userCode;
        this.username = username;
    }


    /**
     * Gets the password value for this UPAuthenticationRequest.
     * 
     * @return password
     */
    public java.lang.String getPassword() {
        return password;
    }


    /**
     * Sets the password value for this UPAuthenticationRequest.
     * 
     * @param password
     */
    public void setPassword(java.lang.String password) {
        this.password = password;
    }


    /**
     * Gets the seedPositions value for this UPAuthenticationRequest.
     * 
     * @return seedPositions
     */
    public int[] getSeedPositions() {
        return seedPositions;
    }


    /**
     * Sets the seedPositions value for this UPAuthenticationRequest.
     * 
     * @param seedPositions
     */
    public void setSeedPositions(int[] seedPositions) {
        this.seedPositions = seedPositions;
    }


    /**
     * Gets the userCode value for this UPAuthenticationRequest.
     * 
     * @return userCode
     */
    public com.aspace.ftress.interfaces.ftress.DTO.UserCode getUserCode() {
        return userCode;
    }


    /**
     * Sets the userCode value for this UPAuthenticationRequest.
     * 
     * @param userCode
     */
    public void setUserCode(com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode) {
        this.userCode = userCode;
    }


    /**
     * Gets the username value for this UPAuthenticationRequest.
     * 
     * @return username
     */
    public java.lang.String getUsername() {
        return username;
    }


    /**
     * Sets the username value for this UPAuthenticationRequest.
     * 
     * @param username
     */
    public void setUsername(java.lang.String username) {
        this.username = username;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof UPAuthenticationRequest)) return false;
        UPAuthenticationRequest other = (UPAuthenticationRequest) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = super.equals(obj) && 
            ((this.password==null && other.getPassword()==null) || 
             (this.password!=null &&
              this.password.equals(other.getPassword()))) &&
            ((this.seedPositions==null && other.getSeedPositions()==null) || 
             (this.seedPositions!=null &&
              java.util.Arrays.equals(this.seedPositions, other.getSeedPositions()))) &&
            ((this.userCode==null && other.getUserCode()==null) || 
             (this.userCode!=null &&
              this.userCode.equals(other.getUserCode()))) &&
            ((this.username==null && other.getUsername()==null) || 
             (this.username!=null &&
              this.username.equals(other.getUsername())));
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
        if (getPassword() != null) {
            _hashCode += getPassword().hashCode();
        }
        if (getSeedPositions() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getSeedPositions());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getSeedPositions(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getUserCode() != null) {
            _hashCode += getUserCode().hashCode();
        }
        if (getUsername() != null) {
            _hashCode += getUsername().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(UPAuthenticationRequest.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UPAuthenticationRequest"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("password");
        elemField.setXmlName(new javax.xml.namespace.QName("", "password"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/encoding/", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("seedPositions");
        elemField.setXmlName(new javax.xml.namespace.QName("", "seedPositions"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("userCode");
        elemField.setXmlName(new javax.xml.namespace.QName("", "userCode"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("username");
        elemField.setXmlName(new javax.xml.namespace.QName("", "username"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/encoding/", "string"));
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
