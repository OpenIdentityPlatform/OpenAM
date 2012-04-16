/**
 * AuthenticatorCloneException.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.aspace.ftress.interfaces.ftress.DTO.exception;

public class AuthenticatorCloneException  extends com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticatorException  implements java.io.Serializable {
    private java.lang.String adapterCode;

    public AuthenticatorCloneException() {
    }

    public AuthenticatorCloneException(
           int errorCode,
           com.aspace.ftress.interfaces.ftress.DTO.Parameter[] parameters,
           long reference,
           java.lang.String atCode,
           java.lang.String adapterCode) {
        super(
            errorCode,
            parameters,
            reference,
            atCode);
        this.adapterCode = adapterCode;
    }


    /**
     * Gets the adapterCode value for this AuthenticatorCloneException.
     * 
     * @return adapterCode
     */
    public java.lang.String getAdapterCode() {
        return adapterCode;
    }


    /**
     * Sets the adapterCode value for this AuthenticatorCloneException.
     * 
     * @param adapterCode
     */
    public void setAdapterCode(java.lang.String adapterCode) {
        this.adapterCode = adapterCode;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof AuthenticatorCloneException)) return false;
        AuthenticatorCloneException other = (AuthenticatorCloneException) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = super.equals(obj) && 
            ((this.adapterCode==null && other.getAdapterCode()==null) || 
             (this.adapterCode!=null &&
              this.adapterCode.equals(other.getAdapterCode())));
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
        if (getAdapterCode() != null) {
            _hashCode += getAdapterCode().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(AuthenticatorCloneException.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "AuthenticatorCloneException"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("adapterCode");
        elemField.setXmlName(new javax.xml.namespace.QName("", "adapterCode"));
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
