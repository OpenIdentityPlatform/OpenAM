/**
 * NoFunctionPrivilegeException.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.aspace.ftress.interfaces.ftress.DTO.exception;

public class NoFunctionPrivilegeException  extends com.aspace.ftress.interfaces.ftress.DTO.exception.BusinessException  implements java.io.Serializable {
    private java.lang.String action;

    private java.lang.String requiredFunctionPrivilege;

    public NoFunctionPrivilegeException() {
    }

    public NoFunctionPrivilegeException(
           int errorCode,
           com.aspace.ftress.interfaces.ftress.DTO.Parameter[] parameters,
           long reference,
           java.lang.String action,
           java.lang.String requiredFunctionPrivilege) {
        super(
            errorCode,
            parameters,
            reference);
        this.action = action;
        this.requiredFunctionPrivilege = requiredFunctionPrivilege;
    }


    /**
     * Gets the action value for this NoFunctionPrivilegeException.
     * 
     * @return action
     */
    public java.lang.String getAction() {
        return action;
    }


    /**
     * Sets the action value for this NoFunctionPrivilegeException.
     * 
     * @param action
     */
    public void setAction(java.lang.String action) {
        this.action = action;
    }


    /**
     * Gets the requiredFunctionPrivilege value for this NoFunctionPrivilegeException.
     * 
     * @return requiredFunctionPrivilege
     */
    public java.lang.String getRequiredFunctionPrivilege() {
        return requiredFunctionPrivilege;
    }


    /**
     * Sets the requiredFunctionPrivilege value for this NoFunctionPrivilegeException.
     * 
     * @param requiredFunctionPrivilege
     */
    public void setRequiredFunctionPrivilege(java.lang.String requiredFunctionPrivilege) {
        this.requiredFunctionPrivilege = requiredFunctionPrivilege;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof NoFunctionPrivilegeException)) return false;
        NoFunctionPrivilegeException other = (NoFunctionPrivilegeException) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = super.equals(obj) && 
            ((this.action==null && other.getAction()==null) || 
             (this.action!=null &&
              this.action.equals(other.getAction()))) &&
            ((this.requiredFunctionPrivilege==null && other.getRequiredFunctionPrivilege()==null) || 
             (this.requiredFunctionPrivilege!=null &&
              this.requiredFunctionPrivilege.equals(other.getRequiredFunctionPrivilege())));
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
        if (getAction() != null) {
            _hashCode += getAction().hashCode();
        }
        if (getRequiredFunctionPrivilege() != null) {
            _hashCode += getRequiredFunctionPrivilege().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(NoFunctionPrivilegeException.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "NoFunctionPrivilegeException"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("action");
        elemField.setXmlName(new javax.xml.namespace.QName("", "action"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/encoding/", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("requiredFunctionPrivilege");
        elemField.setXmlName(new javax.xml.namespace.QName("", "requiredFunctionPrivilege"));
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
