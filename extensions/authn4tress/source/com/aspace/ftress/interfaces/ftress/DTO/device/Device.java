/**
 * Device.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.aspace.ftress.interfaces.ftress.DTO.device;

public class Device  implements java.io.Serializable {
    private byte[] SDB;

    private java.lang.String SDBKey;

    private java.util.Calendar addedDate;

    private com.aspace.ftress.interfaces.ftress.DTO.device.DeviceGroupCode deviceGroupCode;

    private com.aspace.ftress.interfaces.ftress.DTO.device.DeviceId deviceId;

    private com.aspace.ftress.interfaces.ftress.DTO.device.DeviceTypeCode deviceTypeCode;

    private java.util.Calendar expiryDate;

    private int issueNumber;

    private boolean neverExpires;

    private java.lang.String serialNumber;

    private java.util.Calendar startDate;

    private com.aspace.ftress.interfaces.ftress.DTO.device.DeviceStatus status;

    public Device() {
    }

    public Device(
           byte[] SDB,
           java.lang.String SDBKey,
           java.util.Calendar addedDate,
           com.aspace.ftress.interfaces.ftress.DTO.device.DeviceGroupCode deviceGroupCode,
           com.aspace.ftress.interfaces.ftress.DTO.device.DeviceId deviceId,
           com.aspace.ftress.interfaces.ftress.DTO.device.DeviceTypeCode deviceTypeCode,
           java.util.Calendar expiryDate,
           int issueNumber,
           boolean neverExpires,
           java.lang.String serialNumber,
           java.util.Calendar startDate,
           com.aspace.ftress.interfaces.ftress.DTO.device.DeviceStatus status) {
           this.SDB = SDB;
           this.SDBKey = SDBKey;
           this.addedDate = addedDate;
           this.deviceGroupCode = deviceGroupCode;
           this.deviceId = deviceId;
           this.deviceTypeCode = deviceTypeCode;
           this.expiryDate = expiryDate;
           this.issueNumber = issueNumber;
           this.neverExpires = neverExpires;
           this.serialNumber = serialNumber;
           this.startDate = startDate;
           this.status = status;
    }


    /**
     * Gets the SDB value for this Device.
     * 
     * @return SDB
     */
    public byte[] getSDB() {
        return SDB;
    }


    /**
     * Sets the SDB value for this Device.
     * 
     * @param SDB
     */
    public void setSDB(byte[] SDB) {
        this.SDB = SDB;
    }


    /**
     * Gets the SDBKey value for this Device.
     * 
     * @return SDBKey
     */
    public java.lang.String getSDBKey() {
        return SDBKey;
    }


    /**
     * Sets the SDBKey value for this Device.
     * 
     * @param SDBKey
     */
    public void setSDBKey(java.lang.String SDBKey) {
        this.SDBKey = SDBKey;
    }


    /**
     * Gets the addedDate value for this Device.
     * 
     * @return addedDate
     */
    public java.util.Calendar getAddedDate() {
        return addedDate;
    }


    /**
     * Sets the addedDate value for this Device.
     * 
     * @param addedDate
     */
    public void setAddedDate(java.util.Calendar addedDate) {
        this.addedDate = addedDate;
    }


    /**
     * Gets the deviceGroupCode value for this Device.
     * 
     * @return deviceGroupCode
     */
    public com.aspace.ftress.interfaces.ftress.DTO.device.DeviceGroupCode getDeviceGroupCode() {
        return deviceGroupCode;
    }


    /**
     * Sets the deviceGroupCode value for this Device.
     * 
     * @param deviceGroupCode
     */
    public void setDeviceGroupCode(com.aspace.ftress.interfaces.ftress.DTO.device.DeviceGroupCode deviceGroupCode) {
        this.deviceGroupCode = deviceGroupCode;
    }


    /**
     * Gets the deviceId value for this Device.
     * 
     * @return deviceId
     */
    public com.aspace.ftress.interfaces.ftress.DTO.device.DeviceId getDeviceId() {
        return deviceId;
    }


    /**
     * Sets the deviceId value for this Device.
     * 
     * @param deviceId
     */
    public void setDeviceId(com.aspace.ftress.interfaces.ftress.DTO.device.DeviceId deviceId) {
        this.deviceId = deviceId;
    }


    /**
     * Gets the deviceTypeCode value for this Device.
     * 
     * @return deviceTypeCode
     */
    public com.aspace.ftress.interfaces.ftress.DTO.device.DeviceTypeCode getDeviceTypeCode() {
        return deviceTypeCode;
    }


    /**
     * Sets the deviceTypeCode value for this Device.
     * 
     * @param deviceTypeCode
     */
    public void setDeviceTypeCode(com.aspace.ftress.interfaces.ftress.DTO.device.DeviceTypeCode deviceTypeCode) {
        this.deviceTypeCode = deviceTypeCode;
    }


    /**
     * Gets the expiryDate value for this Device.
     * 
     * @return expiryDate
     */
    public java.util.Calendar getExpiryDate() {
        return expiryDate;
    }


    /**
     * Sets the expiryDate value for this Device.
     * 
     * @param expiryDate
     */
    public void setExpiryDate(java.util.Calendar expiryDate) {
        this.expiryDate = expiryDate;
    }


    /**
     * Gets the issueNumber value for this Device.
     * 
     * @return issueNumber
     */
    public int getIssueNumber() {
        return issueNumber;
    }


    /**
     * Sets the issueNumber value for this Device.
     * 
     * @param issueNumber
     */
    public void setIssueNumber(int issueNumber) {
        this.issueNumber = issueNumber;
    }


    /**
     * Gets the neverExpires value for this Device.
     * 
     * @return neverExpires
     */
    public boolean isNeverExpires() {
        return neverExpires;
    }


    /**
     * Sets the neverExpires value for this Device.
     * 
     * @param neverExpires
     */
    public void setNeverExpires(boolean neverExpires) {
        this.neverExpires = neverExpires;
    }


    /**
     * Gets the serialNumber value for this Device.
     * 
     * @return serialNumber
     */
    public java.lang.String getSerialNumber() {
        return serialNumber;
    }


    /**
     * Sets the serialNumber value for this Device.
     * 
     * @param serialNumber
     */
    public void setSerialNumber(java.lang.String serialNumber) {
        this.serialNumber = serialNumber;
    }


    /**
     * Gets the startDate value for this Device.
     * 
     * @return startDate
     */
    public java.util.Calendar getStartDate() {
        return startDate;
    }


    /**
     * Sets the startDate value for this Device.
     * 
     * @param startDate
     */
    public void setStartDate(java.util.Calendar startDate) {
        this.startDate = startDate;
    }


    /**
     * Gets the status value for this Device.
     * 
     * @return status
     */
    public com.aspace.ftress.interfaces.ftress.DTO.device.DeviceStatus getStatus() {
        return status;
    }


    /**
     * Sets the status value for this Device.
     * 
     * @param status
     */
    public void setStatus(com.aspace.ftress.interfaces.ftress.DTO.device.DeviceStatus status) {
        this.status = status;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof Device)) return false;
        Device other = (Device) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.SDB==null && other.getSDB()==null) || 
             (this.SDB!=null &&
              java.util.Arrays.equals(this.SDB, other.getSDB()))) &&
            ((this.SDBKey==null && other.getSDBKey()==null) || 
             (this.SDBKey!=null &&
              this.SDBKey.equals(other.getSDBKey()))) &&
            ((this.addedDate==null && other.getAddedDate()==null) || 
             (this.addedDate!=null &&
              this.addedDate.equals(other.getAddedDate()))) &&
            ((this.deviceGroupCode==null && other.getDeviceGroupCode()==null) || 
             (this.deviceGroupCode!=null &&
              this.deviceGroupCode.equals(other.getDeviceGroupCode()))) &&
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
            this.neverExpires == other.isNeverExpires() &&
            ((this.serialNumber==null && other.getSerialNumber()==null) || 
             (this.serialNumber!=null &&
              this.serialNumber.equals(other.getSerialNumber()))) &&
            ((this.startDate==null && other.getStartDate()==null) || 
             (this.startDate!=null &&
              this.startDate.equals(other.getStartDate()))) &&
            ((this.status==null && other.getStatus()==null) || 
             (this.status!=null &&
              this.status.equals(other.getStatus())));
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
        if (getSDB() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getSDB());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getSDB(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getSDBKey() != null) {
            _hashCode += getSDBKey().hashCode();
        }
        if (getAddedDate() != null) {
            _hashCode += getAddedDate().hashCode();
        }
        if (getDeviceGroupCode() != null) {
            _hashCode += getDeviceGroupCode().hashCode();
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
        _hashCode += (isNeverExpires() ? Boolean.TRUE : Boolean.FALSE).hashCode();
        if (getSerialNumber() != null) {
            _hashCode += getSerialNumber().hashCode();
        }
        if (getStartDate() != null) {
            _hashCode += getStartDate().hashCode();
        }
        if (getStatus() != null) {
            _hashCode += getStatus().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(Device.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://device.DTO.ftress.interfaces.ftress.aspace.com", "Device"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("SDB");
        elemField.setXmlName(new javax.xml.namespace.QName("", "SDB"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/encoding/", "base64Binary"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("SDBKey");
        elemField.setXmlName(new javax.xml.namespace.QName("", "SDBKey"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/encoding/", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("addedDate");
        elemField.setXmlName(new javax.xml.namespace.QName("", "addedDate"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "dateTime"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("deviceGroupCode");
        elemField.setXmlName(new javax.xml.namespace.QName("", "deviceGroupCode"));
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
        elemField.setFieldName("neverExpires");
        elemField.setXmlName(new javax.xml.namespace.QName("", "neverExpires"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
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
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("status");
        elemField.setXmlName(new javax.xml.namespace.QName("", "status"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://device.DTO.ftress.interfaces.ftress.aspace.com", "DeviceStatus"));
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
