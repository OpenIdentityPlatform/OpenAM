/**
 * DeviceException.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.aspace.ftress.interfaces.ftress.DTO.exception;

public class DeviceException  extends com.aspace.ftress.interfaces.ftress.DTO.exception.BusinessException  implements java.io.Serializable {
    private com.aspace.ftress.interfaces.ftress.DTO.device.DeviceSearchCriteria criteria;

    private com.aspace.ftress.interfaces.ftress.DTO.device.Device[] devices;

    private java.util.Calendar expiryDate;

    private com.aspace.ftress.interfaces.ftress.DTO.device.DeviceSynchronisationRequest synchronisationRequest;

    private java.lang.String userCode;

    public DeviceException() {
    }

    public DeviceException(
           int errorCode,
           com.aspace.ftress.interfaces.ftress.DTO.Parameter[] parameters,
           long reference,
           com.aspace.ftress.interfaces.ftress.DTO.device.DeviceSearchCriteria criteria,
           com.aspace.ftress.interfaces.ftress.DTO.device.Device[] devices,
           java.util.Calendar expiryDate,
           com.aspace.ftress.interfaces.ftress.DTO.device.DeviceSynchronisationRequest synchronisationRequest,
           java.lang.String userCode) {
        super(
            errorCode,
            parameters,
            reference);
        this.criteria = criteria;
        this.devices = devices;
        this.expiryDate = expiryDate;
        this.synchronisationRequest = synchronisationRequest;
        this.userCode = userCode;
    }


    /**
     * Gets the criteria value for this DeviceException.
     * 
     * @return criteria
     */
    public com.aspace.ftress.interfaces.ftress.DTO.device.DeviceSearchCriteria getCriteria() {
        return criteria;
    }


    /**
     * Sets the criteria value for this DeviceException.
     * 
     * @param criteria
     */
    public void setCriteria(com.aspace.ftress.interfaces.ftress.DTO.device.DeviceSearchCriteria criteria) {
        this.criteria = criteria;
    }


    /**
     * Gets the devices value for this DeviceException.
     * 
     * @return devices
     */
    public com.aspace.ftress.interfaces.ftress.DTO.device.Device[] getDevices() {
        return devices;
    }


    /**
     * Sets the devices value for this DeviceException.
     * 
     * @param devices
     */
    public void setDevices(com.aspace.ftress.interfaces.ftress.DTO.device.Device[] devices) {
        this.devices = devices;
    }


    /**
     * Gets the expiryDate value for this DeviceException.
     * 
     * @return expiryDate
     */
    public java.util.Calendar getExpiryDate() {
        return expiryDate;
    }


    /**
     * Sets the expiryDate value for this DeviceException.
     * 
     * @param expiryDate
     */
    public void setExpiryDate(java.util.Calendar expiryDate) {
        this.expiryDate = expiryDate;
    }


    /**
     * Gets the synchronisationRequest value for this DeviceException.
     * 
     * @return synchronisationRequest
     */
    public com.aspace.ftress.interfaces.ftress.DTO.device.DeviceSynchronisationRequest getSynchronisationRequest() {
        return synchronisationRequest;
    }


    /**
     * Sets the synchronisationRequest value for this DeviceException.
     * 
     * @param synchronisationRequest
     */
    public void setSynchronisationRequest(com.aspace.ftress.interfaces.ftress.DTO.device.DeviceSynchronisationRequest synchronisationRequest) {
        this.synchronisationRequest = synchronisationRequest;
    }


    /**
     * Gets the userCode value for this DeviceException.
     * 
     * @return userCode
     */
    public java.lang.String getUserCode() {
        return userCode;
    }


    /**
     * Sets the userCode value for this DeviceException.
     * 
     * @param userCode
     */
    public void setUserCode(java.lang.String userCode) {
        this.userCode = userCode;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof DeviceException)) return false;
        DeviceException other = (DeviceException) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = super.equals(obj) && 
            ((this.criteria==null && other.getCriteria()==null) || 
             (this.criteria!=null &&
              this.criteria.equals(other.getCriteria()))) &&
            ((this.devices==null && other.getDevices()==null) || 
             (this.devices!=null &&
              java.util.Arrays.equals(this.devices, other.getDevices()))) &&
            ((this.expiryDate==null && other.getExpiryDate()==null) || 
             (this.expiryDate!=null &&
              this.expiryDate.equals(other.getExpiryDate()))) &&
            ((this.synchronisationRequest==null && other.getSynchronisationRequest()==null) || 
             (this.synchronisationRequest!=null &&
              this.synchronisationRequest.equals(other.getSynchronisationRequest()))) &&
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
        if (getCriteria() != null) {
            _hashCode += getCriteria().hashCode();
        }
        if (getDevices() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getDevices());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getDevices(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getExpiryDate() != null) {
            _hashCode += getExpiryDate().hashCode();
        }
        if (getSynchronisationRequest() != null) {
            _hashCode += getSynchronisationRequest().hashCode();
        }
        if (getUserCode() != null) {
            _hashCode += getUserCode().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(DeviceException.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "DeviceException"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("criteria");
        elemField.setXmlName(new javax.xml.namespace.QName("", "criteria"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://device.DTO.ftress.interfaces.ftress.aspace.com", "DeviceSearchCriteria"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("devices");
        elemField.setXmlName(new javax.xml.namespace.QName("", "devices"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://device.DTO.ftress.interfaces.ftress.aspace.com", "Device"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("expiryDate");
        elemField.setXmlName(new javax.xml.namespace.QName("", "expiryDate"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "dateTime"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("synchronisationRequest");
        elemField.setXmlName(new javax.xml.namespace.QName("", "synchronisationRequest"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://device.DTO.ftress.interfaces.ftress.aspace.com", "DeviceSynchronisationRequest"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("userCode");
        elemField.setXmlName(new javax.xml.namespace.QName("", "userCode"));
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


    /**
     * Writes the exception data to the faultDetails
     */
    public void writeDetails(javax.xml.namespace.QName qname, org.apache.axis.encoding.SerializationContext context) throws java.io.IOException {
        context.serialize(qname, null, this);
    }
}
