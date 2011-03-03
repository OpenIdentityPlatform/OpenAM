/**
 * DeviceManualSynchronisationRequest.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.aspace.ftress.interfaces.ftress.DTO.device;

public class DeviceManualSynchronisationRequest  extends com.aspace.ftress.interfaces.ftress.DTO.device.DeviceSynchronisationRequest  implements java.io.Serializable {
    private long clock;

    private long counter;

    public DeviceManualSynchronisationRequest() {
    }

    public DeviceManualSynchronisationRequest(
           com.aspace.ftress.interfaces.ftress.DTO.device.DeviceSearchCriteria deviceSearchCriteria,
           long clock,
           long counter) {
        super(
            deviceSearchCriteria);
        this.clock = clock;
        this.counter = counter;
    }


    /**
     * Gets the clock value for this DeviceManualSynchronisationRequest.
     * 
     * @return clock
     */
    public long getClock() {
        return clock;
    }


    /**
     * Sets the clock value for this DeviceManualSynchronisationRequest.
     * 
     * @param clock
     */
    public void setClock(long clock) {
        this.clock = clock;
    }


    /**
     * Gets the counter value for this DeviceManualSynchronisationRequest.
     * 
     * @return counter
     */
    public long getCounter() {
        return counter;
    }


    /**
     * Sets the counter value for this DeviceManualSynchronisationRequest.
     * 
     * @param counter
     */
    public void setCounter(long counter) {
        this.counter = counter;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof DeviceManualSynchronisationRequest)) return false;
        DeviceManualSynchronisationRequest other = (DeviceManualSynchronisationRequest) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = super.equals(obj) && 
            this.clock == other.getClock() &&
            this.counter == other.getCounter();
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
        _hashCode += new Long(getClock()).hashCode();
        _hashCode += new Long(getCounter()).hashCode();
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(DeviceManualSynchronisationRequest.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://device.DTO.ftress.interfaces.ftress.aspace.com", "DeviceManualSynchronisationRequest"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("clock");
        elemField.setXmlName(new javax.xml.namespace.QName("", "clock"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "long"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("counter");
        elemField.setXmlName(new javax.xml.namespace.QName("", "counter"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "long"));
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
