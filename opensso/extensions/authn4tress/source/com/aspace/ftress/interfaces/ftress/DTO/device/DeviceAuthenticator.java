/**
 * DeviceAuthenticator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.aspace.ftress.interfaces.ftress.DTO.device;

public class DeviceAuthenticator  extends com.aspace.ftress.interfaces.ftress.DTO.Authenticator  implements java.io.Serializable {
    private com.aspace.ftress.interfaces.ftress.DTO.device.DeviceId deviceId;

    private int expiryThreshold;

    public DeviceAuthenticator() {
    }

    public DeviceAuthenticator(
           com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationTypeCode,
           com.aspace.ftress.interfaces.ftress.DTO.ChannelsBlocked channelsBlocked,
           com.aspace.ftress.interfaces.ftress.DTO.AuthenticationStatistics statistics,
           java.lang.String status,
           com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode,
           java.util.Calendar validFrom,
           java.util.Calendar validTo,
           com.aspace.ftress.interfaces.ftress.DTO.device.DeviceId deviceId,
           int expiryThreshold) {
        super(
            authenticationTypeCode,
            channelsBlocked,
            statistics,
            status,
            userCode,
            validFrom,
            validTo);
        this.deviceId = deviceId;
        this.expiryThreshold = expiryThreshold;
    }


    /**
     * Gets the deviceId value for this DeviceAuthenticator.
     * 
     * @return deviceId
     */
    public com.aspace.ftress.interfaces.ftress.DTO.device.DeviceId getDeviceId() {
        return deviceId;
    }


    /**
     * Sets the deviceId value for this DeviceAuthenticator.
     * 
     * @param deviceId
     */
    public void setDeviceId(com.aspace.ftress.interfaces.ftress.DTO.device.DeviceId deviceId) {
        this.deviceId = deviceId;
    }


    /**
     * Gets the expiryThreshold value for this DeviceAuthenticator.
     * 
     * @return expiryThreshold
     */
    public int getExpiryThreshold() {
        return expiryThreshold;
    }


    /**
     * Sets the expiryThreshold value for this DeviceAuthenticator.
     * 
     * @param expiryThreshold
     */
    public void setExpiryThreshold(int expiryThreshold) {
        this.expiryThreshold = expiryThreshold;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof DeviceAuthenticator)) return false;
        DeviceAuthenticator other = (DeviceAuthenticator) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = super.equals(obj) && 
            ((this.deviceId==null && other.getDeviceId()==null) || 
             (this.deviceId!=null &&
              this.deviceId.equals(other.getDeviceId()))) &&
            this.expiryThreshold == other.getExpiryThreshold();
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
        if (getDeviceId() != null) {
            _hashCode += getDeviceId().hashCode();
        }
        _hashCode += getExpiryThreshold();
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(DeviceAuthenticator.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://device.DTO.ftress.interfaces.ftress.aspace.com", "DeviceAuthenticator"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("deviceId");
        elemField.setXmlName(new javax.xml.namespace.QName("", "deviceId"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://device.DTO.ftress.interfaces.ftress.aspace.com", "DeviceId"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("expiryThreshold");
        elemField.setXmlName(new javax.xml.namespace.QName("", "expiryThreshold"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
        elemField.setNillable(false);
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
