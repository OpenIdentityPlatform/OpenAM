/**
 * ALSIInvalidException.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.aspace.ftress.interfaces.ftress.DTO.exception;

public class ALSIInvalidException  extends com.aspace.ftress.interfaces.ftress.DTO.exception.BusinessException  implements java.io.Serializable {
    private java.lang.String ALSI;

    private boolean indirectALSI;

    public ALSIInvalidException() {
    }

    public ALSIInvalidException(
           int errorCode,
           com.aspace.ftress.interfaces.ftress.DTO.Parameter[] parameters,
           long reference,
           java.lang.String ALSI,
           boolean indirectALSI) {
        super(
            errorCode,
            parameters,
            reference);
        this.ALSI = ALSI;
        this.indirectALSI = indirectALSI;
    }


    /**
     * Gets the ALSI value for this ALSIInvalidException.
     * 
     * @return ALSI
     */
    public java.lang.String getALSI() {
        return ALSI;
    }


    /**
     * Sets the ALSI value for this ALSIInvalidException.
     * 
     * @param ALSI
     */
    public void setALSI(java.lang.String ALSI) {
        this.ALSI = ALSI;
    }


    /**
     * Gets the indirectALSI value for this ALSIInvalidException.
     * 
     * @return indirectALSI
     */
    public boolean isIndirectALSI() {
        return indirectALSI;
    }


    /**
     * Sets the indirectALSI value for this ALSIInvalidException.
     * 
     * @param indirectALSI
     */
    public void setIndirectALSI(boolean indirectALSI) {
        this.indirectALSI = indirectALSI;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof ALSIInvalidException)) return false;
        ALSIInvalidException other = (ALSIInvalidException) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = super.equals(obj) && 
            ((this.ALSI==null && other.getALSI()==null) || 
             (this.ALSI!=null &&
              this.ALSI.equals(other.getALSI()))) &&
            this.indirectALSI == other.isIndirectALSI();
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
        if (getALSI() != null) {
            _hashCode += getALSI().hashCode();
        }
        _hashCode += (isIndirectALSI() ? Boolean.TRUE : Boolean.FALSE).hashCode();
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(ALSIInvalidException.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ALSIInvalidException"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("ALSI");
        elemField.setXmlName(new javax.xml.namespace.QName("", "ALSI"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/encoding/", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("indirectALSI");
        elemField.setXmlName(new javax.xml.namespace.QName("", "indirectALSI"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
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
