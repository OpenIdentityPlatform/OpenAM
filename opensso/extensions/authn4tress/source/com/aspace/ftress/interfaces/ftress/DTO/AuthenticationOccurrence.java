/**
 * AuthenticationOccurrence.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.aspace.ftress.interfaces.ftress.DTO;

public class AuthenticationOccurrence  implements java.io.Serializable {
    private java.util.Calendar authenticationTime;

    private com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationType;

    public AuthenticationOccurrence() {
    }

    public AuthenticationOccurrence(
           java.util.Calendar authenticationTime,
           com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationType) {
           this.authenticationTime = authenticationTime;
           this.authenticationType = authenticationType;
    }


    /**
     * Gets the authenticationTime value for this AuthenticationOccurrence.
     * 
     * @return authenticationTime
     */
    public java.util.Calendar getAuthenticationTime() {
        return authenticationTime;
    }


    /**
     * Sets the authenticationTime value for this AuthenticationOccurrence.
     * 
     * @param authenticationTime
     */
    public void setAuthenticationTime(java.util.Calendar authenticationTime) {
        this.authenticationTime = authenticationTime;
    }


    /**
     * Gets the authenticationType value for this AuthenticationOccurrence.
     * 
     * @return authenticationType
     */
    public com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode getAuthenticationType() {
        return authenticationType;
    }


    /**
     * Sets the authenticationType value for this AuthenticationOccurrence.
     * 
     * @param authenticationType
     */
    public void setAuthenticationType(com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationType) {
        this.authenticationType = authenticationType;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof AuthenticationOccurrence)) return false;
        AuthenticationOccurrence other = (AuthenticationOccurrence) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.authenticationTime==null && other.getAuthenticationTime()==null) || 
             (this.authenticationTime!=null &&
              this.authenticationTime.equals(other.getAuthenticationTime()))) &&
            ((this.authenticationType==null && other.getAuthenticationType()==null) || 
             (this.authenticationType!=null &&
              this.authenticationType.equals(other.getAuthenticationType())));
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
        if (getAuthenticationTime() != null) {
            _hashCode += getAuthenticationTime().hashCode();
        }
        if (getAuthenticationType() != null) {
            _hashCode += getAuthenticationType().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(AuthenticationOccurrence.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationOccurrence"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("authenticationTime");
        elemField.setXmlName(new javax.xml.namespace.QName("", "authenticationTime"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "dateTime"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("authenticationType");
        elemField.setXmlName(new javax.xml.namespace.QName("", "authenticationType"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTypeCode"));
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
