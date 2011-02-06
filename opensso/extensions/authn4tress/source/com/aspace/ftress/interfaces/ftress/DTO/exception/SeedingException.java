/**
 * SeedingException.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.aspace.ftress.interfaces.ftress.DTO.exception;

public class SeedingException  extends com.aspace.ftress.interfaces.ftress.DTO.exception.BusinessException  implements java.io.Serializable {
    private java.lang.String MDPromptCode;

    private java.lang.String authenticationTypeCode;

    private int[] seedPositionsProvided;

    private int seedsRequired;

    public SeedingException() {
    }

    public SeedingException(
           int errorCode,
           com.aspace.ftress.interfaces.ftress.DTO.Parameter[] parameters,
           long reference,
           java.lang.String MDPromptCode,
           java.lang.String authenticationTypeCode,
           int[] seedPositionsProvided,
           int seedsRequired) {
        super(
            errorCode,
            parameters,
            reference);
        this.MDPromptCode = MDPromptCode;
        this.authenticationTypeCode = authenticationTypeCode;
        this.seedPositionsProvided = seedPositionsProvided;
        this.seedsRequired = seedsRequired;
    }


    /**
     * Gets the MDPromptCode value for this SeedingException.
     * 
     * @return MDPromptCode
     */
    public java.lang.String getMDPromptCode() {
        return MDPromptCode;
    }


    /**
     * Sets the MDPromptCode value for this SeedingException.
     * 
     * @param MDPromptCode
     */
    public void setMDPromptCode(java.lang.String MDPromptCode) {
        this.MDPromptCode = MDPromptCode;
    }


    /**
     * Gets the authenticationTypeCode value for this SeedingException.
     * 
     * @return authenticationTypeCode
     */
    public java.lang.String getAuthenticationTypeCode() {
        return authenticationTypeCode;
    }


    /**
     * Sets the authenticationTypeCode value for this SeedingException.
     * 
     * @param authenticationTypeCode
     */
    public void setAuthenticationTypeCode(java.lang.String authenticationTypeCode) {
        this.authenticationTypeCode = authenticationTypeCode;
    }


    /**
     * Gets the seedPositionsProvided value for this SeedingException.
     * 
     * @return seedPositionsProvided
     */
    public int[] getSeedPositionsProvided() {
        return seedPositionsProvided;
    }


    /**
     * Sets the seedPositionsProvided value for this SeedingException.
     * 
     * @param seedPositionsProvided
     */
    public void setSeedPositionsProvided(int[] seedPositionsProvided) {
        this.seedPositionsProvided = seedPositionsProvided;
    }


    /**
     * Gets the seedsRequired value for this SeedingException.
     * 
     * @return seedsRequired
     */
    public int getSeedsRequired() {
        return seedsRequired;
    }


    /**
     * Sets the seedsRequired value for this SeedingException.
     * 
     * @param seedsRequired
     */
    public void setSeedsRequired(int seedsRequired) {
        this.seedsRequired = seedsRequired;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof SeedingException)) return false;
        SeedingException other = (SeedingException) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = super.equals(obj) && 
            ((this.MDPromptCode==null && other.getMDPromptCode()==null) || 
             (this.MDPromptCode!=null &&
              this.MDPromptCode.equals(other.getMDPromptCode()))) &&
            ((this.authenticationTypeCode==null && other.getAuthenticationTypeCode()==null) || 
             (this.authenticationTypeCode!=null &&
              this.authenticationTypeCode.equals(other.getAuthenticationTypeCode()))) &&
            ((this.seedPositionsProvided==null && other.getSeedPositionsProvided()==null) || 
             (this.seedPositionsProvided!=null &&
              java.util.Arrays.equals(this.seedPositionsProvided, other.getSeedPositionsProvided()))) &&
            this.seedsRequired == other.getSeedsRequired();
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
        if (getMDPromptCode() != null) {
            _hashCode += getMDPromptCode().hashCode();
        }
        if (getAuthenticationTypeCode() != null) {
            _hashCode += getAuthenticationTypeCode().hashCode();
        }
        if (getSeedPositionsProvided() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getSeedPositionsProvided());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getSeedPositionsProvided(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        _hashCode += getSeedsRequired();
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(SeedingException.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "SeedingException"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("MDPromptCode");
        elemField.setXmlName(new javax.xml.namespace.QName("", "MDPromptCode"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/encoding/", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("authenticationTypeCode");
        elemField.setXmlName(new javax.xml.namespace.QName("", "authenticationTypeCode"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/encoding/", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("seedPositionsProvided");
        elemField.setXmlName(new javax.xml.namespace.QName("", "seedPositionsProvided"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("seedsRequired");
        elemField.setXmlName(new javax.xml.namespace.QName("", "seedsRequired"));
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


    /**
     * Writes the exception data to the faultDetails
     */
    public void writeDetails(javax.xml.namespace.QName qname, org.apache.axis.encoding.SerializationContext context) throws java.io.IOException {
        context.serialize(qname, null, this);
    }
}
