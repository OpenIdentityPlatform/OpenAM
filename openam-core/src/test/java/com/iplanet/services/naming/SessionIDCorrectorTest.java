package com.iplanet.services.naming;

import static org.assertj.core.api.Assertions.assertThat;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests should not need documenting. However because this one deals with the confusing
 * mapping of Server IDs and Site ID, it does require some comment.
 *
 * During these tests I have referred to two concepts.
 *
 * Server ID and Site ID: A server which is part of a site.
 *
 * Primary ID (S1) and Site ID (SI): The actual ID's used by the Session Service to
 * identify a Server which is part of a Site.
 */
public class SessionIDCorrectorTest {
    /**
     * A server which is not part of a site is signified by having its server ID
     * in the SiteID (SI) field of the extensions.
     */
    @Test
    public void shouldLeaveServerIDInSiteIDFieldWhenNoMappingsPresent() {
        SessionIDCorrector corrector = generateAutocorrect("");
        String result = corrector.translateSiteID("", "01");
        assertThat(result).isEqualTo("01");
    }

    /**
     * However, it is possible for a Server ID to be stored in the Primary ID (S1)
     * without a Site present in Site ID (SI). However this combination
     * is never used in practice.
     *
     * Note, this test results in the same result as the above
     * #shouldLeaveServerIDInPrimaryIDFieldWhenBothPrimaryAndSiteIDArePresent
     *
     * This is perhaps the most confusing aspect of the SessionID extensions
     * mapping logic.
     */
    @Test
    public void shouldNotChangePrimaryIDWhenNoMappingsPresent() {
        SessionIDCorrector corrector = generateAutocorrect("");
        String result = corrector.translatePrimaryID("01", "");
        assertThat(result).isEqualTo("01");
    }

    /**
     * When both Primary ID (S1) and Site ID (SI) are present this signals a Server
     * which is part of a Site.
     */
    @Test
    public void shouldLeaveServerIDInPrimaryIDFieldWhenBothPrimaryAndSiteIDArePresent() {
        SessionIDCorrector corrector = generateAutocorrect("");
        String result = corrector.translatePrimaryID("01", "02");
        assertThat(result).isEqualTo("01");
    }

    /**
     * When signalling that only a Server is present, the Site will be empty.
     */
    @Test
    public void shouldLeaveSiteIDEmptyWhenOnlyServerIDIsPresent() {
        SessionIDCorrector corrector = generateAutocorrect("");
        String result = corrector.translatePrimaryID("", "01");
        assertThat(result).isNull();
    }

    @Test
    public void shouldMapServerToSite() {
        SessionIDCorrector corrector = generateAutocorrect("01,02");
        String result = corrector.translatePrimaryID("", "01");
        assertThat(result).isEqualTo("01");
    }

    @Test
    public void shouldMapUpdateSiteIDWhenMappingServerToSite() {
        SessionIDCorrector corrector = generateAutocorrect("01,02");
        String result = corrector.translateSiteID("", "01");
        assertThat(result).isEqualTo("02");
    }

    @Test
    public void shouldStripSiteFromServer() {
        SessionIDCorrector corrector = generateAutocorrect("01,--");
        String result = corrector.translateSiteID("01", "02");
        assertThat(result).isEqualTo("01");
    }

    /**
     * Uses a pipe separated format to easily describe the mappings from
     * Server to Site. This function is irrespective of the counter-intuitive
     * behaviour described above. It simply maps Server to Site.
     *
     * @param mapping Pipe separated format.
     *
     * @return Non null Autocorrect.
     */
    private SessionIDCorrector generateAutocorrect(String mapping) {
        Map<String, String> serverToSite = new HashMap<>();
        for (String line : mapping.split("\\|")) {
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = line.trim().split(",");
            serverToSite.put(parts[0], parts[1].equals("--") ? null : parts[1]);
        }
        return new SessionIDCorrector(serverToSite);
    }
}
