/**
 * ChannelsBlocked.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.aspace.ftress.interfaces.ftress.DTO;

public class ChannelsBlocked  implements java.io.Serializable {
    private com.aspace.ftress.interfaces.ftress.DTO.ChannelCode[] primaryBlocks;

    private com.aspace.ftress.interfaces.ftress.DTO.ChannelCode[] secondaryBlocks;

    public ChannelsBlocked() {
    }

    public ChannelsBlocked(
           com.aspace.ftress.interfaces.ftress.DTO.ChannelCode[] primaryBlocks,
           com.aspace.ftress.interfaces.ftress.DTO.ChannelCode[] secondaryBlocks) {
           this.primaryBlocks = primaryBlocks;
           this.secondaryBlocks = secondaryBlocks;
    }


    /**
     * Gets the primaryBlocks value for this ChannelsBlocked.
     * 
     * @return primaryBlocks
     */
    public com.aspace.ftress.interfaces.ftress.DTO.ChannelCode[] getPrimaryBlocks() {
        return primaryBlocks;
    }


    /**
     * Sets the primaryBlocks value for this ChannelsBlocked.
     * 
     * @param primaryBlocks
     */
    public void setPrimaryBlocks(com.aspace.ftress.interfaces.ftress.DTO.ChannelCode[] primaryBlocks) {
        this.primaryBlocks = primaryBlocks;
    }


    /**
     * Gets the secondaryBlocks value for this ChannelsBlocked.
     * 
     * @return secondaryBlocks
     */
    public com.aspace.ftress.interfaces.ftress.DTO.ChannelCode[] getSecondaryBlocks() {
        return secondaryBlocks;
    }


    /**
     * Sets the secondaryBlocks value for this ChannelsBlocked.
     * 
     * @param secondaryBlocks
     */
    public void setSecondaryBlocks(com.aspace.ftress.interfaces.ftress.DTO.ChannelCode[] secondaryBlocks) {
        this.secondaryBlocks = secondaryBlocks;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof ChannelsBlocked)) return false;
        ChannelsBlocked other = (ChannelsBlocked) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.primaryBlocks==null && other.getPrimaryBlocks()==null) || 
             (this.primaryBlocks!=null &&
              java.util.Arrays.equals(this.primaryBlocks, other.getPrimaryBlocks()))) &&
            ((this.secondaryBlocks==null && other.getSecondaryBlocks()==null) || 
             (this.secondaryBlocks!=null &&
              java.util.Arrays.equals(this.secondaryBlocks, other.getSecondaryBlocks())));
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
        if (getPrimaryBlocks() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getPrimaryBlocks());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getPrimaryBlocks(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getSecondaryBlocks() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getSecondaryBlocks());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getSecondaryBlocks(), i);
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
        new org.apache.axis.description.TypeDesc(ChannelsBlocked.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelsBlocked"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("primaryBlocks");
        elemField.setXmlName(new javax.xml.namespace.QName("", "primaryBlocks"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("secondaryBlocks");
        elemField.setXmlName(new javax.xml.namespace.QName("", "secondaryBlocks"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"));
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
