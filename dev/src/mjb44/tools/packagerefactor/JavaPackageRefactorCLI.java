package mjb44.tools.packagerefactor;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.io.File;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

import com.google.common.base.Joiner;
import com.beust.jcommander.Parameter;

import mutil.json.JsonProvider;

public class JavaPackageRefactorCLI implements IConfigurationProvider {

    @Parameter(names = {"-o", "--origin"}, description="origin directory", required=true, variableArity = false)
    String origin = ".";
    @Override
    public String getOrigin() {return origin;}

    @Parameter(names = {"-x", "--excludes"}, description="directories to exclude from copy", required=false, variableArity = true)
    public List<String> excludes = new ArrayList<>();
    @Override
    public List<String> getExcludes() {return excludes;}    

    @Parameter(names = {"-a", "--anchors"}, description="anchors in origin for directory restructuring", required=false, variableArity = true)
    public List<String> anchors = new ArrayList<>();
    @Override
    public List<String> getAnchors() {return anchors;}

    @Parameter(names = {"-t", "--package-translation"}, description="package translation in the form of: 'some.package'#'a.renaming.of.the.aforementioned.package'", required=false, variableArity = true)
    public List<String> translation = new ArrayList<>();
    @Override
    public List<String> getTranslation() {return translation;}

    @Parameter(names = {"-n", "--translatable-file-names"}, description="file names to translate in the form of: (a) -n foo, (b) -n *.foo, (c) -n foo.* or (d) -n *.* NB: files ending in *.java are always translated regardless of the value of this parameter", required=false, variableArity = true)
    public List<String> translatableFilenames = new ArrayList<>();
    @Override
    public List<String> getTranslatableFilenames() {return translatableFilenames;}
    
    @Parameter(names = {"-d", "--destin"}, description="destination directory", required=true, variableArity = false)
    String destin = ".";
    @Override
    public String getDestin() {return destin;}

    @Parameter(names = {"-r", "--excludes-anchors-relative"}, description="interpret -x and -a as relative to the origin ", required=false, variableArity=false)
    boolean relative = false;
    @Override
    public boolean isRelative() {return relative;}

    @Parameter(names = {"-c", "--config-file"}, description="configuration file in JSON format to use for options (except for -o, -d and -r", required=false, variableArity = false)
    String jsonConfigFile;

    @Parameter(names = {"-dj", "--dump-json-file"}, description="dump a JSON file capturing the configurations applicable for this run", required=false, variableArity = false)
    String dumpJsonConfigFile;

    @Parameter(names = {"-djf", "--dump-json-file-force"}, description="dump a JSON file capturing the configurations applicable for this run (overwrite existing JSON file if exists)", required=false, variableArity = false)  
    String dumpJsonConfigFileForce;
    

    public static String argumentsProblem(JavaPackageRefactorCLI cli) {
        if ((cli.jsonConfigFile!=null) &&
            ((!cli.excludes.isEmpty())||(!cli.anchors.isEmpty())||(!cli.translation.isEmpty())||(!cli.translatableFilenames.isEmpty())||(cli.relative)))
            return "The -c option can not be used alongside any of the following options: [-x, -a, -t, -n, -r]";
        if ((cli.jsonConfigFile!=null) && (!Files.isRegularFile(Paths.get(cli.jsonConfigFile))))
            return String.format("Configuration file [%s] does not exist or is not a file", cli.jsonConfigFile);
        if ((cli.dumpJsonConfigFile!=null) && (cli.dumpJsonConfigFileForce!=null))
            return String.format("Only one of the -dj or -djf options may be used but not both");
        if ((cli.dumpJsonConfigFile!=null) && (Files.exists(Paths.get(cli.dumpJsonConfigFile))))
            return String.format("Config dump file already exists: [%s], refusing to overwrite it, use the -djf option if you wish to overwrite the existing file", cli.dumpJsonConfigFile);
        String argumentsProblemCommon = argumentsProblemConfigCore(cli, cli.jsonConfigFile);
        if (argumentsProblemCommon!=null)
            return argumentsProblemCommon;
        return null;
    }
    
    public static String argumentsProblemConfigCore(IConfigurationProvider config, String jsonConfigFile) {
        Path origin = Paths.get(config.getOrigin());
        Path destin = Paths.get(config.getDestin());
        if (!Files.isDirectory(origin))
            return String.format("Origin [%s] does not exist or is not a directory"
                                 , config.getOrigin());

        if (Files.exists(destin)) {
            return String.format("Destin [%s] already exists - cowardly refusing to proceed"
                                 , config.getDestin());
        }

        String anchorsProblem = argumentProblemCheckPathsExist(origin, config.getAnchors(), config.isRelative());
        if (anchorsProblem!=null)
            return anchorsProblem;
        String excludesProblem = argumentProblemCheckPathsExist(origin, config.getExcludes(), config.isRelative());
        if (excludesProblem!=null)
            return excludesProblem;
        try {
            // If the jsonConfigFile is not null then most of the configuration values reside in the
            // JSON object, so we'll have to wait for the recursive call to check that
            //            if (jsonConfigFile==null) {
            // ignored is only created for the side effect of possibly throwing a ConfigurationException if
            // some check fails in the (more involved) factory method Configuration.fromConfigurationProvider
                Configuration ignored = Configuration.fromConfigurationProvider(config);
                //            }
            if (jsonConfigFile!=null) {
                try {
                    JSONConfiguration configJson = JsonProvider.fromJson(com.google.common.io.Files.toString(Paths.get(jsonConfigFile).toFile()
                                                                                                             , StandardCharsets.UTF_8)
                                                                         , JSONConfiguration.class);
                    IConfigurationProvider configProvider = configJson.createConfigurationProvider(config.getOrigin()
                                                                                                   , config.getDestin());
                    String problemInRecursiveCall = argumentsProblemConfigCore(configProvider, (String) null);
                    if (problemInRecursiveCall!=null)
                        return problemInRecursiveCall;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (ConfigurationException e) {
            return e.getMessage();
        }

        return null;
    }

    private static String argumentProblemCheckPathsExist(Path origin, List<String> paths, boolean relative) {
        for (String path: paths) {
            Path pathP = null;
            if (relative)
                pathP = origin.resolve(path);
            else
                pathP = Paths.get(path);
            if (!Files.isDirectory(pathP))
                if (relative)
                    return String.format("Path [%s] in origin [%s] is not a directory or does not exist"
                                         , pathP
                                         , origin);
                else
                    return String.format("Path [%s] is not a directory or does not exist"
                                         , pathP);
        }
        return null;
    }
}
