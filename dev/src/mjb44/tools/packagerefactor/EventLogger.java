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

    public static final String ANSI_RESET  = "\u001B[0m" ;
    public static final String ANSI_BLACK  = "\u001B[30m";
    public static final String ANSI_RED    = "\u001B[31m";
    public static final String ANSI_GREEN  = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE   = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN   = "\u001B[36m";
    public static final String ANSI_WHITE  = "\u001B[37m";

    public String report(boolean quiet) {
        List<String> rv = new ArrayList<>();
        final String REPORT = "REPORT";
        for (EventType eventType: EventType.values())
            if ( (!quiet) || (eventType.unsettling) )
            populateForType(rv, eventType);
        return Joiner.on(System.lineSeparator()).join(rv);
    }

    public boolean unsettlingEventsExist() {
        for (Event event: events) {
            if (event.type.unsettling)
                return true;
        }
        return false;
    }

    private void populateForType(List<String> lines, EventType eventType) {
        final String REPORT = "REPORT";
        List<Event> events = eventsForType(eventType);
        if (!events.isEmpty()) {
            if (!eventType.singular) {
                lines.add(String.format("%s%s#%s %d instances of event type [%s]%s"
                                        , ANSI_BLUE
                                        , REPORT
                                        , eventType.code.toUpperCase()
                                        , events.size()
                                        , eventType.descr
                                        , ANSI_RESET));
                          
            }
            for (Event brokenLink: events)
                lines.add(String.format("%s%s#%s%s %s"
                                        , ANSI_GREEN
                                        , REPORT
                                        , eventType.code.toUpperCase()
                                        , ANSI_RESET
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
