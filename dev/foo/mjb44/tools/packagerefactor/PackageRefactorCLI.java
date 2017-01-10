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

    @Parameter(names = {"-f", "--from"}, description="from", required=true, variableArity = false)
    String from = ".";

    @Parameter(names = {"-t", "--to"}, description="to", required=true, variableArity = false)
    String to = ".";



    @Parameter(names = {"-r", "--refactor"}, description="package refactorings in the form: a.b.c=x.y", required=false, variableArity = true)
    public List<String> refactorings = new ArrayList<>();

    public static String argumentsProblem(PackageRefactorCLI cli) {
        Path from = Paths.get(cli.from);
        Path to   = Paths.get(cli.to);
        if (!Files.isDirectory(from))
            return String.format("From [%s] does not exist or is not a directory"
                                 , cli.from);

        if (!Files.isDirectory(to))
            return String.format("To [%s] does not exist or is not a directory"
                                 , cli.to);

        if (to.toFile().listFiles().length!=0)
            return String.format("To [%s] is not empty - cowardly refusing to proceed"
                                 , cli.to);
        return null;
    }
}
