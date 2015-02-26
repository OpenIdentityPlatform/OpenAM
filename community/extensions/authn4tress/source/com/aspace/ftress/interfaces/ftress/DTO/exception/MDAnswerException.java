/**
 * MDAnswerException.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.aspace.ftress.interfaces.ftress.DTO.exception;

public class MDAnswerException  extends com.aspace.ftress.interfaces.ftress.DTO.exception.BusinessException  implements java.io.Serializable {
    private java.lang.String mdPromptCode;

    public MDAnswerException() {
    }

    public MDAnswerException(
           int errorCode,
           com.aspace.ftress.interfaces.ftress.DTO.Parameter[] parameters,
           long reference,
           java.lang.String mdPromptCode) {
        super(
            errorCode,
            parameters,
            reference);
        this.mdPromptCode = mdPromptCode;
    }


    /**
     * Gets the mdPromptCode value for this MDAnswerException.
     * 
     * @return mdPromptCode
     */
    public java.lang.String getMdPromptCode() {
        return mdPromptCode;
    }


    /**
     * Sets the mdPromptCode value for this MDAnswerException.
     * 
     * @param mdPromptCode
     */
    public void setMdPromptCode(java.lang.String mdPromptCode) {
        this.mdPromptCode = mdPromptCode;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof MDAnswerException)) return false;
        MDAnswerException other = (MDAnswerException) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = super.equals(obj) && 
            ((this.mdPromptCode==null && other.getMdPromptCode()==null) || 
             (this.mdPromptCode!=null &&
              this.mdPromptCode.equals(other.getMdPromptCode())));
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
        if (getMdPromptCode() != null) {
            _hashCode += getMdPromptCode().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(MDAnswerException.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "MDAnswerException"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("mdPromptCode");
        elemField.setXmlName(new javax.xml.namespace.QName("", "mdPromptCode"));
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
