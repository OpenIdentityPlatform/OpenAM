package org.forgerock.openam.core.rest.sms;

import org.forgerock.openam.test.apidescriptor.ApiAnnotationAssert;
import org.junit.Test;

public class SitesResourceProviderTest {

    @Test
    public void shouldFailIfAnnotationsAreNotValid() {
        ApiAnnotationAssert.assertThat(SitesResourceProvider.class).hasValidAnnotations();
    }

}
