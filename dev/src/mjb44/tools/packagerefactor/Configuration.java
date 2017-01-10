package mjb44.tools.packagerefactor;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.LinkedHashSet;
import java.nio.file.Path;

public class Configuration {

    public Path       origin;
    public Set<Path>  anchors;
    public Path       destin;
    
    public Configuration(Path origin, Path destin, List<String> anchors) {
        this.origin        = origin;
        this.destin        = destin;
        this.anchors       = new LinkedHashSet<>();
        for (String anchor: anchors)
            this.anchors.add(origin.resolve(anchor));
    }
}
