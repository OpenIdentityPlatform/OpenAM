/**
 * DeviceSearchCriteria.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.aspace.ftress.interfaces.ftress.DTO.device;

public class DeviceSearchCriteria  extends com.aspace.ftress.interfaces.ftress.DTO.SearchCriteria  implements java.io.Serializable {
    private com.aspace.ftress.interfaces.ftress.DTO.device.DeviceGroupCode[] deviceGroupCodes;

    private com.aspace.ftress.interfaces.ftress.DTO.device.DeviceId deviceId;

    private com.aspace.ftress.interfaces.ftress.DTO.device.DeviceTypeCode deviceTypeCode;

    private java.util.Calendar expiryDate;

    private int issueNumber;

    private java.lang.String serialNumber;

    private java.util.Calendar startDate;

    public DeviceSearchCriteria() {
    }

    public DeviceSearchCriteria(
           com.aspace.ftress.interfaces.ftress.DTO.device.DeviceGroupCode[] deviceGroupCodes,
           com.aspace.ftress.interfaces.ftress.DTO.device.DeviceId deviceId,
           com.aspace.ftress.interfaces.ftress.DTO.device.DeviceTypeCode deviceTypeCode,
           java.util.Calendar expiryDate,
           int issueNumber,
           java.lang.String serialNumber,
           java.util.Calendar startDate) {
        this.deviceGroupCodes = deviceGroupCodes;
        this.deviceId = deviceId;
        this.deviceTypeCode = deviceTypeCode;
        this.expiryDate = expiryDate;
        this.issueNumber = issueNumber;
        this.serialNumber = serialNumber;
        this.startDate = startDate;
    }


    /**
     * Gets the deviceGroupCodes value for this DeviceSearchCriteria.
     * 
     * @return deviceGroupCodes
     */
    public com.aspace.ftress.interfaces.ftress.DTO.device.DeviceGroupCode[] getDeviceGroupCodes() {
        return deviceGroupCodes;
    }


    /**
     * Sets the deviceGroupCodes value for this DeviceSearchCriteria.
     * 
     * @param deviceGroupCodes
     */
    public void setDeviceGroupCodes(com.aspace.ftress.interfaces.ftress.DTO.device.DeviceGroupCode[] deviceGroupCodes) {
        this.deviceGroupCodes = deviceGroupCodes;
    }


    /**
     * Gets the deviceId value for this DeviceSearchCriteria.
     * 
     * @return deviceId
     */
    public com.aspace.ftress.interfaces.ftress.DTO.device.DeviceId getDeviceId() {
        return deviceId;
    }


    /**
     * Sets the deviceId value for this DeviceSearchCriteria.
     * 
     * @param deviceId
     */
    public void setDeviceId(com.aspace.ftress.interfaces.ftress.DTO.device.DeviceId deviceId) {
        this.deviceId = deviceId;
    }


    /**
     * Gets the deviceTypeCode value for this DeviceSearchCriteria.
     * 
     * @return deviceTypeCode
     */
    public com.aspace.ftress.interfaces.ftress.DTO.device.DeviceTypeCode getDeviceTypeCode() {
        return deviceTypeCode;
    }


    /**
     * Sets the deviceTypeCode value for this DeviceSearchCriteria.
     * 
     * @param deviceTypeCode
     */
    public void setDeviceTypeCode(com.aspace.ftress.interfaces.ftress.DTO.device.DeviceTypeCode deviceTypeCode) {
        this.deviceTypeCode = deviceTypeCode;
    }


    /**
     * Gets the expiryDate value for this DeviceSearchCriteria.
     * 
     * @return expiryDate
     */
    public java.util.Calendar getExpiryDate() {
        return expiryDate;
    }


    /**
     * Sets the expiryDate value for this DeviceSearchCriteria.
     * 
     * @param expiryDate
     */
    public void setExpiryDate(java.util.Calendar expiryDate) {
        this.expiryDate = expiryDate;
    }


    /**
     * Gets the issueNumber value for this DeviceSearchCriteria.
     * 
     * @return issueNumber
     */
    public int getIssueNumber() {
        return issueNumber;
    }


    /**
     * Sets the issueNumber value for this DeviceSearchCriteria.
     * 
     * @param issueNumber
     */
    public void setIssueNumber(int issueNumber) {
        this.issueNumber = issueNumber;
    }


    /**
     * Gets the serialNumber value for this DeviceSearchCriteria.
     * 
     * @return serialNumber
     */
    public java.lang.String getSerialNumber() {
        return serialNumber;
    }


    /**
     * Sets the serialNumber value for this DeviceSearchCriteria.
     * 
     * @param serialNumber
     */
    public void setSerialNumber(java.lang.String serialNumber) {
        this.serialNumber = serialNumber;
    }


    /**
     * Gets the startDate value for this DeviceSearchCriteria.
     * 
     * @return startDate
     */
    public java.util.Calendar getStartDate() {
        return startDate;
    }


    /**
     * Sets the startDate value for this DeviceSearchCriteria.
     * 
     * @param startDate
     */
    public void setStartDate(java.util.Calendar startDate) {
        this.startDate = startDate;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof DeviceSearchCriteria)) return false;
        DeviceSearchCriteria other = (DeviceSearchCriteria) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = super.equals(obj) && 
            ((this.deviceGroupCodes==null && other.getDeviceGroupCodes()==null) || 
             (this.deviceGroupCodes!=null &&
              java.util.Arrays.equals(this.deviceGroupCodes, other.getDeviceGroupCodes()))) &&
            ((this.deviceId==null && other.getDeviceId()==null) || 
             (this.deviceId!=null &&
              this.deviceId.equals(other.getDeviceId()))) &&
            ((this.deviceTypeCode==null && other.getDeviceTypeCode()==null) || 
             (this.deviceTypeCode!=null &&
              this.deviceTypeCode.equals(other.getDeviceTypeCode()))) &&
            ((this.expiryDate==null && other.getExpiryDate()==null) || 
             (this.expiryDate!=null &&
              this.expiryDate.equals(other.getExpiryDate()))) &&
            this.issueNumber == other.getIssueNumber() &&
            ((this.serialNumber==null && other.getSerialNumber()==null) || 
             (this.serialNumber!=null &&
              this.serialNumber.equals(other.getSerialNumber()))) &&
            ((this.startDate==null && other.getStartDate()==null) || 
             (this.startDate!=null &&
              this.startDate.equals(other.getStartDate())));
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
        if (getDeviceGroupCodes() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getDeviceGroupCodes());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getDeviceGroupCodes(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getDeviceId() != null) {
            _hashCode += getDeviceId().hashCode();
        }
        if (getDeviceTypeCode() != null) {
            _hashCode += getDeviceTypeCode().hashCode();
        }
        if (getExpiryDate() != null) {
            _hashCode += getExpiryDate().hashCode();
        }
        _hashCode += getIssueNumber();
        if (getSerialNumber() != null) {
            _hashCode += getSerialNumber().hashCode();
        }
        if (getStartDate() != null) {
            _hashCode += getStartDate().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(DeviceSearchCriteria.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://device.DTO.ftress.interfaces.ftress.aspace.com", "DeviceSearchCriteria"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("deviceGroupCodes");
        elemField.setXmlName(new javax.xml.namespace.QName("", "deviceGroupCodes"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://device.DTO.ftress.interfaces.ftress.aspace.com", "DeviceGroupCode"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("deviceId");
        elemField.setXmlName(new javax.xml.namespace.QName("", "deviceId"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://device.DTO.ftress.interfaces.ftress.aspace.com", "DeviceId"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("deviceTypeCode");
        elemField.setXmlName(new javax.xml.namespace.QName("", "deviceTypeCode"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://device.DTO.ftress.interfaces.ftress.aspace.com", "DeviceTypeCode"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("expiryDate");
        elemField.setXmlName(new javax.xml.namespace.QName("", "expiryDate"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "dateTime"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("issueNumber");
        elemField.setXmlName(new javax.xml.namespace.QName("", "issueNumber"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("serialNumber");
        elemField.setXmlName(new javax.xml.namespace.QName("", "serialNumber"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/encoding/", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("startDate");
        elemField.setXmlName(new javax.xml.namespace.QName("", "startDate"));
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
