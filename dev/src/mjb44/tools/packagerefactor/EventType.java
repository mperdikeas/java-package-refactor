package mjb44.tools.packagerefactor;

import java.util.List;
import java.util.ArrayList;

public enum EventType {

    BROKEN_SYMLINK("br-sym");

    public String code;
    
    private EventType(String code) {
        this.code = code;
    }

    public static EventType fromCode(String code) {
        for (EventType event: EventType.values())
            if (event.code.equals(code))
                return event;
        return null;
    }
}
