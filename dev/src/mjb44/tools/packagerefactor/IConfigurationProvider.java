package mjb44.tools.packagerefactor;

import java.util.List;

public interface IConfigurationProvider {

    String       getOrigin                ();
    List<String> getExcludes              ();
    List<String> getAnchors               ();
    List<String> getExclusiveClasses      ();
    List<String> getTranslation           ();
    List<String> getTranslatableFilenames ();
    String       getDestin                ();
    boolean      isRelative               ();
    boolean      isQuiet                  ();    
    
}
