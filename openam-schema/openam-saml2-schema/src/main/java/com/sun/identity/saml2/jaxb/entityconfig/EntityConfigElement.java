package com.sun.identity.saml2.jaxb.entityconfig;

import java.util.AbstractList;
import java.util.List;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Root-element binding for EntityConfig, extending EntityConfigType.
 *
 * <p>{@code @XmlRootElement} is required so JAXB 4.x returns this concrete
 * class directly on unmarshal rather than wrapping it in
 * {@code JAXBElement&lt;EntityConfigType&gt;}. Without it every
 * {@code instanceof EntityConfigElement} check in SAML2MetaManager fails.
 *
 * <p>The {@link #getIDPSSOConfigOrSPSSOConfigOrAuthnAuthorityConfig()} override
 * is a safety adapter: if the backing list ever contains raw
 * {@code JAXBElement} wrappers (e.g., from an older context path) this adapter
 * unwraps them transparently so that legacy cast/instanceof code in
 * SAML2MetaManager continues to work without modification.
 */
@XmlRootElement(name = "EntityConfig", namespace = "urn:sun:fm:SAML:2.0:entityconfig")
public class EntityConfigElement extends EntityConfigType {

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public List<BaseConfigType> getIDPSSOConfigOrSPSSOConfigOrAuthnAuthorityConfig() {
        final List backing = super.getIDPSSOConfigOrSPSSOConfigOrAuthnAuthorityConfig();
        return new AbstractList<BaseConfigType>() {
            @Override
            public int size() {
                return backing.size();
            }

            @Override
            public BaseConfigType get(int i) {
                Object elem = backing.get(i);
                if (elem instanceof JAXBElement) {
                    return (BaseConfigType) ((JAXBElement<?>) elem).getValue();
                }
                return (BaseConfigType) elem;
            }

            @Override
            public boolean add(BaseConfigType e) {
                return backing.add(e);
            }

            @Override
            public void add(int index, BaseConfigType element) {
                backing.add(index, element);
            }
        };
    }
}
