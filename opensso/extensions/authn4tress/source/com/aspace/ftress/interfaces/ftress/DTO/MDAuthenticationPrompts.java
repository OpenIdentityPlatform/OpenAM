/**
 * MDAuthenticationPrompts.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.aspace.ftress.interfaces.ftress.DTO;

public class MDAuthenticationPrompts  implements java.io.Serializable {
    private int answersRequired;

    private com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticationPrompt[] prompts;

    public MDAuthenticationPrompts() {
    }

    public MDAuthenticationPrompts(
           int answersRequired,
           com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticationPrompt[] prompts) {
           this.answersRequired = answersRequired;
           this.prompts = prompts;
    }


    /**
     * Gets the answersRequired value for this MDAuthenticationPrompts.
     * 
     * @return answersRequired
     */
    public int getAnswersRequired() {
        return answersRequired;
    }


    /**
     * Sets the answersRequired value for this MDAuthenticationPrompts.
     * 
     * @param answersRequired
     */
    public void setAnswersRequired(int answersRequired) {
        this.answersRequired = answersRequired;
    }


    /**
     * Gets the prompts value for this MDAuthenticationPrompts.
     * 
     * @return prompts
     */
    public com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticationPrompt[] getPrompts() {
        return prompts;
    }


    /**
     * Sets the prompts value for this MDAuthenticationPrompts.
     * 
     * @param prompts
     */
    public void setPrompts(com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticationPrompt[] prompts) {
        this.prompts = prompts;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof MDAuthenticationPrompts)) return false;
        MDAuthenticationPrompts other = (MDAuthenticationPrompts) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            this.answersRequired == other.getAnswersRequired() &&
            ((this.prompts==null && other.getPrompts()==null) || 
             (this.prompts!=null &&
              java.util.Arrays.equals(this.prompts, other.getPrompts())));
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
        _hashCode += getAnswersRequired();
        if (getPrompts() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getPrompts());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getPrompts(), i);
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
        new org.apache.axis.description.TypeDesc(MDAuthenticationPrompts.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "MDAuthenticationPrompts"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("answersRequired");
        elemField.setXmlName(new javax.xml.namespace.QName("", "answersRequired"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("prompts");
        elemField.setXmlName(new javax.xml.namespace.QName("", "prompts"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "MDAuthenticationPrompt"));
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
