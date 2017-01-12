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

    public Path                            origin;
    public Set<Path>                       excludes;    
    public Set<Path>                       anchors;
    public Map<List<String>, List<String>> translation;
    public Set<String>                     translatableFilenames;
    public Path                            destin;

    
    public Configuration(Path                              origin
                         , Set<Path>                       excludes
                         , Set<Path>                       anchors
                         , Map<List<String>, List<String>> translation
                         , Set<String>                     translatableFilenames
                         , Path                            destin) {
        this.origin                = origin;
        this.excludes              = excludes;
        this.anchors               = anchors;
        this.translation           = translation;
        this.translatableFilenames = translatableFilenames;
        this.destin                = destin;        
    }

    protected ToStringHelper toStringHelper() {
        return MoreObjects.toStringHelper(this)
            .add("origin"               , origin)
            .add("excludes"             , excludes)
            .add("anchors"              , anchors)
            .add("translation"          , translation)
            .add("translatableFilenames", translatableFilenames)
            .add("destin"               , destin)
            ;
    }
    
    @Override
    public String toString() {
        return toStringHelper().toString();
    }
    


    public static Configuration fromConfigurationProvider(IConfigurationProvider cp) throws ConfigurationException {

        Path origin = Paths.get(cp.getOrigin()).normalize();
        Set<Path> excludes = stringsToPaths(cp.getExcludes(), origin, cp.getRelative());
        Set<Path> anchors  = stringsToPaths(cp.getAnchors() , origin, cp.getRelative());
        Map<List<String>, List<String>> translation = translation(cp.getTranslation());
        Set<String> translatableFilenames = translatableFilenames(cp.getTranslatableFilenames());
        Path destin = Paths.get(cp.getDestin()).normalize();        
        return new Configuration(origin, excludes, anchors, translation, translatableFilenames, destin);
    }

    private static Set<Path> stringsToPaths (List<String> paths, Path origin, boolean relative) throws ConfigurationException {
        Set<Path> rv = new LinkedHashSet<>();
        Path p = null;
        for (String path: paths) {
            if (relative)
                p = origin.resolve(path).normalize();
            else
                p = Paths.get(path).normalize();
            if (!rv.add(p))
                throw new ConfigurationException(String.format("Path [%s] already encountered - please don't use duplicate paths"
                                                               , path));
        }
        return rv;
    }

    private static LinkedHashMap<List<String>, List<String>> translation(List<String> translations) throws ConfigurationException {
        LinkedHashMap<List<String>, List<String>> rv = new LinkedHashMap<>();
        for (String translation: translations) {
            String[] fromTo = translation.split("#");
            Assert.assertEquals(2, fromTo.length);
            List<String> from = splitOnDots(fromTo[0]);
            List<String> to   = splitOnDots(fromTo[1]);
            if (rv.put(from, to)!=null)
                throw new ConfigurationException(String.format("Duplicate translation rule for [%s] encountered"
                                                               , join(from)));
        }
        return rv;
    }

    private static List<String> splitOnDots(String packageName) {
        return Arrays.asList(packageName.split("\\."));
    }

    private static Set<String> translatableFilenames(List<String> translatableFilenames) throws ConfigurationException {
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
            if (!rv.add(out))
                throw new ConfigurationException(String.format("translatable filename [%s] has already been provided, please don't use duplicate translatable filenames"
                                                               , translatableFilename));
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
