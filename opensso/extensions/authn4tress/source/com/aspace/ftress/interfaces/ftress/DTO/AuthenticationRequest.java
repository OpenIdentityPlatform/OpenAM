/**
 * AuthenticationRequest.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.aspace.ftress.interfaces.ftress.DTO;

public abstract class AuthenticationRequest  implements java.io.Serializable {
    private com.aspace.ftress.interfaces.ftress.DTO.AuthenticationRequestParameter[] auditedParameters;

    private boolean authenticateNoSession;

    private com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationTypeCode;

    private com.aspace.ftress.interfaces.ftress.DTO.AuthenticationRequestParameter[] parameters;

    public AuthenticationRequest() {
    }

    public AuthenticationRequest(
           com.aspace.ftress.interfaces.ftress.DTO.AuthenticationRequestParameter[] auditedParameters,
           boolean authenticateNoSession,
           com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationTypeCode,
           com.aspace.ftress.interfaces.ftress.DTO.AuthenticationRequestParameter[] parameters) {
           this.auditedParameters = auditedParameters;
           this.authenticateNoSession = authenticateNoSession;
           this.authenticationTypeCode = authenticationTypeCode;
           this.parameters = parameters;
    }


    /**
     * Gets the auditedParameters value for this AuthenticationRequest.
     * 
     * @return auditedParameters
     */
    public com.aspace.ftress.interfaces.ftress.DTO.AuthenticationRequestParameter[] getAuditedParameters() {
        return auditedParameters;
    }


    /**
     * Sets the auditedParameters value for this AuthenticationRequest.
     * 
     * @param auditedParameters
     */
    public void setAuditedParameters(com.aspace.ftress.interfaces.ftress.DTO.AuthenticationRequestParameter[] auditedParameters) {
        this.auditedParameters = auditedParameters;
    }


    /**
     * Gets the authenticateNoSession value for this AuthenticationRequest.
     * 
     * @return authenticateNoSession
     */
    public boolean isAuthenticateNoSession() {
        return authenticateNoSession;
    }


    /**
     * Sets the authenticateNoSession value for this AuthenticationRequest.
     * 
     * @param authenticateNoSession
     */
    public void setAuthenticateNoSession(boolean authenticateNoSession) {
        this.authenticateNoSession = authenticateNoSession;
    }


    /**
     * Gets the authenticationTypeCode value for this AuthenticationRequest.
     * 
     * @return authenticationTypeCode
     */
    public com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode getAuthenticationTypeCode() {
        return authenticationTypeCode;
    }


    /**
     * Sets the authenticationTypeCode value for this AuthenticationRequest.
     * 
     * @param authenticationTypeCode
     */
    public void setAuthenticationTypeCode(com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationTypeCode) {
        this.authenticationTypeCode = authenticationTypeCode;
    }


    /**
     * Gets the parameters value for this AuthenticationRequest.
     * 
     * @return parameters
     */
    public com.aspace.ftress.interfaces.ftress.DTO.AuthenticationRequestParameter[] getParameters() {
        return parameters;
    }


    /**
     * Sets the parameters value for this AuthenticationRequest.
     * 
     * @param parameters
     */
    public void setParameters(com.aspace.ftress.interfaces.ftress.DTO.AuthenticationRequestParameter[] parameters) {
        this.parameters = parameters;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof AuthenticationRequest)) return false;
        AuthenticationRequest other = (AuthenticationRequest) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.auditedParameters==null && other.getAuditedParameters()==null) || 
             (this.auditedParameters!=null &&
              java.util.Arrays.equals(this.auditedParameters, other.getAuditedParameters()))) &&
            this.authenticateNoSession == other.isAuthenticateNoSession() &&
            ((this.authenticationTypeCode==null && other.getAuthenticationTypeCode()==null) || 
             (this.authenticationTypeCode!=null &&
              this.authenticationTypeCode.equals(other.getAuthenticationTypeCode()))) &&
            ((this.parameters==null && other.getParameters()==null) || 
             (this.parameters!=null &&
              java.util.Arrays.equals(this.parameters, other.getParameters())));
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
        if (getAuditedParameters() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getAuditedParameters());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getAuditedParameters(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        _hashCode += (isAuthenticateNoSession() ? Boolean.TRUE : Boolean.FALSE).hashCode();
        if (getAuthenticationTypeCode() != null) {
            _hashCode += getAuthenticationTypeCode().hashCode();
        }
        if (getParameters() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getParameters());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getParameters(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(AuthenticationRequest.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationRequest"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("auditedParameters");
        elemField.setXmlName(new javax.xml.namespace.QName("", "auditedParameters"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationRequestParameter"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("authenticateNoSession");
        elemField.setXmlName(new javax.xml.namespace.QName("", "authenticateNoSession"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("authenticationTypeCode");
        elemField.setXmlName(new javax.xml.namespace.QName("", "authenticationTypeCode"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTypeCode"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("parameters");
        elemField.setXmlName(new javax.xml.namespace.QName("", "parameters"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationRequestParameter"));
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
