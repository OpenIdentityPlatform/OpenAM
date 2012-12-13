/**
 * AuthenticationResponse.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.aspace.ftress.interfaces.ftress.DTO;

public class AuthenticationResponse  implements java.io.Serializable {
    private com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi;

    private int expiryThreshold;

    private com.aspace.ftress.interfaces.ftress.DTO.MDPromptCode[] failedPrompts;

    private java.lang.String message;

    private com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponseParameter[] parameters;

    private int reason;

    private int response;

    private com.aspace.ftress.interfaces.ftress.DTO.AuthenticationStatistics statistics;

    private java.lang.String status;

    private com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode;

    public AuthenticationResponse() {
    }

    public AuthenticationResponse(
           com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi,
           int expiryThreshold,
           com.aspace.ftress.interfaces.ftress.DTO.MDPromptCode[] failedPrompts,
           java.lang.String message,
           com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponseParameter[] parameters,
           int reason,
           int response,
           com.aspace.ftress.interfaces.ftress.DTO.AuthenticationStatistics statistics,
           java.lang.String status,
           com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode) {
           this.alsi = alsi;
           this.expiryThreshold = expiryThreshold;
           this.failedPrompts = failedPrompts;
           this.message = message;
           this.parameters = parameters;
           this.reason = reason;
           this.response = response;
           this.statistics = statistics;
           this.status = status;
           this.userCode = userCode;
    }


    /**
     * Gets the alsi value for this AuthenticationResponse.
     * 
     * @return alsi
     */
    public com.aspace.ftress.interfaces.ftress.DTO.ALSI getAlsi() {
        return alsi;
    }


    /**
     * Sets the alsi value for this AuthenticationResponse.
     * 
     * @param alsi
     */
    public void setAlsi(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi) {
        this.alsi = alsi;
    }


    /**
     * Gets the expiryThreshold value for this AuthenticationResponse.
     * 
     * @return expiryThreshold
     */
    public int getExpiryThreshold() {
        return expiryThreshold;
    }


    /**
     * Sets the expiryThreshold value for this AuthenticationResponse.
     * 
     * @param expiryThreshold
     */
    public void setExpiryThreshold(int expiryThreshold) {
        this.expiryThreshold = expiryThreshold;
    }


    /**
     * Gets the failedPrompts value for this AuthenticationResponse.
     * 
     * @return failedPrompts
     */
    public com.aspace.ftress.interfaces.ftress.DTO.MDPromptCode[] getFailedPrompts() {
        return failedPrompts;
    }


    /**
     * Sets the failedPrompts value for this AuthenticationResponse.
     * 
     * @param failedPrompts
     */
    public void setFailedPrompts(com.aspace.ftress.interfaces.ftress.DTO.MDPromptCode[] failedPrompts) {
        this.failedPrompts = failedPrompts;
    }


    /**
     * Gets the message value for this AuthenticationResponse.
     * 
     * @return message
     */
    public java.lang.String getMessage() {
        return message;
    }


    /**
     * Sets the message value for this AuthenticationResponse.
     * 
     * @param message
     */
    public void setMessage(java.lang.String message) {
        this.message = message;
    }


    /**
     * Gets the parameters value for this AuthenticationResponse.
     * 
     * @return parameters
     */
    public com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponseParameter[] getParameters() {
        return parameters;
    }


    /**
     * Sets the parameters value for this AuthenticationResponse.
     * 
     * @param parameters
     */
    public void setParameters(com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponseParameter[] parameters) {
        this.parameters = parameters;
    }


    /**
     * Gets the reason value for this AuthenticationResponse.
     * 
     * @return reason
     */
    public int getReason() {
        return reason;
    }


    /**
     * Sets the reason value for this AuthenticationResponse.
     * 
     * @param reason
     */
    public void setReason(int reason) {
        this.reason = reason;
    }


    /**
     * Gets the response value for this AuthenticationResponse.
     * 
     * @return response
     */
    public int getResponse() {
        return response;
    }


    /**
     * Sets the response value for this AuthenticationResponse.
     * 
     * @param response
     */
    public void setResponse(int response) {
        this.response = response;
    }


    /**
     * Gets the statistics value for this AuthenticationResponse.
     * 
     * @return statistics
     */
    public com.aspace.ftress.interfaces.ftress.DTO.AuthenticationStatistics getStatistics() {
        return statistics;
    }


    /**
     * Sets the statistics value for this AuthenticationResponse.
     * 
     * @param statistics
     */
    public void setStatistics(com.aspace.ftress.interfaces.ftress.DTO.AuthenticationStatistics statistics) {
        this.statistics = statistics;
    }


    /**
     * Gets the status value for this AuthenticationResponse.
     * 
     * @return status
     */
    public java.lang.String getStatus() {
        return status;
    }


    /**
     * Sets the status value for this AuthenticationResponse.
     * 
     * @param status
     */
    public void setStatus(java.lang.String status) {
        this.status = status;
    }


    /**
     * Gets the userCode value for this AuthenticationResponse.
     * 
     * @return userCode
     */
    public com.aspace.ftress.interfaces.ftress.DTO.UserCode getUserCode() {
        return userCode;
    }


    /**
     * Sets the userCode value for this AuthenticationResponse.
     * 
     * @param userCode
     */
    public void setUserCode(com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode) {
        this.userCode = userCode;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof AuthenticationResponse)) return false;
        AuthenticationResponse other = (AuthenticationResponse) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.alsi==null && other.getAlsi()==null) || 
             (this.alsi!=null &&
              this.alsi.equals(other.getAlsi()))) &&
            this.expiryThreshold == other.getExpiryThreshold() &&
            ((this.failedPrompts==null && other.getFailedPrompts()==null) || 
             (this.failedPrompts!=null &&
              java.util.Arrays.equals(this.failedPrompts, other.getFailedPrompts()))) &&
            ((this.message==null && other.getMessage()==null) || 
             (this.message!=null &&
              this.message.equals(other.getMessage()))) &&
            ((this.parameters==null && other.getParameters()==null) || 
             (this.parameters!=null &&
              java.util.Arrays.equals(this.parameters, other.getParameters()))) &&
            this.reason == other.getReason() &&
            this.response == other.getResponse() &&
            ((this.statistics==null && other.getStatistics()==null) || 
             (this.statistics!=null &&
              this.statistics.equals(other.getStatistics()))) &&
            ((this.status==null && other.getStatus()==null) || 
             (this.status!=null &&
              this.status.equals(other.getStatus()))) &&
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
        int _hashCode = 1;
        if (getAlsi() != null) {
            _hashCode += getAlsi().hashCode();
        }
        _hashCode += getExpiryThreshold();
        if (getFailedPrompts() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getFailedPrompts());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getFailedPrompts(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getMessage() != null) {
            _hashCode += getMessage().hashCode();
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
        _hashCode += getReason();
        _hashCode += getResponse();
        if (getStatistics() != null) {
            _hashCode += getStatistics().hashCode();
        }
        if (getStatus() != null) {
            _hashCode += getStatus().hashCode();
        }
        if (getUserCode() != null) {
            _hashCode += getUserCode().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(AuthenticationResponse.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationResponse"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("alsi");
        elemField.setXmlName(new javax.xml.namespace.QName("", "alsi"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("expiryThreshold");
        elemField.setXmlName(new javax.xml.namespace.QName("", "expiryThreshold"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("failedPrompts");
        elemField.setXmlName(new javax.xml.namespace.QName("", "failedPrompts"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "MDPromptCode"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("message");
        elemField.setXmlName(new javax.xml.namespace.QName("", "message"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/encoding/", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("parameters");
        elemField.setXmlName(new javax.xml.namespace.QName("", "parameters"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationResponseParameter"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("reason");
        elemField.setXmlName(new javax.xml.namespace.QName("", "reason"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("response");
        elemField.setXmlName(new javax.xml.namespace.QName("", "response"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("statistics");
        elemField.setXmlName(new javax.xml.namespace.QName("", "statistics"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationStatistics"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("status");
        elemField.setXmlName(new javax.xml.namespace.QName("", "status"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/encoding/", "string"));
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
