package mjb44.tools.packagerefactor;

import java.io.IOException;
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
        Configuration config = Configuration.fromCLI(cli);
        System.out.printf("%s#main CONFIG is: %s\n"
                          , PackageRefactor.class.getName()
                          , config);
        doWork(config);
    }

    
    private static void doWork(Configuration config) {
        EventLogger eventLogger = new EventLogger();
        try {
            Files.walkFileTree(config.origin
                               , new DirectoryRefactorer(config, eventLogger));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.printf("%s\n", eventLogger.report());
    }

}
