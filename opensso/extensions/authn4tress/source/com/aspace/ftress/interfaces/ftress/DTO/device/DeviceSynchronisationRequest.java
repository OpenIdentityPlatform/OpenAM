/**
 * DeviceSynchronisationRequest.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.aspace.ftress.interfaces.ftress.DTO.device;

public abstract class DeviceSynchronisationRequest  implements java.io.Serializable {
    private com.aspace.ftress.interfaces.ftress.DTO.device.DeviceSearchCriteria deviceSearchCriteria;

    public DeviceSynchronisationRequest() {
    }

    public DeviceSynchronisationRequest(
           com.aspace.ftress.interfaces.ftress.DTO.device.DeviceSearchCriteria deviceSearchCriteria) {
           this.deviceSearchCriteria = deviceSearchCriteria;
    }


    /**
     * Gets the deviceSearchCriteria value for this DeviceSynchronisationRequest.
     * 
     * @return deviceSearchCriteria
     */
    public com.aspace.ftress.interfaces.ftress.DTO.device.DeviceSearchCriteria getDeviceSearchCriteria() {
        return deviceSearchCriteria;
    }


    /**
     * Sets the deviceSearchCriteria value for this DeviceSynchronisationRequest.
     * 
     * @param deviceSearchCriteria
     */
    public void setDeviceSearchCriteria(com.aspace.ftress.interfaces.ftress.DTO.device.DeviceSearchCriteria deviceSearchCriteria) {
        this.deviceSearchCriteria = deviceSearchCriteria;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof DeviceSynchronisationRequest)) return false;
        DeviceSynchronisationRequest other = (DeviceSynchronisationRequest) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.deviceSearchCriteria==null && other.getDeviceSearchCriteria()==null) || 
             (this.deviceSearchCriteria!=null &&
              this.deviceSearchCriteria.equals(other.getDeviceSearchCriteria())));
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
        if (getDeviceSearchCriteria() != null) {
            _hashCode += getDeviceSearchCriteria().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(DeviceSynchronisationRequest.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://device.DTO.ftress.interfaces.ftress.aspace.com", "DeviceSynchronisationRequest"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("deviceSearchCriteria");
        elemField.setXmlName(new javax.xml.namespace.QName("", "deviceSearchCriteria"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://device.DTO.ftress.interfaces.ftress.aspace.com", "DeviceSearchCriteria"));
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
