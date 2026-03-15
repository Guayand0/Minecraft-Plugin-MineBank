package com.Guayand0.managers;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class EventManager {

    public enum EventType {
        PROFIT,
        TAX
    }

    public static class EventInfo {
        private final EventType type;
        private final double multiplier;
        private final long remainingMillis;

        public EventInfo(EventType type, double multiplier, long remainingMillis) {
            this.type = type;
            this.multiplier = multiplier;
            this.remainingMillis = remainingMillis;
        }

        public EventType getType() {
            return type;
        }

        public double getMultiplier() {
            return multiplier;
        }

        public long getRemainingMillis() {
            return remainingMillis;
        }
    }

    private static class EventData {
        private double multiplier;
        private long endTimeMillis;
    }

    private final Map<EventType, EventData> events = new EnumMap<>(EventType.class);

    public void startEvent(EventType type, double multiplier, long durationMillis) {
        EventData data = new EventData();
        data.multiplier = multiplier;
        data.endTimeMillis = System.currentTimeMillis() + durationMillis;
        events.put(type, data);
    }

    public void cancelEvent(EventType type) {
        events.remove(type);
    }

    public void cancelAllEvents() {
        events.clear();
    }

    public boolean isActive(EventType type) {
        EventData data = events.get(type);
        if (data == null) return false;
        if (System.currentTimeMillis() >= data.endTimeMillis) {
            events.remove(type);
            return false;
        }
        return true;
    }

    public double getMultiplier(EventType type) {
        if (!isActive(type)) return 1.0;
        EventData data = events.get(type);
        return data == null ? 1.0 : data.multiplier;
    }

    public long getRemainingMillis(EventType type) {
        if (!isActive(type)) return 0L;
        EventData data = events.get(type);
        if (data == null) return 0L;
        return Math.max(0L, data.endTimeMillis - System.currentTimeMillis());
    }

    public List<EventInfo> getActiveEvents() {
        List<EventInfo> list = new ArrayList<>();
        for (EventType type : EventType.values()) {
            if (isActive(type)) {
                list.add(new EventInfo(type, getMultiplier(type), getRemainingMillis(type)));
            }
        }
        return list;
    }
}
