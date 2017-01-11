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

public class PackageRefactorCLI {

    @Parameter(names = {"-o", "--origin"}, description="origin directory", required=true, variableArity = false)
    String origin = ".";

    @Parameter(names = {"-x", "--excludes"}, description="directories to exclude from copy", required=false, variableArity = true)
    public List<String> excludes = new ArrayList<>();

    @Parameter(names = {"-a", "--anchors"}, description="anchors in origin for directory restructuring", required=true, variableArity = true)
    public List<String> anchors = new ArrayList<>();

    @Parameter(names = {"-t", "--package-translation"}, description="package translation in the form of: 'some.package'='a.renaming.of.the.aforementioned.package'", required=true, variableArity = true)
    public List<String> translation = new ArrayList<>();

    @Parameter(names = {"-n", "--translatable-file-names"}, description="file names to translate in the form of: (a) -n foo, (b) -n *.foo, (c) -n foo.* or (d) -n *.* ; if not provided all files are translated", required=false, variableArity = true)
    public List<String> translatableFilenames = new ArrayList<>();
    
    @Parameter(names = {"-d", "--destin"}, description="destination directory", required=true, variableArity = false)
    String destin = ".";


    public static String argumentsProblem(PackageRefactorCLI cli) {
        Path origin = Paths.get(cli.origin);
        Path destin = Paths.get(cli.destin);
        if (!Files.isDirectory(origin))
            return String.format("Origin [%s] does not exist or is not a directory"
                                 , cli.origin);

        if (!Files.isDirectory(destin))
            return String.format("Destin [%s] does not exist or is not a directory"
                                 , cli.destin);

        if (destin.toFile().listFiles().length!=0)
            return String.format("Destin [%s] is not empty - cowardly refusing to proceed"
                                 , cli.destin);
        String anchorsProblem = argumentProblemCheckPathsExist(origin, cli.anchors);
        if (anchorsProblem!=null)
            return anchorsProblem;
        String excludesProblem = argumentProblemCheckPathsExist(origin, cli.excludes);
        if (excludesProblem!=null)
            return excludesProblem;
        
        return null;
    }

    private static String argumentProblemCheckPathsExist(Path origin, List<String> paths) {
        for (String path: paths) {
            Path pathP = origin.resolve(path);
            if (!Files.isDirectory(pathP))
                return String.format("Path [%s] in origin [%s] is not a directory or does not exist"
                                     , pathP
                                     , origin);
        }
        return null;
    }
}
