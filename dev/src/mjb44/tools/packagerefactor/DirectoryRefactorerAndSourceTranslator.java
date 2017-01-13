package mjb44.tools.packagerefactor;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.LinkedHashMap;
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


public class DirectoryRefactorerAndSourceTranslator extends SimpleFileVisitor<Path> implements FileVisitor<Path> {

    private static final String JAVA_REGEXP = ".*\\.java";

    private Configuration              config;
    private EventLogger                eventLogger;
    public  Set<List<String>>          translationsUsed;
    public  int                        nFilesCopied;
    public  Map<String, Integer>       nFilesTranslated;
    public  Map<List<String>, Integer> nFilesFilteredOut;
    public DirectoryRefactorerAndSourceTranslator(Configuration config, EventLogger eventLogger) {
        this.config           = config;
        this.eventLogger      = eventLogger;
        this.translationsUsed = new LinkedHashSet<>();
        this.nFilesCopied     = 0;
        this.nFilesTranslated = new LinkedHashMap<>();
        this.nFilesTranslated.put(JAVA_REGEXP, 0);
        for (String regexp: config.translatableFilenames)
            this.nFilesTranslated.put(regexp, 0);
        this.nFilesFilteredOut = new LinkedHashMap<>();
        for (List<String> path: config.exclusiveClasses.keySet())
            this.nFilesFilteredOut.put(path, 0);
    }

    private Path anchorForDirectory(Path dir) {
        Path rv = null;
        for (Path anchor: this.config.anchors) {
            if (dir.startsWith(anchor)) {
                Assert.assertNull(String.format("multiple anchor found for [%s]", dir.toString())
                                  , rv);
                rv = anchor;
            }
        }
        return rv;
    }

    private List<String> removeAnchor(Path anchor, List<String> pathParts) {
        Path anchorRelativeToOrigin = this.config.origin.relativize(anchor);
        int i = 0;
        for ( ; i < anchorRelativeToOrigin.getNameCount(); i++) {
            if (!anchorRelativeToOrigin.getName(i).toString().equals(pathParts.get(i)))
                break;
        }
        List<String> rv = pathParts.subList(i, pathParts.size());
        return rv;
    }

    private List<String> translateAccordingToMap(List<String> in) {
        List<String> rv = new ArrayList<>();

        for (int i = 0; i < in.size() ; ) {
            // find longest match that starts from i
            int lengthOfMatch = in.size()-i;
            for (; lengthOfMatch >0 ; lengthOfMatch--) {
                List<String> candidatePart = in.subList(i, i+lengthOfMatch);
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
                this.translationsUsed.add(part);
                List<String> translatedPart = this.config.translation.get(part);
                rv.addAll(translatedPart);
                i+=lengthOfMatch;
            }
        }
        return rv;
    }
    
    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        if (this.config.excludes.contains(dir)) {
            return FileVisitResult.SKIP_SUBTREE;
        } else
            return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException e) {
        Path         dirRelativeToOrigin              = this.config.origin.relativize(dir);
        List<Path>   dirRelativeToOriginComponents    = Lists.newArrayList(dirRelativeToOrigin);
        List<String> dirRelativeToOriginComponentsStr = stringify(dirRelativeToOriginComponents);
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

    private boolean packageShouldBeCopiedOver(List<String> path) {
        Boolean rv = null; 
        if (this.config.exclusiveClasses.isEmpty())
            rv = true;
        else
            rv = this.config.exclusiveClasses.keySet().contains(path);
        return rv;
    }
        

    private void copyToTarget(Path dir, List<String> path) {
        try {
            Path anchor = anchorForDirectory(dir);
            Path dirInOutput = resolve(this.config.destin, path);
            if (anchor != null) {
                List<String> pathWithAnchorRemoved = removeAnchor(anchor, path);
                List<String> outputPath = translateAccordingToMap(pathWithAnchorRemoved);
                Path anchorRelativeToOrigin = this.config.origin.relativize(anchor);
                Path anchorInDestination = this.config.destin.resolve(anchorRelativeToOrigin);
                dirInOutput = resolve(anchorInDestination, outputPath);
            }
            if ((anchor==null) || ((anchor!=null) && packageShouldBeCopiedOver(removeAnchor(anchor, path)))) {
                Files.createDirectories( dirInOutput );
                for (File file: dir.toFile().listFiles()) {
                    if (file.isDirectory())
                        continue;
                    if (!file.exists()) {
                        Assert.assertTrue(Files.isSymbolicLink(file.toPath()));
                        this.eventLogger.log(EventType.BROKEN_SYMLINK, file.getPath());
                        continue;
                    }
                    boolean fileShouldBeCopiedOver = false;
                    if ((anchor==null) || (this.config.exclusiveClasses.isEmpty()))
                        fileShouldBeCopiedOver = true;
                    else if (!file.getName().toLowerCase().endsWith(".java"))
                        fileShouldBeCopiedOver = true; // all non-Java files should be copied
                    else {
                        List<String> pathWithAnchorRemoved = removeAnchor(anchor, path);                        
                        ClassesInPackage classesInPackage = this.config.exclusiveClasses.get(pathWithAnchorRemoved);
                        Assert.assertNotNull(classesInPackage);
                        if (classesInPackage.all)
                            fileShouldBeCopiedOver = true;
                        else {
                            String fileName        = file.getName();
                            String simpleClassName = fileName.substring(0, fileName.length()-5);
                            if (classesInPackage.simpleClassNames.contains(simpleClassName))
                                fileShouldBeCopiedOver = true;
                            else {
                                fileShouldBeCopiedOver = false;
                                nFilesFilteredOut.put(pathWithAnchorRemoved, nFilesFilteredOut.get(pathWithAnchorRemoved)+1);
                            }
                        }
                    }
                    if (fileShouldBeCopiedOver) {
                        Path destinationFile = dirInOutput.resolve(file.getName());
                        Files.copy(file.toPath(), destinationFile);
                        nFilesCopied++;
                        String regexpFound = fileIsApplicableForContentTranslation(file.toPath().getFileName());
                        if (regexpFound!=null) {
                            translateFile(destinationFile);
                            Assert.assertTrue(String.format("Regexp [%s] not found in [%s]"
                                                            , regexpFound
                                                            , nFilesTranslated.keySet())
                                              , nFilesTranslated.containsKey(regexpFound));
                            nFilesTranslated.put(regexpFound, nFilesTranslated.get(regexpFound)+1);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String fileIsApplicableForContentTranslation(Path file) {
        Set<String> effectiveTranslatableFilenames = new LinkedHashSet<>(this.config.translatableFilenames);
        effectiveTranslatableFilenames.add(JAVA_REGEXP);
        for (String regexp: effectiveTranslatableFilenames) {
            Pattern pattern = Pattern.compile(regexp);
            Matcher matcher = pattern.matcher(file.toString());
            if (matcher.matches()) {
                return regexp;
            }
        }
        return null;
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


