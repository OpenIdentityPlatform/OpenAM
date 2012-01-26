/**
 * DeviceAutoSynchronisationRequest.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.aspace.ftress.interfaces.ftress.DTO.device;

public class DeviceAutoSynchronisationRequest  extends com.aspace.ftress.interfaces.ftress.DTO.device.DeviceSynchronisationRequest  implements java.io.Serializable {
    private java.lang.String oneTimePassword;

    public DeviceAutoSynchronisationRequest() {
    }

    public DeviceAutoSynchronisationRequest(
           com.aspace.ftress.interfaces.ftress.DTO.device.DeviceSearchCriteria deviceSearchCriteria,
           java.lang.String oneTimePassword) {
        super(
            deviceSearchCriteria);
        this.oneTimePassword = oneTimePassword;
    }


    /**
     * Gets the oneTimePassword value for this DeviceAutoSynchronisationRequest.
     * 
     * @return oneTimePassword
     */
    public java.lang.String getOneTimePassword() {
        return oneTimePassword;
    }


    /**
     * Sets the oneTimePassword value for this DeviceAutoSynchronisationRequest.
     * 
     * @param oneTimePassword
     */
    public void setOneTimePassword(java.lang.String oneTimePassword) {
        this.oneTimePassword = oneTimePassword;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof DeviceAutoSynchronisationRequest)) return false;
        DeviceAutoSynchronisationRequest other = (DeviceAutoSynchronisationRequest) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = super.equals(obj) && 
            ((this.oneTimePassword==null && other.getOneTimePassword()==null) || 
             (this.oneTimePassword!=null &&
              this.oneTimePassword.equals(other.getOneTimePassword())));
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
        if (getOneTimePassword() != null) {
            _hashCode += getOneTimePassword().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(DeviceAutoSynchronisationRequest.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://device.DTO.ftress.interfaces.ftress.aspace.com", "DeviceAutoSynchronisationRequest"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("oneTimePassword");
        elemField.setXmlName(new javax.xml.namespace.QName("", "oneTimePassword"));
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
