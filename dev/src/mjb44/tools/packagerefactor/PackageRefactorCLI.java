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

    @Parameter(names = {"-d", "--destin"}, description="destination directory", required=true, variableArity = false)
    String destin = ".";

    @Parameter(names = {"-a", "--anchors"}, description="anchors in origin for directory restructuring and (?) content renaming", required=true, variableArity = true)
    public List<String> anchors = new ArrayList<>();



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
        for (String anchor: cli.anchors) {
            Path anchorP = origin.resolve(anchor);
            if (!Files.isDirectory(anchorP))
                return String.format("Anchor [%s] in origin [%s] is not a directory or does not exist"
                                     , anchor
                                     , origin);
        }
        return null;
    }
}
