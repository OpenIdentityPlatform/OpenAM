/**
 * UPAuthenticator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.aspace.ftress.interfaces.ftress.DTO;

public class UPAuthenticator  extends com.aspace.ftress.interfaces.ftress.DTO.Authenticator  implements java.io.Serializable {
    private int expiryThreshold;

    private java.lang.String password;

    private int passwordResetStatus;

    private java.util.Calendar passwordResetStatusDate;

    private java.lang.String username;

    public UPAuthenticator() {
    }

    public UPAuthenticator(
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
           java.lang.String username) {
        super(
            authenticationTypeCode,
            channelsBlocked,
            statistics,
            status,
            userCode,
            validFrom,
            validTo);
        this.expiryThreshold = expiryThreshold;
        this.password = password;
        this.passwordResetStatus = passwordResetStatus;
        this.passwordResetStatusDate = passwordResetStatusDate;
        this.username = username;
    }


    /**
     * Gets the expiryThreshold value for this UPAuthenticator.
     * 
     * @return expiryThreshold
     */
    public int getExpiryThreshold() {
        return expiryThreshold;
    }


    /**
     * Sets the expiryThreshold value for this UPAuthenticator.
     * 
     * @param expiryThreshold
     */
    public void setExpiryThreshold(int expiryThreshold) {
        this.expiryThreshold = expiryThreshold;
    }


    /**
     * Gets the password value for this UPAuthenticator.
     * 
     * @return password
     */
    public java.lang.String getPassword() {
        return password;
    }


    /**
     * Sets the password value for this UPAuthenticator.
     * 
     * @param password
     */
    public void setPassword(java.lang.String password) {
        this.password = password;
    }


    /**
     * Gets the passwordResetStatus value for this UPAuthenticator.
     * 
     * @return passwordResetStatus
     */
    public int getPasswordResetStatus() {
        return passwordResetStatus;
    }


    /**
     * Sets the passwordResetStatus value for this UPAuthenticator.
     * 
     * @param passwordResetStatus
     */
    public void setPasswordResetStatus(int passwordResetStatus) {
        this.passwordResetStatus = passwordResetStatus;
    }


    /**
     * Gets the passwordResetStatusDate value for this UPAuthenticator.
     * 
     * @return passwordResetStatusDate
     */
    public java.util.Calendar getPasswordResetStatusDate() {
        return passwordResetStatusDate;
    }


    /**
     * Sets the passwordResetStatusDate value for this UPAuthenticator.
     * 
     * @param passwordResetStatusDate
     */
    public void setPasswordResetStatusDate(java.util.Calendar passwordResetStatusDate) {
        this.passwordResetStatusDate = passwordResetStatusDate;
    }


    /**
     * Gets the username value for this UPAuthenticator.
     * 
     * @return username
     */
    public java.lang.String getUsername() {
        return username;
    }


    /**
     * Sets the username value for this UPAuthenticator.
     * 
     * @param username
     */
    public void setUsername(java.lang.String username) {
        this.username = username;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof UPAuthenticator)) return false;
        UPAuthenticator other = (UPAuthenticator) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = super.equals(obj) && 
            this.expiryThreshold == other.getExpiryThreshold() &&
            ((this.password==null && other.getPassword()==null) || 
             (this.password!=null &&
              this.password.equals(other.getPassword()))) &&
            this.passwordResetStatus == other.getPasswordResetStatus() &&
            ((this.passwordResetStatusDate==null && other.getPasswordResetStatusDate()==null) || 
             (this.passwordResetStatusDate!=null &&
              this.passwordResetStatusDate.equals(other.getPasswordResetStatusDate()))) &&
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
        _hashCode += getExpiryThreshold();
        if (getPassword() != null) {
            _hashCode += getPassword().hashCode();
        }
        _hashCode += getPasswordResetStatus();
        if (getPasswordResetStatusDate() != null) {
            _hashCode += getPasswordResetStatusDate().hashCode();
        }
        if (getUsername() != null) {
            _hashCode += getUsername().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(UPAuthenticator.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UPAuthenticator"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("expiryThreshold");
        elemField.setXmlName(new javax.xml.namespace.QName("", "expiryThreshold"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("password");
        elemField.setXmlName(new javax.xml.namespace.QName("", "password"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/encoding/", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("passwordResetStatus");
        elemField.setXmlName(new javax.xml.namespace.QName("", "passwordResetStatus"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("passwordResetStatusDate");
        elemField.setXmlName(new javax.xml.namespace.QName("", "passwordResetStatusDate"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "dateTime"));
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
