/**
 * PrimeUser.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.aspace.ftress.interfaces.ftress.DTO;

public class PrimeUser  extends com.aspace.ftress.interfaces.ftress.DTO.User  implements java.io.Serializable {
    private com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode primeUserAuthenticationTypeCode;

    private java.lang.String[] primeUserFunctionSets;

    private com.aspace.ftress.interfaces.ftress.DTO.GroupCode primeUserGroup;

    private com.aspace.ftress.interfaces.ftress.DTO.GroupCode primeUserSubGroup;

    public PrimeUser() {
    }

    public PrimeUser(
           com.aspace.ftress.interfaces.ftress.DTO.Attribute[] attributes,
           com.aspace.ftress.interfaces.ftress.DTO.UserCode code,
           com.aspace.ftress.interfaces.ftress.DTO.GroupCode groupCode,
           java.util.Calendar startDate,
           java.lang.String status,
           com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode primeUserAuthenticationTypeCode,
           java.lang.String[] primeUserFunctionSets,
           com.aspace.ftress.interfaces.ftress.DTO.GroupCode primeUserGroup,
           com.aspace.ftress.interfaces.ftress.DTO.GroupCode primeUserSubGroup) {
        super(
            attributes,
            code,
            groupCode,
            startDate,
            status);
        this.primeUserAuthenticationTypeCode = primeUserAuthenticationTypeCode;
        this.primeUserFunctionSets = primeUserFunctionSets;
        this.primeUserGroup = primeUserGroup;
        this.primeUserSubGroup = primeUserSubGroup;
    }


    /**
     * Gets the primeUserAuthenticationTypeCode value for this PrimeUser.
     * 
     * @return primeUserAuthenticationTypeCode
     */
    public com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode getPrimeUserAuthenticationTypeCode() {
        return primeUserAuthenticationTypeCode;
    }


    /**
     * Sets the primeUserAuthenticationTypeCode value for this PrimeUser.
     * 
     * @param primeUserAuthenticationTypeCode
     */
    public void setPrimeUserAuthenticationTypeCode(com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode primeUserAuthenticationTypeCode) {
        this.primeUserAuthenticationTypeCode = primeUserAuthenticationTypeCode;
    }


    /**
     * Gets the primeUserFunctionSets value for this PrimeUser.
     * 
     * @return primeUserFunctionSets
     */
    public java.lang.String[] getPrimeUserFunctionSets() {
        return primeUserFunctionSets;
    }


    /**
     * Sets the primeUserFunctionSets value for this PrimeUser.
     * 
     * @param primeUserFunctionSets
     */
    public void setPrimeUserFunctionSets(java.lang.String[] primeUserFunctionSets) {
        this.primeUserFunctionSets = primeUserFunctionSets;
    }


    /**
     * Gets the primeUserGroup value for this PrimeUser.
     * 
     * @return primeUserGroup
     */
    public com.aspace.ftress.interfaces.ftress.DTO.GroupCode getPrimeUserGroup() {
        return primeUserGroup;
    }


    /**
     * Sets the primeUserGroup value for this PrimeUser.
     * 
     * @param primeUserGroup
     */
    public void setPrimeUserGroup(com.aspace.ftress.interfaces.ftress.DTO.GroupCode primeUserGroup) {
        this.primeUserGroup = primeUserGroup;
    }


    /**
     * Gets the primeUserSubGroup value for this PrimeUser.
     * 
     * @return primeUserSubGroup
     */
    public com.aspace.ftress.interfaces.ftress.DTO.GroupCode getPrimeUserSubGroup() {
        return primeUserSubGroup;
    }


    /**
     * Sets the primeUserSubGroup value for this PrimeUser.
     * 
     * @param primeUserSubGroup
     */
    public void setPrimeUserSubGroup(com.aspace.ftress.interfaces.ftress.DTO.GroupCode primeUserSubGroup) {
        this.primeUserSubGroup = primeUserSubGroup;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof PrimeUser)) return false;
        PrimeUser other = (PrimeUser) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = super.equals(obj) && 
            ((this.primeUserAuthenticationTypeCode==null && other.getPrimeUserAuthenticationTypeCode()==null) || 
             (this.primeUserAuthenticationTypeCode!=null &&
              this.primeUserAuthenticationTypeCode.equals(other.getPrimeUserAuthenticationTypeCode()))) &&
            ((this.primeUserFunctionSets==null && other.getPrimeUserFunctionSets()==null) || 
             (this.primeUserFunctionSets!=null &&
              java.util.Arrays.equals(this.primeUserFunctionSets, other.getPrimeUserFunctionSets()))) &&
            ((this.primeUserGroup==null && other.getPrimeUserGroup()==null) || 
             (this.primeUserGroup!=null &&
              this.primeUserGroup.equals(other.getPrimeUserGroup()))) &&
            ((this.primeUserSubGroup==null && other.getPrimeUserSubGroup()==null) || 
             (this.primeUserSubGroup!=null &&
              this.primeUserSubGroup.equals(other.getPrimeUserSubGroup())));
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
        if (getPrimeUserAuthenticationTypeCode() != null) {
            _hashCode += getPrimeUserAuthenticationTypeCode().hashCode();
        }
        if (getPrimeUserFunctionSets() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getPrimeUserFunctionSets());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getPrimeUserFunctionSets(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getPrimeUserGroup() != null) {
            _hashCode += getPrimeUserGroup().hashCode();
        }
        if (getPrimeUserSubGroup() != null) {
            _hashCode += getPrimeUserSubGroup().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(PrimeUser.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "PrimeUser"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("primeUserAuthenticationTypeCode");
        elemField.setXmlName(new javax.xml.namespace.QName("", "primeUserAuthenticationTypeCode"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTypeCode"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("primeUserFunctionSets");
        elemField.setXmlName(new javax.xml.namespace.QName("", "primeUserFunctionSets"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/encoding/", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("primeUserGroup");
        elemField.setXmlName(new javax.xml.namespace.QName("", "primeUserGroup"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "GroupCode"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("primeUserSubGroup");
        elemField.setXmlName(new javax.xml.namespace.QName("", "primeUserSubGroup"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "GroupCode"));
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
