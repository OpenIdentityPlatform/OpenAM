/**
 * Authenticator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.aspace.ftress.interfaces.ftress.DTO;

public class Authenticator  implements java.io.Serializable {
    private com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationTypeCode;

    private com.aspace.ftress.interfaces.ftress.DTO.ChannelsBlocked channelsBlocked;

    private com.aspace.ftress.interfaces.ftress.DTO.AuthenticationStatistics statistics;

    private java.lang.String status;

    private com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode;

    private java.util.Calendar validFrom;

    private java.util.Calendar validTo;

    public Authenticator() {
    }

    public Authenticator(
           com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationTypeCode,
           com.aspace.ftress.interfaces.ftress.DTO.ChannelsBlocked channelsBlocked,
           com.aspace.ftress.interfaces.ftress.DTO.AuthenticationStatistics statistics,
           java.lang.String status,
           com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode,
           java.util.Calendar validFrom,
           java.util.Calendar validTo) {
           this.authenticationTypeCode = authenticationTypeCode;
           this.channelsBlocked = channelsBlocked;
           this.statistics = statistics;
           this.status = status;
           this.userCode = userCode;
           this.validFrom = validFrom;
           this.validTo = validTo;
    }


    /**
     * Gets the authenticationTypeCode value for this Authenticator.
     * 
     * @return authenticationTypeCode
     */
    public com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode getAuthenticationTypeCode() {
        return authenticationTypeCode;
    }


    /**
     * Sets the authenticationTypeCode value for this Authenticator.
     * 
     * @param authenticationTypeCode
     */
    public void setAuthenticationTypeCode(com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationTypeCode) {
        this.authenticationTypeCode = authenticationTypeCode;
    }


    /**
     * Gets the channelsBlocked value for this Authenticator.
     * 
     * @return channelsBlocked
     */
    public com.aspace.ftress.interfaces.ftress.DTO.ChannelsBlocked getChannelsBlocked() {
        return channelsBlocked;
    }


    /**
     * Sets the channelsBlocked value for this Authenticator.
     * 
     * @param channelsBlocked
     */
    public void setChannelsBlocked(com.aspace.ftress.interfaces.ftress.DTO.ChannelsBlocked channelsBlocked) {
        this.channelsBlocked = channelsBlocked;
    }


    /**
     * Gets the statistics value for this Authenticator.
     * 
     * @return statistics
     */
    public com.aspace.ftress.interfaces.ftress.DTO.AuthenticationStatistics getStatistics() {
        return statistics;
    }


    /**
     * Sets the statistics value for this Authenticator.
     * 
     * @param statistics
     */
    public void setStatistics(com.aspace.ftress.interfaces.ftress.DTO.AuthenticationStatistics statistics) {
        this.statistics = statistics;
    }


    /**
     * Gets the status value for this Authenticator.
     * 
     * @return status
     */
    public java.lang.String getStatus() {
        return status;
    }


    /**
     * Sets the status value for this Authenticator.
     * 
     * @param status
     */
    public void setStatus(java.lang.String status) {
        this.status = status;
    }


    /**
     * Gets the userCode value for this Authenticator.
     * 
     * @return userCode
     */
    public com.aspace.ftress.interfaces.ftress.DTO.UserCode getUserCode() {
        return userCode;
    }


    /**
     * Sets the userCode value for this Authenticator.
     * 
     * @param userCode
     */
    public void setUserCode(com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode) {
        this.userCode = userCode;
    }


    /**
     * Gets the validFrom value for this Authenticator.
     * 
     * @return validFrom
     */
    public java.util.Calendar getValidFrom() {
        return validFrom;
    }


    /**
     * Sets the validFrom value for this Authenticator.
     * 
     * @param validFrom
     */
    public void setValidFrom(java.util.Calendar validFrom) {
        this.validFrom = validFrom;
    }


    /**
     * Gets the validTo value for this Authenticator.
     * 
     * @return validTo
     */
    public java.util.Calendar getValidTo() {
        return validTo;
    }


    /**
     * Sets the validTo value for this Authenticator.
     * 
     * @param validTo
     */
    public void setValidTo(java.util.Calendar validTo) {
        this.validTo = validTo;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof Authenticator)) return false;
        Authenticator other = (Authenticator) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.authenticationTypeCode==null && other.getAuthenticationTypeCode()==null) || 
             (this.authenticationTypeCode!=null &&
              this.authenticationTypeCode.equals(other.getAuthenticationTypeCode()))) &&
            ((this.channelsBlocked==null && other.getChannelsBlocked()==null) || 
             (this.channelsBlocked!=null &&
              this.channelsBlocked.equals(other.getChannelsBlocked()))) &&
            ((this.statistics==null && other.getStatistics()==null) || 
             (this.statistics!=null &&
              this.statistics.equals(other.getStatistics()))) &&
            ((this.status==null && other.getStatus()==null) || 
             (this.status!=null &&
              this.status.equals(other.getStatus()))) &&
            ((this.userCode==null && other.getUserCode()==null) || 
             (this.userCode!=null &&
              this.userCode.equals(other.getUserCode()))) &&
            ((this.validFrom==null && other.getValidFrom()==null) || 
             (this.validFrom!=null &&
              this.validFrom.equals(other.getValidFrom()))) &&
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
        if (getAuthenticationTypeCode() != null) {
            _hashCode += getAuthenticationTypeCode().hashCode();
        }
        if (getChannelsBlocked() != null) {
            _hashCode += getChannelsBlocked().hashCode();
        }
        if (getStatistics() != null) {
            _hashCode += getStatistics().hashCode();
        }
        if (getStatus() != null) {
            _hashCode += getStatus().hashCode();
        }
        if (getUserCode() != null) {
            _hashCode += getUserCode().hashCode();
        }
        if (getValidFrom() != null) {
            _hashCode += getValidFrom().hashCode();
        }
        if (getValidTo() != null) {
            _hashCode += getValidTo().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(Authenticator.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "Authenticator"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("authenticationTypeCode");
        elemField.setXmlName(new javax.xml.namespace.QName("", "authenticationTypeCode"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTypeCode"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("channelsBlocked");
        elemField.setXmlName(new javax.xml.namespace.QName("", "channelsBlocked"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelsBlocked"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("statistics");
        elemField.setXmlName(new javax.xml.namespace.QName("", "statistics"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationStatistics"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("status");
        elemField.setXmlName(new javax.xml.namespace.QName("", "status"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/encoding/", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("userCode");
        elemField.setXmlName(new javax.xml.namespace.QName("", "userCode"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("validFrom");
        elemField.setXmlName(new javax.xml.namespace.QName("", "validFrom"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "dateTime"));
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
