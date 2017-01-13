package mjb44.tools.packagerefactor;

public class Event {


    public EventType type;
    public String    detail;

    public Event(EventType type, String detail) {
        this.type   = type;
        this.detail = detail;
    }

    @Override
    public String toString() {
        return String.format("(%s:%s)"
                             , type.code
                             , detail);
    }
}
