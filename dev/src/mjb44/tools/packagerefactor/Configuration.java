package mjb44.tools.packagerefactor;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Arrays;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

import static mjb44.tools.packagerefactor.Util.join;

public class Configuration {

    public Path       origin;
    public Set<Path>  excludes;    
    public Set<Path>  anchors;
    public Path       destin;

    public Map<List<String>, List<String>> translation;
    public Set<String> translatableFilenames;
    
    public Configuration(Path origin, Path destin, List<String> excludes, List<String> anchors, Map<List<String>, List<String>> translation, Set<String> translatableFilenames) {
        this.origin        = origin;
        this.destin        = destin;
        this.excludes       = new LinkedHashSet<>();
        for (String exclude: excludes)
            this.excludes.add(origin.resolve(exclude).normalize());
        this.anchors       = new LinkedHashSet<>();
        for (String anchor: anchors)
            this.anchors.add(origin.resolve(anchor).normalize());
        this.translation = translation;
        this.translatableFilenames = translatableFilenames;
    }

    protected ToStringHelper toStringHelper() {
        return MoreObjects.toStringHelper(this)
            .add("origin", origin)
            .add("excludes", excludes)
            .add("anchors", anchors)
            .add("destin", destin)
            .add("translation", translation)
            .add("translatableFilenames", translatableFilenames)
            ;
    }
    
    @Override
    public String toString() {
        return toStringHelper().toString();
    }
    


    public static Configuration fromCLI(PackageRefactorCLI cli) {

        Path origin = Paths.get(cli.origin).normalize();
        Path destin = Paths.get(cli.destin).normalize();
        Map<List<String>, List<String>> translation = translation(cli.translation);
        Set<String> translatableFilenames = translatableFilenames(cli.translatableFilenames);
        return new Configuration(origin, destin, cli.excludes, cli.anchors, translation, translatableFilenames); 
    }

    private static LinkedHashMap<List<String>, List<String>> translation(List<String> translations) {
        LinkedHashMap<List<String>, List<String>> rv = new LinkedHashMap<>();
        for (String translation: translations) {
            String[] fromTo = translation.split("=");
            Assert.assertEquals(2, fromTo.length);
            List<String> from = splitOnDots(fromTo[0]);
            List<String> to   = splitOnDots(fromTo[1]);
            Assert.assertNull(rv.put(from, to));
        }
        return rv;
    }

    private static List<String> splitOnDots(String packageName) {
        return Arrays.asList(packageName.split("\\."));
    }

    private static Set<String> translatableFilenames(List<String> translatableFilenames) {
        Set<String> rv = new LinkedHashSet<>();
        for (String translatableFilename: translatableFilenames) {
            String out = translatableFilename;
            List<RegexpAndValue> regexpAndValues = Arrays.asList(new RegexpAndValue[]{
                    new RegexpAndValue("\\."  , "\\\\."),
                    new RegexpAndValue("\\*"  , ".*"   )});
            for (RegexpAndValue regexpAndValue: regexpAndValues) {
                out = out.replaceAll(regexpAndValue.regexp
                                   , regexpAndValue.value);
            }
            rv.add(out);
        }
        System.out.printf("%s changed into %s\n"
                          , join(translatableFilenames)
                          , join(rv));
        return rv;
    }
}

class RegexpAndValue {
    public String regexp;
    public String value;
    public RegexpAndValue(String regexp, String value) {
        this.regexp = regexp;
        this.value  = value;
    }
}
