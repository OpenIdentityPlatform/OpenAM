/**
 * AuthenticationStatistics.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.aspace.ftress.interfaces.ftress.DTO;

public class AuthenticationStatistics  implements java.io.Serializable {
    private java.lang.Integer challengeCount;

    private int consecutiveFailed;

    private int consecutiveSuccess;

    private com.aspace.ftress.interfaces.ftress.DTO.ChannelCode lastSuccessfulChannel;

    private java.util.Calendar lastSuccessfulDateTime;

    private com.aspace.ftress.interfaces.ftress.DTO.ChannelCode lastUnsuccessfulChannel;

    private java.util.Calendar lastUnsuccessfulDateTime;

    private int total;

    private int totalFailed;

    private int totalSuccessful;

    public AuthenticationStatistics() {
    }

    public AuthenticationStatistics(
           java.lang.Integer challengeCount,
           int consecutiveFailed,
           int consecutiveSuccess,
           com.aspace.ftress.interfaces.ftress.DTO.ChannelCode lastSuccessfulChannel,
           java.util.Calendar lastSuccessfulDateTime,
           com.aspace.ftress.interfaces.ftress.DTO.ChannelCode lastUnsuccessfulChannel,
           java.util.Calendar lastUnsuccessfulDateTime,
           int total,
           int totalFailed,
           int totalSuccessful) {
           this.challengeCount = challengeCount;
           this.consecutiveFailed = consecutiveFailed;
           this.consecutiveSuccess = consecutiveSuccess;
           this.lastSuccessfulChannel = lastSuccessfulChannel;
           this.lastSuccessfulDateTime = lastSuccessfulDateTime;
           this.lastUnsuccessfulChannel = lastUnsuccessfulChannel;
           this.lastUnsuccessfulDateTime = lastUnsuccessfulDateTime;
           this.total = total;
           this.totalFailed = totalFailed;
           this.totalSuccessful = totalSuccessful;
    }


    /**
     * Gets the challengeCount value for this AuthenticationStatistics.
     * 
     * @return challengeCount
     */
    public java.lang.Integer getChallengeCount() {
        return challengeCount;
    }


    /**
     * Sets the challengeCount value for this AuthenticationStatistics.
     * 
     * @param challengeCount
     */
    public void setChallengeCount(java.lang.Integer challengeCount) {
        this.challengeCount = challengeCount;
    }


    /**
     * Gets the consecutiveFailed value for this AuthenticationStatistics.
     * 
     * @return consecutiveFailed
     */
    public int getConsecutiveFailed() {
        return consecutiveFailed;
    }


    /**
     * Sets the consecutiveFailed value for this AuthenticationStatistics.
     * 
     * @param consecutiveFailed
     */
    public void setConsecutiveFailed(int consecutiveFailed) {
        this.consecutiveFailed = consecutiveFailed;
    }


    /**
     * Gets the consecutiveSuccess value for this AuthenticationStatistics.
     * 
     * @return consecutiveSuccess
     */
    public int getConsecutiveSuccess() {
        return consecutiveSuccess;
    }


    /**
     * Sets the consecutiveSuccess value for this AuthenticationStatistics.
     * 
     * @param consecutiveSuccess
     */
    public void setConsecutiveSuccess(int consecutiveSuccess) {
        this.consecutiveSuccess = consecutiveSuccess;
    }


    /**
     * Gets the lastSuccessfulChannel value for this AuthenticationStatistics.
     * 
     * @return lastSuccessfulChannel
     */
    public com.aspace.ftress.interfaces.ftress.DTO.ChannelCode getLastSuccessfulChannel() {
        return lastSuccessfulChannel;
    }


    /**
     * Sets the lastSuccessfulChannel value for this AuthenticationStatistics.
     * 
     * @param lastSuccessfulChannel
     */
    public void setLastSuccessfulChannel(com.aspace.ftress.interfaces.ftress.DTO.ChannelCode lastSuccessfulChannel) {
        this.lastSuccessfulChannel = lastSuccessfulChannel;
    }


    /**
     * Gets the lastSuccessfulDateTime value for this AuthenticationStatistics.
     * 
     * @return lastSuccessfulDateTime
     */
    public java.util.Calendar getLastSuccessfulDateTime() {
        return lastSuccessfulDateTime;
    }


    /**
     * Sets the lastSuccessfulDateTime value for this AuthenticationStatistics.
     * 
     * @param lastSuccessfulDateTime
     */
    public void setLastSuccessfulDateTime(java.util.Calendar lastSuccessfulDateTime) {
        this.lastSuccessfulDateTime = lastSuccessfulDateTime;
    }


    /**
     * Gets the lastUnsuccessfulChannel value for this AuthenticationStatistics.
     * 
     * @return lastUnsuccessfulChannel
     */
    public com.aspace.ftress.interfaces.ftress.DTO.ChannelCode getLastUnsuccessfulChannel() {
        return lastUnsuccessfulChannel;
    }


    /**
     * Sets the lastUnsuccessfulChannel value for this AuthenticationStatistics.
     * 
     * @param lastUnsuccessfulChannel
     */
    public void setLastUnsuccessfulChannel(com.aspace.ftress.interfaces.ftress.DTO.ChannelCode lastUnsuccessfulChannel) {
        this.lastUnsuccessfulChannel = lastUnsuccessfulChannel;
    }


    /**
     * Gets the lastUnsuccessfulDateTime value for this AuthenticationStatistics.
     * 
     * @return lastUnsuccessfulDateTime
     */
    public java.util.Calendar getLastUnsuccessfulDateTime() {
        return lastUnsuccessfulDateTime;
    }


    /**
     * Sets the lastUnsuccessfulDateTime value for this AuthenticationStatistics.
     * 
     * @param lastUnsuccessfulDateTime
     */
    public void setLastUnsuccessfulDateTime(java.util.Calendar lastUnsuccessfulDateTime) {
        this.lastUnsuccessfulDateTime = lastUnsuccessfulDateTime;
    }


    /**
     * Gets the total value for this AuthenticationStatistics.
     * 
     * @return total
     */
    public int getTotal() {
        return total;
    }


    /**
     * Sets the total value for this AuthenticationStatistics.
     * 
     * @param total
     */
    public void setTotal(int total) {
        this.total = total;
    }


    /**
     * Gets the totalFailed value for this AuthenticationStatistics.
     * 
     * @return totalFailed
     */
    public int getTotalFailed() {
        return totalFailed;
    }


    /**
     * Sets the totalFailed value for this AuthenticationStatistics.
     * 
     * @param totalFailed
     */
    public void setTotalFailed(int totalFailed) {
        this.totalFailed = totalFailed;
    }


    /**
     * Gets the totalSuccessful value for this AuthenticationStatistics.
     * 
     * @return totalSuccessful
     */
    public int getTotalSuccessful() {
        return totalSuccessful;
    }


    /**
     * Sets the totalSuccessful value for this AuthenticationStatistics.
     * 
     * @param totalSuccessful
     */
    public void setTotalSuccessful(int totalSuccessful) {
        this.totalSuccessful = totalSuccessful;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof AuthenticationStatistics)) return false;
        AuthenticationStatistics other = (AuthenticationStatistics) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.challengeCount==null && other.getChallengeCount()==null) || 
             (this.challengeCount!=null &&
              this.challengeCount.equals(other.getChallengeCount()))) &&
            this.consecutiveFailed == other.getConsecutiveFailed() &&
            this.consecutiveSuccess == other.getConsecutiveSuccess() &&
            ((this.lastSuccessfulChannel==null && other.getLastSuccessfulChannel()==null) || 
             (this.lastSuccessfulChannel!=null &&
              this.lastSuccessfulChannel.equals(other.getLastSuccessfulChannel()))) &&
            ((this.lastSuccessfulDateTime==null && other.getLastSuccessfulDateTime()==null) || 
             (this.lastSuccessfulDateTime!=null &&
              this.lastSuccessfulDateTime.equals(other.getLastSuccessfulDateTime()))) &&
            ((this.lastUnsuccessfulChannel==null && other.getLastUnsuccessfulChannel()==null) || 
             (this.lastUnsuccessfulChannel!=null &&
              this.lastUnsuccessfulChannel.equals(other.getLastUnsuccessfulChannel()))) &&
            ((this.lastUnsuccessfulDateTime==null && other.getLastUnsuccessfulDateTime()==null) || 
             (this.lastUnsuccessfulDateTime!=null &&
              this.lastUnsuccessfulDateTime.equals(other.getLastUnsuccessfulDateTime()))) &&
            this.total == other.getTotal() &&
            this.totalFailed == other.getTotalFailed() &&
            this.totalSuccessful == other.getTotalSuccessful();
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
        if (getChallengeCount() != null) {
            _hashCode += getChallengeCount().hashCode();
        }
        _hashCode += getConsecutiveFailed();
        _hashCode += getConsecutiveSuccess();
        if (getLastSuccessfulChannel() != null) {
            _hashCode += getLastSuccessfulChannel().hashCode();
        }
        if (getLastSuccessfulDateTime() != null) {
            _hashCode += getLastSuccessfulDateTime().hashCode();
        }
        if (getLastUnsuccessfulChannel() != null) {
            _hashCode += getLastUnsuccessfulChannel().hashCode();
        }
        if (getLastUnsuccessfulDateTime() != null) {
            _hashCode += getLastUnsuccessfulDateTime().hashCode();
        }
        _hashCode += getTotal();
        _hashCode += getTotalFailed();
        _hashCode += getTotalSuccessful();
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(AuthenticationStatistics.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationStatistics"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("challengeCount");
        elemField.setXmlName(new javax.xml.namespace.QName("", "challengeCount"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/encoding/", "int"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("consecutiveFailed");
        elemField.setXmlName(new javax.xml.namespace.QName("", "consecutiveFailed"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("consecutiveSuccess");
        elemField.setXmlName(new javax.xml.namespace.QName("", "consecutiveSuccess"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("lastSuccessfulChannel");
        elemField.setXmlName(new javax.xml.namespace.QName("", "lastSuccessfulChannel"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("lastSuccessfulDateTime");
        elemField.setXmlName(new javax.xml.namespace.QName("", "lastSuccessfulDateTime"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "dateTime"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("lastUnsuccessfulChannel");
        elemField.setXmlName(new javax.xml.namespace.QName("", "lastUnsuccessfulChannel"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("lastUnsuccessfulDateTime");
        elemField.setXmlName(new javax.xml.namespace.QName("", "lastUnsuccessfulDateTime"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "dateTime"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("total");
        elemField.setXmlName(new javax.xml.namespace.QName("", "total"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("totalFailed");
        elemField.setXmlName(new javax.xml.namespace.QName("", "totalFailed"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("totalSuccessful");
        elemField.setXmlName(new javax.xml.namespace.QName("", "totalSuccessful"));
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
