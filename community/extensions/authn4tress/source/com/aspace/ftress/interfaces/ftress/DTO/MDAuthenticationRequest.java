/**
 * MDAuthenticationRequest.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.aspace.ftress.interfaces.ftress.DTO;

public class MDAuthenticationRequest  extends com.aspace.ftress.interfaces.ftress.DTO.AuthenticationRequest  implements java.io.Serializable {
    private com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticationAnswer[] answers;

    private com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode;

    public MDAuthenticationRequest() {
    }

    public MDAuthenticationRequest(
           com.aspace.ftress.interfaces.ftress.DTO.AuthenticationRequestParameter[] auditedParameters,
           boolean authenticateNoSession,
           com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationTypeCode,
           com.aspace.ftress.interfaces.ftress.DTO.AuthenticationRequestParameter[] parameters,
           com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticationAnswer[] answers,
           com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode) {
        super(
            auditedParameters,
            authenticateNoSession,
            authenticationTypeCode,
            parameters);
        this.answers = answers;
        this.userCode = userCode;
    }


    /**
     * Gets the answers value for this MDAuthenticationRequest.
     * 
     * @return answers
     */
    public com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticationAnswer[] getAnswers() {
        return answers;
    }


    /**
     * Sets the answers value for this MDAuthenticationRequest.
     * 
     * @param answers
     */
    public void setAnswers(com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticationAnswer[] answers) {
        this.answers = answers;
    }


    /**
     * Gets the userCode value for this MDAuthenticationRequest.
     * 
     * @return userCode
     */
    public com.aspace.ftress.interfaces.ftress.DTO.UserCode getUserCode() {
        return userCode;
    }


    /**
     * Sets the userCode value for this MDAuthenticationRequest.
     * 
     * @param userCode
     */
    public void setUserCode(com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode) {
        this.userCode = userCode;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof MDAuthenticationRequest)) return false;
        MDAuthenticationRequest other = (MDAuthenticationRequest) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = super.equals(obj) && 
            ((this.answers==null && other.getAnswers()==null) || 
             (this.answers!=null &&
              java.util.Arrays.equals(this.answers, other.getAnswers()))) &&
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
        if (getAnswers() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getAnswers());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getAnswers(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getUserCode() != null) {
            _hashCode += getUserCode().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(MDAuthenticationRequest.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "MDAuthenticationRequest"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("answers");
        elemField.setXmlName(new javax.xml.namespace.QName("", "answers"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "MDAuthenticationAnswer"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("userCode");
        elemField.setXmlName(new javax.xml.namespace.QName("", "userCode"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"));
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
