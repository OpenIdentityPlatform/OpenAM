package org.forgerock.openam.utils;

import org.testng.annotations.Test;

import java.util.Calendar;

import static org.testng.Assert.assertEquals;

public class TimeUtilsTest {
    @Test
    public void shouldConvertTimeToEpochedTimeAndBackAgain() {
        // Given
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.MILLISECOND, 0);

        // When
        Calendar result = TimeUtils.fromUnixTime(TimeUtils.toUnixTime(calendar));

        // Then
        assertEquals(result.getTimeInMillis(), calendar.getTimeInMillis());
    }
}