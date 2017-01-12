package mjb44.tools.packagerefactor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

import mutil.cli.CLIUtil;
import mutil.json.JsonProvider;

public class PackageRefactor {

    public static void main (String args[]) throws Exception {
        PackageRefactorCLI cli = CLIUtil.cli(PackageRefactor.class.getName(), args, PackageRefactorCLI.class);
        String problem = PackageRefactorCLI.argumentsProblem(cli);
        if (problem!=null) {
            System.out.printf("%s\n", problem);
            System.exit(1);
        }
        // TODO write hygiene code that attempts to internalize the sample JSON file 
        if (false)
        { // transient: obtain a JSON dump
            ConfigurationJson o = new ConfigurationJson(  cli.excludes
                                                          , cli.anchors
                                                          , cli.translation
                                                          , cli.translatableFilenames);
            String json = JsonProvider.toJson(o);
            java.io.PrintWriter pw = new java.io.PrintWriter("dump.json");
            pw.printf("%s\n", json);
            pw.flush();
            pw.close();
        }
        Configuration config = null;
        try {
            IConfigurationProvider configProvider = null;
            if (cli.configFilename!=null) {
                ConfigurationJson configJson = JsonProvider.fromJson(com.google.common.io.Files.toString(Paths.get(cli.configFilename).toFile()
                                                                                                         , StandardCharsets.UTF_8)
                                                                     , ConfigurationJson.class);
                configProvider = configJson.createConfigurationProvider(cli.origin, cli.destin);
            } else {
                configProvider = cli;
            }
            config = Configuration.fromConfigurationProvider(configProvider);
        } catch (ConfigurationException e) {
            throw new RuntimeException(String.format("bug - this should have been caught by now in the call to %s.argumentsProblem"
                                                     , PackageRefactorCLI.class.getName()));
        }
        System.out.printf("%s#main CONFIG is: %s\n"
                          , PackageRefactor.class.getName()
                          , config);
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
}
