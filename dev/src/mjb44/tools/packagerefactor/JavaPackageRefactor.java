package mjb44.tools.packagerefactor;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

import mutil.cli.CLIUtil;
import mutil.json.JsonProvider;

public class JavaPackageRefactor {

    public static void main (String args[]) throws Exception {
        JavaPackageRefactorCLI cli = CLIUtil.cli(JavaPackageRefactor.class.getName(), args, JavaPackageRefactorCLI.class);
        String problem = JavaPackageRefactorCLI.argumentsProblem(cli);
        if (problem!=null) {
            System.out.printf("%s\n", problem);
            System.exit(1);
        }

        Configuration config = null;
        try {
            IConfigurationProvider configProvider = null;            
            if (cli.jsonConfigFile != null) {
                JSONConfiguration configJson = JsonProvider.fromJson(com.google.common.io.Files.toString(Paths.get(cli.jsonConfigFile).toFile()
                                                                                                         , StandardCharsets.UTF_8)
                                                                     , JSONConfiguration.class);
                configProvider = configJson.createConfigurationProvider(cli.origin, cli.destin);
            } else {
                configProvider = cli;
            }
            config = Configuration.fromConfigurationProvider(configProvider);
        } catch (ConfigurationException e) {
            throw new RuntimeException(String.format("bug - this should have been caught by now in the call to %s.argumentsProblem"
                                                     , JavaPackageRefactorCLI.class.getName()));
        }
        dumpConfigurationIfNecessary(cli.dumpJsonConfigFile, cli.dumpJsonConfigFileForce, config);
        doWork(config); // TODO: report statistics at the end and treat verbosity
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

    private static void dumpConfigurationIfNecessary(String dumpJsonConfigFile, String dumpJsonConfigFileForce, Configuration config) throws FileNotFoundException {
        if ((dumpJsonConfigFile!=null) || (dumpJsonConfigFileForce!=null)) {
            String dumpFile = dumpJsonConfigFile!=null?dumpJsonConfigFile:dumpJsonConfigFileForce;
            JSONConfiguration o = config.relativize();
            String json = JsonProvider.toJson(o);
            java.io.PrintWriter pw = new java.io.PrintWriter(dumpFile);
            pw.printf("%s\n", json);
            pw.flush();
            pw.close();
        }
    }
}
