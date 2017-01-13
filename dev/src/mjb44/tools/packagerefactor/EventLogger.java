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
        List<Event> brokenLinks = eventsForType(EventType.BROKEN_SYMLINK);
        rv.add(String.format("%s#%s %d broken links could not be copied over:"
                             , REPORT
                             , EventType.BROKEN_SYMLINK.code.toUpperCase()
                             , brokenLinks.size()));
        for (Event brokenLink: brokenLinks)
            rv.add(String.format("%s#%s %s"
                                 , REPORT
                                 , EventType.BROKEN_SYMLINK.code.toUpperCase()
                                 , brokenLink.detail));
        return Joiner.on(System.lineSeparator()).join(rv);
    }

    public List<Event> eventsForType(EventType eventType) {
        List<Event> rv = new ArrayList<>();
        for (Event event: events) {
            if (event.type==EventType.BROKEN_SYMLINK)
                rv.add(event);
        }
        return rv;
    }
}
