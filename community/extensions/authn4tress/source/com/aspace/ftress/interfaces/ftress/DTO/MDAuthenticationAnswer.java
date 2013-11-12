/**
 * MDAuthenticationAnswer.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.aspace.ftress.interfaces.ftress.DTO;

public class MDAuthenticationAnswer  extends com.aspace.ftress.interfaces.ftress.DTO.MDAnswer  implements java.io.Serializable {
    private int[] seedPositions;

    public MDAuthenticationAnswer() {
    }

    public MDAuthenticationAnswer(
           java.lang.String answer,
           com.aspace.ftress.interfaces.ftress.DTO.MDPromptCode promptCode,
           int[] seedPositions) {
        super(
            answer,
            promptCode);
        this.seedPositions = seedPositions;
    }


    /**
     * Gets the seedPositions value for this MDAuthenticationAnswer.
     * 
     * @return seedPositions
     */
    public int[] getSeedPositions() {
        return seedPositions;
    }


    /**
     * Sets the seedPositions value for this MDAuthenticationAnswer.
     * 
     * @param seedPositions
     */
    public void setSeedPositions(int[] seedPositions) {
        this.seedPositions = seedPositions;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof MDAuthenticationAnswer)) return false;
        MDAuthenticationAnswer other = (MDAuthenticationAnswer) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = super.equals(obj) && 
            ((this.seedPositions==null && other.getSeedPositions()==null) || 
             (this.seedPositions!=null &&
              java.util.Arrays.equals(this.seedPositions, other.getSeedPositions())));
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
        if (getSeedPositions() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getSeedPositions());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getSeedPositions(), i);
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
        new org.apache.axis.description.TypeDesc(MDAuthenticationAnswer.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "MDAuthenticationAnswer"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("seedPositions");
        elemField.setXmlName(new javax.xml.namespace.QName("", "seedPositions"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
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
