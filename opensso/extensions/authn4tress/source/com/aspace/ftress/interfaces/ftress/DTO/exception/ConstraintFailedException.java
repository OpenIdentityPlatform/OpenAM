/**
 * ConstraintFailedException.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.aspace.ftress.interfaces.ftress.DTO.exception;

public class ConstraintFailedException  extends com.aspace.ftress.interfaces.ftress.DTO.exception.BusinessException  implements java.io.Serializable {
    private com.aspace.ftress.interfaces.ftress.DTO.ConstraintFailure[] failureReasons;

    public ConstraintFailedException() {
    }

    public ConstraintFailedException(
           int errorCode,
           com.aspace.ftress.interfaces.ftress.DTO.Parameter[] parameters,
           long reference,
           com.aspace.ftress.interfaces.ftress.DTO.ConstraintFailure[] failureReasons) {
        super(
            errorCode,
            parameters,
            reference);
        this.failureReasons = failureReasons;
    }


    /**
     * Gets the failureReasons value for this ConstraintFailedException.
     * 
     * @return failureReasons
     */
    public com.aspace.ftress.interfaces.ftress.DTO.ConstraintFailure[] getFailureReasons() {
        return failureReasons;
    }


    /**
     * Sets the failureReasons value for this ConstraintFailedException.
     * 
     * @param failureReasons
     */
    public void setFailureReasons(com.aspace.ftress.interfaces.ftress.DTO.ConstraintFailure[] failureReasons) {
        this.failureReasons = failureReasons;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof ConstraintFailedException)) return false;
        ConstraintFailedException other = (ConstraintFailedException) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = super.equals(obj) && 
            ((this.failureReasons==null && other.getFailureReasons()==null) || 
             (this.failureReasons!=null &&
              java.util.Arrays.equals(this.failureReasons, other.getFailureReasons())));
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
        if (getFailureReasons() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getFailureReasons());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getFailureReasons(), i);
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
        new org.apache.axis.description.TypeDesc(ConstraintFailedException.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ConstraintFailedException"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("failureReasons");
        elemField.setXmlName(new javax.xml.namespace.QName("", "failureReasons"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ConstraintFailure"));
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


    /**
     * Writes the exception data to the faultDetails
     */
    public void writeDetails(javax.xml.namespace.QName qname, org.apache.axis.encoding.SerializationContext context) throws java.io.IOException {
        context.serialize(qname, null, this);
    }
}
