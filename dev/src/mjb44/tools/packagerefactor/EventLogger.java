package mjb44.tools.packagerefactor;

import java.util.List;
import java.util.ArrayList;

import com.google.common.base.Joiner;

public class EventLogger {

    public List<String> events;

    public EventLogger() {
        this.events = new ArrayList<>();
    }

    public void logBrokenSymlink(String fname) {
        events.add(String.format("broken symlink skipped: [%s]", fname));
    }

    public String report() {
        return Joiner.on(System.lineSeparator()).join(events);
    }
}
