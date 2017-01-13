package mjb44.tools.packagerefactor;

import java.util.List;
import java.util.ArrayList;

import com.google.common.base.Joiner;

public class EventLogger {

    public List<Event> events;

    public EventLogger() {
        this.events = new ArrayList<>();
    }

    public void log(EventType type, String detail) {
        events.add(new Event(type, detail));
    }

    public String report() {
        List<String> rv = new ArrayList<>();
        final String REPORT = "REPORT";
        for (EventType eventType: EventType.values())
            populateForType(rv, eventType);
        return Joiner.on(System.lineSeparator()).join(rv);
    }

    private void populateForType(List<String> lines, EventType eventType) {
        final String REPORT = "REPORT";
        List<Event> events = eventsForType(eventType);
        if (!events.isEmpty()) {
            if (!eventType.singular) {
                lines.add(String.format("%s#%s %d instances of event type [%s]"
                                        , REPORT
                                        , eventType.code.toUpperCase()
                                        , events.size()
                                        , eventType.descr));
            }
            for (Event brokenLink: events)
                lines.add(String.format("%s#%s %s"
                                        , REPORT
                                        , eventType.code.toUpperCase()
                                        , brokenLink.detail));
        }
    }

    public List<Event> eventsForType(EventType eventType) {
        List<Event> rv = new ArrayList<>();
        for (Event event: events) {
            if (event.type==eventType)
                rv.add(event);
        }
        return rv;
    }
}
