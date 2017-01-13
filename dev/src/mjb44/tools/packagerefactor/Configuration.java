package mjb44.tools.packagerefactor;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Arrays;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

import org.junit.Assert;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

import static mjb44.tools.packagerefactor.Util.join;

public class Configuration {

    private IConfigurationProvider               configProvider; // the configuration provider that created this configuration ?
    public Path                                  origin;
    public Set<Path>                             excludes;    
    public Set<Path>                             anchors;
    public Map<List<String>, ClassesInPackage>   exclusiveClasses;
    public Map<List<String>, List<String>>       translation;
    public Set<String>                           translatableFilenames;
    public Path                                  destin;


    private static boolean isPrefixOfSomeExclusivePackage(Map<List<String>, ClassesInPackage> exclusiveClasses
                                                          , List<String>                      translationKey) {
        Assert.assertFalse(exclusiveClasses.isEmpty());
        boolean rv = false;
        for (List<String> exclusivePackage: exclusiveClasses.keySet()) {
            if (translationKey.size()<=exclusivePackage.size()) {
                if (translationKey.equals(exclusivePackage.subList(0, translationKey.size())))
                    rv = true;
            }
        }
        return rv;
    }
    
    private Configuration(IConfigurationProvider           configProvider
                          , Path                            origin
                          , Set<Path>                       excludes
                          , Set<Path>                       anchors
                          , Map<List<String>, ClassesInPackage>   exclusiveClasses
                          , Map<List<String>, List<String>> translation
                          , Set<String>                     translatableFilenames
                          , Path                            destin) throws ConfigurationException {
        this.configProvider = configProvider;
        if (!Files.isDirectory(origin))
            throw new ConfigurationException(String.format("Origin [%s] does not exist or is not a directory"
                                                           , origin));
        for (Path exclude: excludes)
            if (!Files.isDirectory(exclude))
                throw new ConfigurationException(String.format("Exclude [%s] does not exist or is not a directory"
                                                               , exclude));
        for (Path anchor: anchors)
            if (!Files.isDirectory(anchor))
                throw new ConfigurationException(String.format("Anchor [%s] does not exist or is not a directory"
                                                               , anchor));
        this.exclusiveClasses = exclusiveClasses;
        {
            for (List<String> pkgS: this.exclusiveClasses.keySet()) {
                System.out.printf("examining package or class specification [%s]\n", Util.join(pkgS));
                Path anchorUnderWhichItIsFound = null;
                for (Path anchor: anchors) {
                    String first = pkgS.get(0);
                    List<String> rest = pkgS.subList(1, pkgS.size());
                    Path pkg = Paths.get(first, rest.toArray(new String[rest.size()]));
                    if (Files.isDirectory(anchor.resolve(pkg))) {
                        anchorUnderWhichItIsFound = anchor;
                        break;
                    }
                }
                if (anchorUnderWhichItIsFound == null)
                    throw new ConfigurationException(String.format("Package [%s] does not exist under any of the following anchors: [%s]"
                                                                   , Util.join(pkgS)
                                                                   , Util.join(Util.stringify(anchors))));             
            }
        }
        if (!this.exclusiveClasses.isEmpty()) {
            // If the exclusive packages option is used, then it only makes sense for the user
            // to specify translation mappings where the keys correspond to some prefix of the
            // exclusive packages.
            for (List<String> translationKey: translation.keySet()) {
                if (!isPrefixOfSomeExclusivePackage(this.exclusiveClasses, translationKey))
                    throw new ConfigurationException(
                    String.format("Translation key [%s] corresponds to none of the exclusive packages {%s}"
                                  , Util.join(translationKey)
                                  , this.exclusiveClasses.toString()));
            }
        }
        if (!Files.isDirectory(origin))
            throw new ConfigurationException(String.format("Destin [%s] does not exist or is not a directory"
                                                           , destin));
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

    public JSONConfiguration relativize() {
        List<String> excludes = null;
        List<String> anchors  = null;

        if (!configProvider.isRelative()) {
            excludes = relativize(origin, configProvider.getExcludes());
            anchors  = relativize(origin, configProvider.getAnchors ());
        } else {
            excludes = configProvider.getExcludes();
            anchors  = configProvider.getAnchors();
        }
        return new JSONConfiguration(excludes
                                     , anchors
                                     , configProvider.getTranslation()
                                     , configProvider.getTranslatableFilenames());
    }

    private static List<String> relativize(Path origin, List<String> dirs) {
        List<String> rv = new ArrayList<>();
        for (String dir : dirs) {
            Path dirp = Paths.get(dir);
            Assert.assertTrue(Files.exists(dirp));
            Assert.assertTrue(dirp.startsWith(origin));
            rv.add(origin.relativize(Paths.get(dir)).toString());
        }
        return rv;
    }
    


    public static Configuration fromConfigurationProvider(IConfigurationProvider cp) throws ConfigurationException {
        Path                                 origin = Paths.get(cp.getOrigin()).normalize();
        Set<Path>                            excludes = stringsToPaths(cp.getExcludes(), origin, cp.isRelative());
        Set<Path>                            anchors  = stringsToPaths(cp.getAnchors() , origin, cp.isRelative());
        Map<List<String>, ClassesInPackage>  exclusiveClasses = exclusiveClasses(cp.getExclusiveClasses());
        Map<List<String>, List<String>>      translation = translation(cp.getTranslation());
        Set<String>                          translatableFilenames = translatableFilenames(cp.getTranslatableFilenames());
        Path                                 destin = Paths.get(cp.getDestin()).normalize();        
        return new Configuration(cp, origin, excludes, anchors, exclusiveClasses, translation, translatableFilenames, destin);
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

    private static Map<List<String>, ClassesInPackage> exclusiveClasses(List<String> exclusiveClassesS) throws ConfigurationException {
        Map<List<String>, ClassesInPackage> rv = new LinkedHashMap<>();
        for (String exclusiveClassS: exclusiveClassesS) {
            List<String> packageCompos = null;
            String simpleClassName     = null;
            {
                if (exclusiveClassS.endsWith(".*")) {
                    String packageName = exclusiveClassS.substring(0, exclusiveClassS.length()-2);
                    packageCompos = Arrays.asList(packageName.split("\\."));
                    simpleClassName = null;
                } else {
                    List<String>      packageComposAndClass = Arrays.asList(exclusiveClassS.split("\\."));
                    packageCompos   = packageComposAndClass.subList(0, packageComposAndClass.size()-1);
                    simpleClassName = packageComposAndClass.get(packageComposAndClass.size()-1);
                }
            }
            if (simpleClassName==null) {
                if (rv.put(packageCompos, ClassesInPackage.all())!=null)
                    throw new ConfigurationException(String.format("Duplicate entire package encountered in the -e arguments: [%s]"
                                                                   , exclusiveClassS));
            } else {
                if (!rv.containsKey(packageCompos)) {
                    List<String> simpleClassNames = new ArrayList<>();
                    simpleClassNames.add(simpleClassName);
                    rv.put(packageCompos, ClassesInPackage.some(simpleClassNames));
                } else {
                    List<String> simpleClassNames = rv.get(packageCompos).simpleClassNames;
                    if (simpleClassNames.contains(simpleClassName))
                        throw new ConfigurationException(String.format("Duplicate class encountered in the -e arguments: [%s]"
                                                                       , exclusiveClassS));
                    simpleClassNames.add(simpleClassName);
                }
            } I am left here to ensure that reading the arguments from the command line works
        }
        if (false) {
        {
            List<String> pkg = new ArrayList<>();
            pkg.add("mutil");
            rv.put(pkg, ClassesInPackage.all());
        }
        {
            List<String> pkg = new ArrayList<>();
            pkg.add("mutil");
            pkg.add("base");
            rv.put(pkg, ClassesInPackage.all());
        }
        {
            List<String> pkg = new ArrayList<>();
            pkg.add("mutil");
            pkg.add("base");
            pkg.add("fsm");
            rv.put(pkg, ClassesInPackage.some( Arrays.asList(new String[]{"FSMEngine", "IFSMConfig"}) ));

        }
        }
        System.out.printf("Returning : [%s]\n", rv);
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
            {
                final String JAVA_EXTENSION = "*.java";
                if (translatableFilename.equals(JAVA_EXTENSION))
                    throw new ConfigurationException(String.format("Java files are always translated, you don't have to include '%s' in the -n parameter"
                                                                   , JAVA_EXTENSION));
}
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
