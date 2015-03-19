package org.forgerock.openam.session.stateless;

import com.iplanet.am.util.SystemPropertiesWrapper;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;

public class StatelessConfigTest {

    private StatelessConfig config;
    private SystemPropertiesWrapper wrapper;

    @BeforeMethod
    public void setUp() {
        wrapper = mock(SystemPropertiesWrapper.class);
        config = new StatelessConfig(wrapper);
    }

    @Test
    public void shouldUseWrapperForJWTCacheSize() {
        int value = 123;
        given(wrapper.getAsInt(anyString(), anyInt())).willReturn(value);
        assertThat(config.getJWTCacheSize()).isEqualTo(value);
    }
}