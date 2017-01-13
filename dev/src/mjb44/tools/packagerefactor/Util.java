package mjb44.tools.packagerefactor;

import java.util.Collection;
import com.google.common.base.Joiner;


public class Util {

    public static String join(Collection<String> components) {
        return Joiner.on(", ").join(components);
    }

}
