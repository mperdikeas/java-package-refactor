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

    private Path                         origin;
    private Path                         destin;
    private Configuration                config;
    private Set<List<String>>            handledPaths;    
    
    public PackageStructuralHandler(Path origin, Path destin, Configuration config) {
        this.origin = origin;
        this.destin = destin;
        this.config = config;
        this.handledPaths = new LinkedHashSet<>();
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException e) {
        Path         dirRelativeToOrigin              = origin.relativize(dir);
        List<Path>   dirRelativeToOriginComponents    = Lists.newArrayList(dirRelativeToOrigin);
        List<String> dirRelativeToOriginComponentsStr = stringify(dirRelativeToOriginComponents);
        System.out.printf("Reached tree leaf - path components are: [%s]\n"
                          , join(dirRelativeToOriginComponentsStr));
        
        if (!handledPaths.contains(dirRelativeToOriginComponentsStr)) {
            copyToTarget(this.origin, dir, this.destin, dirRelativeToOriginComponentsStr, true);
            updateHandledPaths(dirRelativeToOriginComponentsStr);
        } else {
            copyToTarget(this.origin, dir, this.destin, dirRelativeToOriginComponentsStr, false);
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

    private static void copyToTarget(Path origin, Path dir, Path output, List<String> path, boolean createDirectories) {
        try {
            System.out.printf("copying [%s] to [%s]\n",
                              join(path),
                              output);
            Path dirInOutput = resolve(output, path);
            if (createDirectories)
                Files.createDirectories( dirInOutput );
            else
                Assert.assertTrue(Files.isDirectory(dirInOutput));
            for (File file: dir.toFile().listFiles()) {
                if (file.isDirectory())
                    continue;
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


class DirectoriesOnly implements FileFilter {
    @Override
    public boolean accept(File pathname) {
        return pathname.isDirectory();
    }
}
