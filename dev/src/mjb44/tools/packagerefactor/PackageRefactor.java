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
        Path origin = Paths.get(cli.origin);
        Path destin = Paths.get(cli.destin);
        Configuration config = new Configuration(origin, destin, cli.anchors);
        workStructure(config);
        workFiles(config);
    }
    private static void workStructure(Configuration config) {
        EventLogger eventLogger = new EventLogger();
        try {
            Files.walkFileTree(config.origin
                               , new PackageStructuralHandler(config, eventLogger));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.printf("%s\n", eventLogger.report());
    }

    private static void workFiles(Configuration config) {
    }
}
