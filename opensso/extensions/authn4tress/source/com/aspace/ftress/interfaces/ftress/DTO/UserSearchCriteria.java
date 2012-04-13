/**
 * UserSearchCriteria.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.aspace.ftress.interfaces.ftress.DTO;

public class UserSearchCriteria  extends com.aspace.ftress.interfaces.ftress.DTO.SearchCriteria  implements java.io.Serializable {
    private com.aspace.ftress.interfaces.ftress.DTO.Attribute[] attributeCriteria;

    private com.aspace.ftress.interfaces.ftress.DTO.GroupCode[] onGroupCodes;

    public UserSearchCriteria() {
    }

    public UserSearchCriteria(
           com.aspace.ftress.interfaces.ftress.DTO.Attribute[] attributeCriteria,
           com.aspace.ftress.interfaces.ftress.DTO.GroupCode[] onGroupCodes) {
        this.attributeCriteria = attributeCriteria;
        this.onGroupCodes = onGroupCodes;
    }


    /**
     * Gets the attributeCriteria value for this UserSearchCriteria.
     * 
     * @return attributeCriteria
     */
    public com.aspace.ftress.interfaces.ftress.DTO.Attribute[] getAttributeCriteria() {
        return attributeCriteria;
    }


    /**
     * Sets the attributeCriteria value for this UserSearchCriteria.
     * 
     * @param attributeCriteria
     */
    public void setAttributeCriteria(com.aspace.ftress.interfaces.ftress.DTO.Attribute[] attributeCriteria) {
        this.attributeCriteria = attributeCriteria;
    }


    /**
     * Gets the onGroupCodes value for this UserSearchCriteria.
     * 
     * @return onGroupCodes
     */
    public com.aspace.ftress.interfaces.ftress.DTO.GroupCode[] getOnGroupCodes() {
        return onGroupCodes;
    }


    /**
     * Sets the onGroupCodes value for this UserSearchCriteria.
     * 
     * @param onGroupCodes
     */
    public void setOnGroupCodes(com.aspace.ftress.interfaces.ftress.DTO.GroupCode[] onGroupCodes) {
        this.onGroupCodes = onGroupCodes;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof UserSearchCriteria)) return false;
        UserSearchCriteria other = (UserSearchCriteria) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = super.equals(obj) && 
            ((this.attributeCriteria==null && other.getAttributeCriteria()==null) || 
             (this.attributeCriteria!=null &&
              java.util.Arrays.equals(this.attributeCriteria, other.getAttributeCriteria()))) &&
            ((this.onGroupCodes==null && other.getOnGroupCodes()==null) || 
             (this.onGroupCodes!=null &&
              java.util.Arrays.equals(this.onGroupCodes, other.getOnGroupCodes())));
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
        if (getAttributeCriteria() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getAttributeCriteria());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getAttributeCriteria(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getOnGroupCodes() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getOnGroupCodes());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getOnGroupCodes(), i);
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
        new org.apache.axis.description.TypeDesc(UserSearchCriteria.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserSearchCriteria"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("attributeCriteria");
        elemField.setXmlName(new javax.xml.namespace.QName("", "attributeCriteria"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "Attribute"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("onGroupCodes");
        elemField.setXmlName(new javax.xml.namespace.QName("", "onGroupCodes"));
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
