/**
 * ConstraintFailure.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.aspace.ftress.interfaces.ftress.DTO;

public class ConstraintFailure  implements java.io.Serializable {
    private java.lang.String entity;

    private java.lang.String entityValue;

    private com.aspace.ftress.interfaces.ftress.DTO.Parameter[] parameters;

    private int reason;

    public ConstraintFailure() {
    }

    public ConstraintFailure(
           java.lang.String entity,
           java.lang.String entityValue,
           com.aspace.ftress.interfaces.ftress.DTO.Parameter[] parameters,
           int reason) {
           this.entity = entity;
           this.entityValue = entityValue;
           this.parameters = parameters;
           this.reason = reason;
    }


    /**
     * Gets the entity value for this ConstraintFailure.
     * 
     * @return entity
     */
    public java.lang.String getEntity() {
        return entity;
    }


    /**
     * Sets the entity value for this ConstraintFailure.
     * 
     * @param entity
     */
    public void setEntity(java.lang.String entity) {
        this.entity = entity;
    }


    /**
     * Gets the entityValue value for this ConstraintFailure.
     * 
     * @return entityValue
     */
    public java.lang.String getEntityValue() {
        return entityValue;
    }


    /**
     * Sets the entityValue value for this ConstraintFailure.
     * 
     * @param entityValue
     */
    public void setEntityValue(java.lang.String entityValue) {
        this.entityValue = entityValue;
    }


    /**
     * Gets the parameters value for this ConstraintFailure.
     * 
     * @return parameters
     */
    public com.aspace.ftress.interfaces.ftress.DTO.Parameter[] getParameters() {
        return parameters;
    }


    /**
     * Sets the parameters value for this ConstraintFailure.
     * 
     * @param parameters
     */
    public void setParameters(com.aspace.ftress.interfaces.ftress.DTO.Parameter[] parameters) {
        this.parameters = parameters;
    }


    /**
     * Gets the reason value for this ConstraintFailure.
     * 
     * @return reason
     */
    public int getReason() {
        return reason;
    }


    /**
     * Sets the reason value for this ConstraintFailure.
     * 
     * @param reason
     */
    public void setReason(int reason) {
        this.reason = reason;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof ConstraintFailure)) return false;
        ConstraintFailure other = (ConstraintFailure) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.entity==null && other.getEntity()==null) || 
             (this.entity!=null &&
              this.entity.equals(other.getEntity()))) &&
            ((this.entityValue==null && other.getEntityValue()==null) || 
             (this.entityValue!=null &&
              this.entityValue.equals(other.getEntityValue()))) &&
            ((this.parameters==null && other.getParameters()==null) || 
             (this.parameters!=null &&
              java.util.Arrays.equals(this.parameters, other.getParameters()))) &&
            this.reason == other.getReason();
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
        if (getEntity() != null) {
            _hashCode += getEntity().hashCode();
        }
        if (getEntityValue() != null) {
            _hashCode += getEntityValue().hashCode();
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
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(ConstraintFailure.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ConstraintFailure"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("entity");
        elemField.setXmlName(new javax.xml.namespace.QName("", "entity"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/encoding/", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("entityValue");
        elemField.setXmlName(new javax.xml.namespace.QName("", "entityValue"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/encoding/", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("parameters");
        elemField.setXmlName(new javax.xml.namespace.QName("", "parameters"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "Parameter"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("reason");
        elemField.setXmlName(new javax.xml.namespace.QName("", "reason"));
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
