/**
 * MDAuthenticator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.aspace.ftress.interfaces.ftress.DTO;

public class MDAuthenticator  extends com.aspace.ftress.interfaces.ftress.DTO.Authenticator  implements java.io.Serializable {
    private boolean[] answerIsPresent;

    private com.aspace.ftress.interfaces.ftress.DTO.MDAnswer[] answers;

    private com.aspace.ftress.interfaces.ftress.DTO.MDPrompt[] prompts;

    private int promptsRequiredForCreation;

    public MDAuthenticator() {
    }

    public MDAuthenticator(
           com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationTypeCode,
           com.aspace.ftress.interfaces.ftress.DTO.ChannelsBlocked channelsBlocked,
           com.aspace.ftress.interfaces.ftress.DTO.AuthenticationStatistics statistics,
           java.lang.String status,
           com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode,
           java.util.Calendar validFrom,
           java.util.Calendar validTo,
           boolean[] answerIsPresent,
           com.aspace.ftress.interfaces.ftress.DTO.MDAnswer[] answers,
           com.aspace.ftress.interfaces.ftress.DTO.MDPrompt[] prompts,
           int promptsRequiredForCreation) {
        super(
            authenticationTypeCode,
            channelsBlocked,
            statistics,
            status,
            userCode,
            validFrom,
            validTo);
        this.answerIsPresent = answerIsPresent;
        this.answers = answers;
        this.prompts = prompts;
        this.promptsRequiredForCreation = promptsRequiredForCreation;
    }


    /**
     * Gets the answerIsPresent value for this MDAuthenticator.
     * 
     * @return answerIsPresent
     */
    public boolean[] getAnswerIsPresent() {
        return answerIsPresent;
    }


    /**
     * Sets the answerIsPresent value for this MDAuthenticator.
     * 
     * @param answerIsPresent
     */
    public void setAnswerIsPresent(boolean[] answerIsPresent) {
        this.answerIsPresent = answerIsPresent;
    }


    /**
     * Gets the answers value for this MDAuthenticator.
     * 
     * @return answers
     */
    public com.aspace.ftress.interfaces.ftress.DTO.MDAnswer[] getAnswers() {
        return answers;
    }


    /**
     * Sets the answers value for this MDAuthenticator.
     * 
     * @param answers
     */
    public void setAnswers(com.aspace.ftress.interfaces.ftress.DTO.MDAnswer[] answers) {
        this.answers = answers;
    }


    /**
     * Gets the prompts value for this MDAuthenticator.
     * 
     * @return prompts
     */
    public com.aspace.ftress.interfaces.ftress.DTO.MDPrompt[] getPrompts() {
        return prompts;
    }


    /**
     * Sets the prompts value for this MDAuthenticator.
     * 
     * @param prompts
     */
    public void setPrompts(com.aspace.ftress.interfaces.ftress.DTO.MDPrompt[] prompts) {
        this.prompts = prompts;
    }


    /**
     * Gets the promptsRequiredForCreation value for this MDAuthenticator.
     * 
     * @return promptsRequiredForCreation
     */
    public int getPromptsRequiredForCreation() {
        return promptsRequiredForCreation;
    }


    /**
     * Sets the promptsRequiredForCreation value for this MDAuthenticator.
     * 
     * @param promptsRequiredForCreation
     */
    public void setPromptsRequiredForCreation(int promptsRequiredForCreation) {
        this.promptsRequiredForCreation = promptsRequiredForCreation;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof MDAuthenticator)) return false;
        MDAuthenticator other = (MDAuthenticator) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = super.equals(obj) && 
            ((this.answerIsPresent==null && other.getAnswerIsPresent()==null) || 
             (this.answerIsPresent!=null &&
              java.util.Arrays.equals(this.answerIsPresent, other.getAnswerIsPresent()))) &&
            ((this.answers==null && other.getAnswers()==null) || 
             (this.answers!=null &&
              java.util.Arrays.equals(this.answers, other.getAnswers()))) &&
            ((this.prompts==null && other.getPrompts()==null) || 
             (this.prompts!=null &&
              java.util.Arrays.equals(this.prompts, other.getPrompts()))) &&
            this.promptsRequiredForCreation == other.getPromptsRequiredForCreation();
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
        if (getAnswerIsPresent() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getAnswerIsPresent());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getAnswerIsPresent(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
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
        _hashCode += getPromptsRequiredForCreation();
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(MDAuthenticator.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "MDAuthenticator"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("answerIsPresent");
        elemField.setXmlName(new javax.xml.namespace.QName("", "answerIsPresent"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("answers");
        elemField.setXmlName(new javax.xml.namespace.QName("", "answers"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "MDAnswer"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("prompts");
        elemField.setXmlName(new javax.xml.namespace.QName("", "prompts"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "MDPrompt"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("promptsRequiredForCreation");
        elemField.setXmlName(new javax.xml.namespace.QName("", "promptsRequiredForCreation"));
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

}
