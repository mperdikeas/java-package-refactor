package mjb44.tools.packagerefactor;

import java.util.List;

public class ConfigurationJson {

    public List<String> excludes;
    public List<String> anchors;
    public List<String> translation;
    public List<String> translatableFilenames;


    public ConfigurationJson(  List<String> excludes
                             , List<String> anchors
                             , List<String> translation
                             , List<String> translatableFilenames) {
        this.excludes = excludes;
        this.anchors = anchors;
        this.translation = translation;
        this.translatableFilenames = translatableFilenames;
    }

    public IConfigurationProvider createConfigurationProvider(final String origin, final String destin) {
        final ConfigurationJson that = this;
        return new IConfigurationProvider(){
            @Override public String       getOrigin                () {return origin;}
            @Override public List<String> getExcludes              () {return that.excludes;}
            @Override public List<String> getAnchors               () {return that.anchors;}
            @Override public List<String> getTranslation           () {return that.translation;}
            @Override public List<String> getTranslatableFilenames () {return that.translatableFilenames;}
            @Override public String       getDestin                () {return destin;}
            @Override public boolean      getRelative              () {return true;}

        };

    }
}
