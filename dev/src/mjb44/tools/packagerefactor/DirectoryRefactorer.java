package mjb44.tools.packagerefactor;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.File;
import java.io.IOException;
import java.io.FileFilter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.FileVisitor;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.FileVisitResult;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import org.junit.Assert;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import static mjb44.tools.packagerefactor.Util.join;


public class DirectoryRefactorer extends SimpleFileVisitor<Path> implements FileVisitor<Path> {

    private Configuration     config;
    private EventLogger       eventLogger;
    
    public DirectoryRefactorer(Configuration config, EventLogger eventLogger) {
        this.config       = config;
        this.eventLogger  = eventLogger;
    }

    private Path anchorForDirectory(Path dir) {
        Path rv = null;
        for (Path anchor: this.config.anchors) {
            if (dir.startsWith(anchor)) {
                Assert.assertNull(String.format("multiple anchor found for [%s]", dir.toString())
                                  , rv);
                rv = anchor;
            } else
                System.out.printf("dir [%s] does not start with [%s]\n", dir, anchor);
        }
        System.out.printf("ANCHOR-FOUND: for dir [%s], anchor is: [%s]\n"
                          , dir
                          , rv);
        return rv;
    }

    private List<String> removeAnchor(Path anchor, List<String> pathParts) {
        Path anchorRelativeToOrigin = this.config.origin.relativize(anchor);
        System.out.printf("Anchor changed from [%s] to [%s] (path parts are: [%s])\n"
                          , anchor
                          , anchorRelativeToOrigin
                          , pathParts);
        int i = 0;
        for ( ; i < anchorRelativeToOrigin.getNameCount(); i++) {
            if (!anchorRelativeToOrigin.getName(i).toString().equals(pathParts.get(i)))
                break;
        }
        List<String> rv = pathParts.subList(i, pathParts.size());
        System.out.printf("ANCHOR-REMOVAL: Anchor broken off at [%d], path from [%s] changed into [%s]\n"
                          , i
                          , join(pathParts)
                          , join(rv));
        return rv;
    }

    private List<String> translateAccordingToMapOld(List<String> in) {
        List<String> rv = new ArrayList<>();
        int startOfSearchForNextMatch = 0;
        for (int i = 0; i < in.size() ; i++) {
            List<String> part = in.subList(startOfSearchForNextMatch, i+1);
            if (this.config.translation.containsKey(part)) {
                rv.subList(rv.size()-(part.size()-1), rv.size()).clear();
                List<String> translatedPart = this.config.translation.get(part);
                System.out.printf("Successfully partially translated [%s] to [%s]\n"
                                  , join(part)
                                  , join(translatedPart));
                rv.addAll(translatedPart);
                startOfSearchForNextMatch = i+1;
            } else {
                rv.add(in.get(i));
            }
        }
        if (!in.equals(rv))
            System.out.printf("Full translation of [%s] to [%s]\n"
                              , join(in)
                              , join(rv));
        return rv;
    }

