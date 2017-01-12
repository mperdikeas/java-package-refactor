package mjb44.tools.packagerefactor;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.io.File;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.common.base.Joiner;
import com.beust.jcommander.Parameter;

public class PackageRefactorCLI implements IConfigurationProvider {

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

    @Parameter(names = {"-n", "--translatable-file-names"}, description="file names to translate in the form of: (a) -n foo, (b) -n *.foo, (c) -n foo.* or (d) -n *.*", required=false, variableArity = true)
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
    public boolean getRelative() {return relative;}

    @Parameter(names = {"-c", "--config-file"}, description="configuration file in JSON format to use for options (except for -o, -d and -r", required=false, variableArity = false)
    String configFilename;
    

    public static String argumentsProblem(PackageRefactorCLI cli) {
        Path origin = Paths.get(cli.origin);
        Path destin = Paths.get(cli.destin);
        if (!Files.isDirectory(origin))
            return String.format("Origin [%s] does not exist or is not a directory"
                                 , cli.origin);

        if (Files.exists(destin))
            return String.format("Destin [%s] already exists - cowardly refusing to proceed"
                                 , cli.destin);

        String anchorsProblem = argumentProblemCheckPathsExist(origin, cli.anchors, cli.relative);
        if (anchorsProblem!=null)
            return anchorsProblem;
        String excludesProblem = argumentProblemCheckPathsExist(origin, cli.excludes, cli.relative);
        if (excludesProblem!=null)
            return excludesProblem;
        try {
            Configuration ignored = Configuration.fromConfigurationProvider(cli);
        } catch (ConfigurationException e) {
            return e.getMessage();
        }
        if ((cli.configFilename!=null) &&
            ((!cli.excludes.isEmpty())||(!cli.anchors.isEmpty())||(!cli.translation.isEmpty())||(!cli.translatableFilenames.isEmpty())||(cli.relative)))
            return "The -c option can not be used alongside any of the following options: [-x, -a, -t, -n, -r]";
        if ((cli.configFilename==null) && (cli.anchors.isEmpty() || cli.translation.isEmpty() || cli.translatableFilenames.isEmpty()))
            return "If a configuration file is not provided (with the -c option), then all of the following options are mandatory: [-a, -t, -n]";
        if ((cli.configFilename!=null) && (!Files.isRegularFile(Paths.get(cli.configFilename))))
            return String.format("Configuration file [%s] does not exist or is not a file", cli.configFilename);
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
