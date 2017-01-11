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
        for (Event event: events)
            rv.add(event.toString());
        return Joiner.on(System.lineSeparator()).join(rv);
    }
}
