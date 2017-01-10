package mjb44.tools.packagerefactor;

import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import mutil.cli.CLIUtil;

public class PackageRefactor {

    public static void main (String args[]) {
        PackageRefactorCLI cli = CLIUtil.cli(PackageRefactor.class.getName(), args, PackageRefactorCLI.class);
        String problem = PackageRefactorCLI.argumentsProblem(cli);
        if (problem!=null) {
            System.out.printf("%s\n", problem);
            System.exit(1);
        }
        Path from = Paths.get(cli.from);
        Path to   = Paths.get(cli.to);
        workStructure(from, to);
        workFiles(to);
    }
    private static void workStructure(Path from, Path to) {
        try {
            Files.walkFileTree(from
                               , new PackageStructuralHandler(from
                                                              , to
                                                              , new Configuration()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void workFiles(Path output) {
    }
}
