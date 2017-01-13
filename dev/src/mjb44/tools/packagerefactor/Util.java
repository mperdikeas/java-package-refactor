package mjb44.tools.packagerefactor;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.nio.file.Path;
import com.google.common.base.Joiner;


public class Util {

    public static String join(Collection<String> components) {
        return join(components, ", ");
    }

    public static String join(Collection<String> components, String sep) {
        return Joiner.on(sep).join(components);
    }
    
    public static List<String> stringify(Collection<Path> paths) {
        List<String> rv = new ArrayList<>();
        for (Path path: paths)
            rv.add(path.toString());
        return rv;
    }

}
