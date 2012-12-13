/**
 * AuthenticationChallenge.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.aspace.ftress.interfaces.ftress.DTO.device;

public class AuthenticationChallenge  implements java.io.Serializable {
    private java.lang.String challenge;

    private java.lang.String challengeRequestErrorMessage;

    private int challengeRequestStatus;

    public AuthenticationChallenge() {
    }

    public AuthenticationChallenge(
           java.lang.String challenge,
           java.lang.String challengeRequestErrorMessage,
           int challengeRequestStatus) {
           this.challenge = challenge;
           this.challengeRequestErrorMessage = challengeRequestErrorMessage;
           this.challengeRequestStatus = challengeRequestStatus;
    }


    /**
     * Gets the challenge value for this AuthenticationChallenge.
     * 
     * @return challenge
     */
    public java.lang.String getChallenge() {
        return challenge;
    }


    /**
     * Sets the challenge value for this AuthenticationChallenge.
     * 
     * @param challenge
     */
    public void setChallenge(java.lang.String challenge) {
        this.challenge = challenge;
    }


    /**
     * Gets the challengeRequestErrorMessage value for this AuthenticationChallenge.
     * 
     * @return challengeRequestErrorMessage
     */
    public java.lang.String getChallengeRequestErrorMessage() {
        return challengeRequestErrorMessage;
    }


    /**
     * Sets the challengeRequestErrorMessage value for this AuthenticationChallenge.
     * 
     * @param challengeRequestErrorMessage
     */
    public void setChallengeRequestErrorMessage(java.lang.String challengeRequestErrorMessage) {
        this.challengeRequestErrorMessage = challengeRequestErrorMessage;
    }


    /**
     * Gets the challengeRequestStatus value for this AuthenticationChallenge.
     * 
     * @return challengeRequestStatus
     */
    public int getChallengeRequestStatus() {
        return challengeRequestStatus;
    }


    /**
     * Sets the challengeRequestStatus value for this AuthenticationChallenge.
     * 
     * @param challengeRequestStatus
     */
    public void setChallengeRequestStatus(int challengeRequestStatus) {
        this.challengeRequestStatus = challengeRequestStatus;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof AuthenticationChallenge)) return false;
        AuthenticationChallenge other = (AuthenticationChallenge) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.challenge==null && other.getChallenge()==null) || 
             (this.challenge!=null &&
              this.challenge.equals(other.getChallenge()))) &&
            ((this.challengeRequestErrorMessage==null && other.getChallengeRequestErrorMessage()==null) || 
             (this.challengeRequestErrorMessage!=null &&
              this.challengeRequestErrorMessage.equals(other.getChallengeRequestErrorMessage()))) &&
            this.challengeRequestStatus == other.getChallengeRequestStatus();
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
        if (getChallenge() != null) {
            _hashCode += getChallenge().hashCode();
        }
        if (getChallengeRequestErrorMessage() != null) {
            _hashCode += getChallengeRequestErrorMessage().hashCode();
        }
        _hashCode += getChallengeRequestStatus();
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(AuthenticationChallenge.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://device.DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationChallenge"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("challenge");
        elemField.setXmlName(new javax.xml.namespace.QName("", "challenge"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/encoding/", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("challengeRequestErrorMessage");
        elemField.setXmlName(new javax.xml.namespace.QName("", "challengeRequestErrorMessage"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/encoding/", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("challengeRequestStatus");
        elemField.setXmlName(new javax.xml.namespace.QName("", "challengeRequestStatus"));
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