    private List<String> translateAccordingToMap(List<String> in) {
        List<String> rv = new ArrayList<>();

        for (int i = 0; i < in.size() ; ) {
            // find longest match that starts from i
            int lengthOfMatch = in.size()-i;
            for (; lengthOfMatch >0 ; lengthOfMatch--) {
                System.out.printf("CHECKING-BEFORE i=[%d]\n", i);
                List<String> candidatePart = in.subList(i, i+lengthOfMatch);
                System.out.printf("CHECKING whether part [%s] is in map\n", join(candidatePart));
                if (this.config.translation.containsKey(candidatePart))
                    break;
            }
            if (lengthOfMatch==0) {
                rv.add(in.get(i)); // nothing to do here, increase index by 1
                i++;
            } else {
                // copy over the translated match and increase index by the lengthOfMatch
                List<String> part = in.subList(i, i+lengthOfMatch);
                Assert.assertTrue(this.config.translation.containsKey(part));
                List<String> translatedPart = this.config.translation.get(part);
                rv.addAll(translatedPart);
                i+=lengthOfMatch;
            }
        }
        if (!in.equals(rv))
            System.out.printf("Full translation of [%s] to [%s]\n"
                              , join(in)
                              , join(rv));
        return rv;
    }
    
    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        if (this.config.excludes.contains(dir)) {
            System.out.printf("EXCLUDES activated for %s\n", dir);
            return FileVisitResult.SKIP_SUBTREE;
        } else
            return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException e) {
        Path         dirRelativeToOrigin              = this.config.origin.relativize(dir);
        List<Path>   dirRelativeToOriginComponents    = Lists.newArrayList(dirRelativeToOrigin);
        List<String> dirRelativeToOriginComponentsStr = stringify(dirRelativeToOriginComponents);
        System.out.printf("Reached tree leaf - path components are: [%s]\n"
                          , join(dirRelativeToOriginComponentsStr));
        copyToTarget(dir, dirRelativeToOriginComponentsStr);
        return FileVisitResult.CONTINUE;
    }

    private static List<String> stringify(List<Path> components) {
        List<String> rv = new ArrayList<>();
        for (Path component: components) {
            rv.add(component.toString());
        }
        return rv;
    }
        

    private void copyToTarget(Path dir, List<String> path) {
        try {
            System.out.printf("copying [%s] to [%s]\n",
                              join(path),
                              this.config.destin);
            Path anchor = anchorForDirectory(dir);
            Path dirInOutput = resolve(this.config.destin, path);
            if (anchor != null) {
                System.out.printf("PACKAGE dir [%s] is in anchors, path is [%s]\n"
                                  , dir
                                  , join(path));
                List<String> outputPath = translateAccordingToMap(removeAnchor(anchor, path));
                Path anchorRelativeToOrigin = this.config.origin.relativize(anchor);
                Path anchorInDestination = this.config.destin.resolve(anchorRelativeToOrigin);
                dirInOutput = resolve(anchorInDestination, outputPath);
                System.out.printf("DIR-IN-OUTPUT is: %s\n", dirInOutput);
            }

            Files.createDirectories( dirInOutput );
            for (File file: dir.toFile().listFiles()) {
                if (file.isDirectory())
                    continue;
                if (!file.exists()) {
                    Assert.assertTrue(Files.isSymbolicLink(file.toPath()));
                    this.eventLogger.log(EventType.BROKEN_SYMLINK, file.getPath());
                    continue;
                }
                Path destinationFile = dirInOutput.resolve(file.getName());
                Files.copy(file.toPath(), destinationFile);
                if (fileIsApplicableForContentTranslation(file.toPath().getFileName()))
                    translateFile(destinationFile);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean fileIsApplicableForContentTranslation(Path file) {
        for (String regexp: this.config.translatableFilenames) {
            Pattern pattern = Pattern.compile(regexp);
            Matcher matcher = pattern.matcher(file.toString());
            if (matcher.matches()) {
                System.out.printf("REGEXP-FNAME-MATCH: file [%s] matches [%s]\n"
                                  , file
                                  , regexp);
                return true;
            }
        }
        return false;
    }

    private void translateFile(Path file) {
        try {
            String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            for (List<String> key: this.config.translation.keySet()) {
                List<String> value = this.config.translation.get(key);
                content = content.replaceAll(packageRegularExpression(key)
                                             , packageValue(value));
                Files.write(file, content.getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String packageRegularExpression(List<String> packageComponents) {
        return Joiner.on("\\.").join(packageComponents);
    }

    private static String packageValue(List<String> packageComponents) {
        return Joiner.on(".").join(packageComponents);
    }
    
    private static Path resolve(Path dir, List<String> parts) {
        Path rv = dir;
        for (String part: parts)
            rv = rv.resolve(part);
        return rv;
    }
}


