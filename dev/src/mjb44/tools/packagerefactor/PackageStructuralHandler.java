package mjb44.tools.packagerefactor;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.LinkedHashSet;

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

import org.junit.Assert;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;


public class PackageStructuralHandler extends SimpleFileVisitor<Path> implements FileVisitor<Path> {

    private Configuration                config;
    private EventLogger                  eventLogger;
    private Set<List<String>>            handledPaths;    
    
    public PackageStructuralHandler(Configuration config, EventLogger eventLogger) {
        this.config       = config;
        this.eventLogger  = eventLogger;
        this.handledPaths = new LinkedHashSet<>();
    }

    private Path anchorForDirectory(Path dir) {
        Path rv = null;
        for (Path anchor: this.config.anchors) {
            if (dir.startsWith(anchor)) {
                Assert.assertNull(String.format("multipled anchor found for [%s]", dir.toString())
                                  , rv);
                rv = anchor;
            }
        }
        return rv;
    }

    private List<String> removeAnchor(Path anchor, List<String> pathParts) {
        int i = 0;
        for ( ; i < anchor.getNameCount(); i++) {
            if (!anchor.getName(i).toString().equals(pathParts.get(i)))
                break;
        }
        List<String> rv = pathParts.subList(i+1, pathParts.size());
        System.out.printf("ANCHOR-REMOVAL: Anchor broken off at [%d], path from [%s] changed into [%s]\n"
                          , i
                          , join(pathParts)
                          , join(rv));
        rv.add("foobar");
        return rv;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException e) {
        Path         dirRelativeToOrigin              = this.config.origin.relativize(dir);
        List<Path>   dirRelativeToOriginComponents    = Lists.newArrayList(dirRelativeToOrigin);
        List<String> dirRelativeToOriginComponentsStr = stringify(dirRelativeToOriginComponents);
        System.out.printf("Reached tree leaf - path components are: [%s]\n"
                          , join(dirRelativeToOriginComponentsStr));
        if (!handledPaths.contains(dirRelativeToOriginComponentsStr)) {
            copyToTarget(dir, dirRelativeToOriginComponentsStr, true);
            updateHandledPaths(dirRelativeToOriginComponentsStr);
        } else {
            copyToTarget(dir, dirRelativeToOriginComponentsStr, false);
        }
        return FileVisitResult.CONTINUE;
    }

    private void updateHandledPaths(List<String> combos) {
        System.out.printf("updateHandledPaths(%s)\n", join(combos));
        for (int i = 1; i <= combos.size(); i++) {
            List<String> subCombos = combos.subList(0, i);
            System.out.printf("subCombos for %d are: %s\n"
                              , i
                              , join(subCombos));
            handledPaths.add(subCombos);
        }
    }

    private static List<String> stringify(List<Path> components) {
        List<String> rv = new ArrayList<>();
        for (Path component: components) {
            rv.add(component.toString());
        }
        return rv;
    }
        
    private static String join(List<String> components) {
        List<String> rv = new ArrayList<>();
        for (String component: components) {
            rv.add(component);
        }
        return Joiner.on(", ").join(rv);
    }

    private void copyToTarget(Path dir, List<String> path, boolean createDirectories) {
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
                List<String> outputPath = removeAnchor(anchor, path);
                Path anchorRelativeToOrigin = this.config.origin.relativize(anchor);
                Path anchorInDestination = this.config.destin.resolve(anchorRelativeToOrigin);
                dirInOutput = resolve(anchorInDestination, outputPath);
                System.out.printf("DIR-IN-OUTPUT is: %s\n", dirInOutput);
            }
            if (createDirectories)
                Files.createDirectories( dirInOutput );
            else
                Assert.assertTrue(String.format("[%s] is not a directory"
                                                , dirInOutput)
                                  , Files.isDirectory(dirInOutput));
            for (File file: dir.toFile().listFiles()) {
                if (file.isDirectory())
                    continue;
                if (!file.exists()) {
                    Assert.assertTrue(Files.isSymbolicLink(file.toPath()));
                    this.eventLogger.logBrokenSymlink(file.getPath());
                    continue;
                }
                Files.copy(file.toPath()
                           , dirInOutput.resolve(file.getName()));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Path resolve(Path dir, List<String> parts) {
        Path rv = dir;
        for (String part: parts)
            rv = rv.resolve(part);
        return rv;
    }
}


