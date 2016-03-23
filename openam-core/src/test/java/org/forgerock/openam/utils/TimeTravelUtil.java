package org.forgerock.openam.utils;

import org.forgerock.util.time.TimeService;

import java.lang.reflect.Field;

public enum TimeTravelUtil {

    /** Singleton Instance. */
    INSTANCE;

    private FastForwardTimeService timeService = new FastForwardTimeService();
    private boolean fastForwardEnabled = false;

    TimeTravelUtil () {
        try {
            Field timeServiceField = Time.class.getDeclaredField("timeService");
            timeServiceField.setAccessible(true);
            timeServiceField.set(Time.INSTANCE, timeService);
            this.fastForwardEnabled = true;
        } catch (IllegalAccessException | NoSuchFieldException e) {
            this.fastForwardEnabled = false;
        }
    }

    public static void fastForward(long millis) {
        if (INSTANCE.fastForwardEnabled) {
            INSTANCE.timeService.fastForward(millis);
        } else {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
            }
        }
    }

    private class FastForwardTimeService implements TimeService {

        private long offset;

        @Override
        public long now() {
            return System.currentTimeMillis() + offset;
        }

        @Override
        public long since(long l) {
            return now() - l;
        }

        public void fastForward(long increment) {
            this.offset += increment;
        }
    }
}