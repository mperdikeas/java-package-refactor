package mjb44.tools.packagerefactor;

import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Map;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;

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
                configProvider = configJson.createConfigurationProvider(cli.origin, cli.destin, cli.quiet);
            } else {
                configProvider = cli;
            }
            config = Configuration.fromConfigurationProvider(configProvider);
        } catch (ConfigurationException e) {
            throw new RuntimeException(String.format("bug - this should have been caught by now in the call to %s.argumentsProblem"
                                                     , JavaPackageRefactorCLI.class.getName()));
        }
        dumpConfigurationIfNecessary(cli.dumpJsonConfigFile, cli.dumpJsonConfigFileForce, config);
        boolean unsettlingEventsExist = doWork(config);
        System.exit(unsettlingEventsExist?2:0);
    }

    
    private static boolean doWork(Configuration config) {
        EventLogger eventLogger = new EventLogger();
        try {
            DirectoryRefactorerAndSourceTranslator worker = new DirectoryRefactorerAndSourceTranslator(config, eventLogger);
            Files.walkFileTree(config.origin, worker);
            
            Set<List<String>> providedTranslationKeysCopy = new LinkedHashSet<>(config.translation.keySet());
            populateEventsForUnusedTranslations(eventLogger, providedTranslationKeysCopy, worker.translationsUsed);
            populateEventsForFilesCopied       (eventLogger, worker.nFilesCopied);
            populateEventsForFilesTranslated   (eventLogger, worker.nFilesTranslated);
            populateEventsForFilesFilteredOut  (eventLogger, worker.nFilesFilteredOut, config);            
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.printf("%s\n", eventLogger.report(config.quiet));
        return eventLogger.unsettlingEventsExist();
    }

    private static void populateEventsForUnusedTranslations(EventLogger eventLogger
                                                            , Set<List<String>> providedTranslationKeys
                                                            , Set<List<String>> translationsUsed) {
        Assert.assertTrue(providedTranslationKeys.containsAll(translationsUsed));
        providedTranslationKeys.removeAll(translationsUsed);
        if (!providedTranslationKeys.isEmpty()) {
            for (List<String> key: providedTranslationKeys)
            eventLogger.log(EventType.UNUSED_TRANSLATION
                            , Util.join(key));
        }
    }

    private static void populateEventsForFilesCopied(EventLogger eventLogger, int nFilesCopied) {
        eventLogger.log(EventType.TOTAL_FILES_COPIED, String.valueOf(nFilesCopied));
    }

    private static void populateEventsForFilesTranslated(EventLogger          eventLogger
                                                       , Map<String, Integer> nFilesTranslated) {
        for (String regexp: nFilesTranslated.keySet()) {
            int n = nFilesTranslated.get(regexp);
            eventLogger.log(EventType.NFILES_TRANSLATED, String.format("[%s] : %d", regexp, n));
            if (n==0)
                eventLogger.log(EventType.FILE_TRANSLATION_REGEXP_UNUSED, regexp);
        }
    }

    private static void populateEventsForFilesFilteredOut(EventLogger               eventLogger
                                                          , Map<List<String>, Integer> nFilesFilteredOut
                                                          , Configuration config) {
        Assert.assertTrue(config.exclusiveClasses.keySet().equals(nFilesFilteredOut.keySet()));
        for (List<String> path: nFilesFilteredOut.keySet()) {
            int n = nFilesFilteredOut.get(path);
            if (n>0) {
                Assert.assertFalse(config.exclusiveClasses.get(path).all);
                eventLogger.log(EventType.FILES_FILTERED_OUT_N, String.format("[%s] : %d"
                                                                              , Util.join(path, ".")
                                                                              , n));
            } else if (n==0) {
                if (!config.exclusiveClasses.get(path).all)
                    eventLogger.log(EventType.FILES_FILTERED_OUT_ZERO, Util.join(path, "."));
            }
        }
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
