package mjb44.tools.packagerefactor;

import java.util.List;
import java.util.ArrayList;

public enum EventType {

    BROKEN_SYMLINK                 ("br-sym"              , false, "broken symlink in source"                ),
    UNUSED_TRANSLATION             ("un-transl"           , false, "translation that remained unused"        ),
    TOTAL_FILES_COPIED             ("ttl-files-copied"    , true , "total files copied"                      ),
    NFILES_TRANSLATED              ("n-files-trnsl-regexp", false, "number of files translated per regexp"   ),
    FILE_TRANSLATION_REGEXP_UNUSED ("fl-tr-regexp-unused" , false, "file translation REGEXP was unused"      ),
    FILES_FILTERED_OUT_N           ("fl-fltrd-out-n"      , false, "number of files filtered out in package" ),    
    FILES_FILTERED_OUT_ZERO        ("fl-fltrd-out-zero"   , false, "zero files filtered out in package"      )
    ;
    public String  code;
    public boolean singular;    
    public String  descr;

    private EventType(String code, boolean singular, String descr) {
        this.code     = code;
        this.singular = singular;
        this.descr    = descr;
    }

    public static EventType fromCode(String code) {
        for (EventType event: EventType.values())
            if (event.code.equals(code))
                return event;
        return null;
    }
}
