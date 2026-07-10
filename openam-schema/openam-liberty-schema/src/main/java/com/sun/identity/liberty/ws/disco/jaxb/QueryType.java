
package com.sun.identity.liberty.ws.disco.jaxb;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlID;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * <p>Java class for QueryType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="QueryType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;group ref="{urn:liberty:disco:2003-08}ResourceIDGroup"/&gt;
 *         &lt;element name="RequestedServiceType" maxOccurs="unbounded" minOccurs="0"&gt;
 *           &lt;complexType&gt;
 *             &lt;complexContent&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                 &lt;sequence&gt;
 *                   &lt;element ref="{urn:liberty:disco:2003-08}ServiceType"/&gt;
 *                   &lt;element ref="{urn:liberty:disco:2003-08}Options" minOccurs="0"/&gt;
 *                 &lt;/sequence&gt;
 *               &lt;/restriction&gt;
 *             &lt;/complexContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}ID" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "QueryType", propOrder = {
    "resourceID",
    "encryptedResourceID",
    "requestedServiceType"
})
public class QueryType {

    @XmlElement(name = "ResourceID")
    protected ResourceIDType resourceID;
    @XmlElement(name = "EncryptedResourceID")
    protected EncryptedResourceIDType encryptedResourceID;
    @XmlElement(name = "RequestedServiceType")
    protected List<QueryType.RequestedServiceType> requestedServiceType;
    @XmlAttribute(name = "id")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    @XmlSchemaType(name = "ID")
    protected String id;

    /**
     * Gets the value of the resourceID property.
     * 
     * @return
     *     possible object is
     *     {@link ResourceIDType }
     *     
     */
    public ResourceIDType getResourceID() {
        return resourceID;
    }

    /**
     * Sets the value of the resourceID property.
     * 
     * @param value
     *     allowed object is
     *     {@link ResourceIDType }
     *     
     */
    public void setResourceID(ResourceIDType value) {
        this.resourceID = value;
    }

    /**
     * Gets the value of the encryptedResourceID property.
     * 
     * @return
     *     possible object is
     *     {@link EncryptedResourceIDType }
     *     
     */
    public EncryptedResourceIDType getEncryptedResourceID() {
        return encryptedResourceID;
    }

    /**
     * Sets the value of the encryptedResourceID property.
     * 
     * @param value
     *     allowed object is
     *     {@link EncryptedResourceIDType }
     *     
     */
    public void setEncryptedResourceID(EncryptedResourceIDType value) {
        this.encryptedResourceID = value;
    }

    /**
     * Gets the value of the requestedServiceType property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a <CODE>set</CODE> method for the requestedServiceType property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getRequestedServiceType().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link QueryType.RequestedServiceType }
     * 
     * 
     */
    public List<QueryType.RequestedServiceType> getRequestedServiceType() {
        if (requestedServiceType == null) {
            requestedServiceType = new ArrayList<QueryType.RequestedServiceType>();
        }
        return this.requestedServiceType;
    }

    /**
     * Gets the value of the id property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setId(String value) {
        this.id = value;
    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType&gt;
     *   &lt;complexContent&gt;
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *       &lt;sequence&gt;
     *         &lt;element ref="{urn:liberty:disco:2003-08}ServiceType"/&gt;
     *         &lt;element ref="{urn:liberty:disco:2003-08}Options" minOccurs="0"/&gt;
     *       &lt;/sequence&gt;
     *     &lt;/restriction&gt;
     *   &lt;/complexContent&gt;
     * &lt;/complexType&gt;
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "serviceType",
        "options"
    })
    public static class RequestedServiceType {

        @XmlElement(name = "ServiceType", required = true)
        @XmlSchemaType(name = "anyURI")
        protected String serviceType;
        @XmlElement(name = "Options")
        protected OptionsType options;

        /**
         * Gets the value of the serviceType property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getServiceType() {
            return serviceType;
        }

        /**
         * Sets the value of the serviceType property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setServiceType(String value) {
            this.serviceType = value;
        }

        /**
         * Gets the value of the options property.
         * 
         * @return
         *     possible object is
         *     {@link OptionsType }
         *     
         */
        public OptionsType getOptions() {
            return options;
        }

        /**
         * Sets the value of the options property.
         * 
         * @param value
         *     allowed object is
         *     {@link OptionsType }
         *     
         */
        public void setOptions(OptionsType value) {
            this.options = value;
        }

    }

    /**
     * Compatibility shim: JAXB 1.x XJC generated this inner class as
     * {@code RequestedServiceTypeType}. JAXB 4.x names it {@code RequestedServiceType}.
     */
    public static class RequestedServiceTypeType extends RequestedServiceType {}

}
