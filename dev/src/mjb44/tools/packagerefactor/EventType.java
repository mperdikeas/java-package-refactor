package mjb44.tools.packagerefactor;

import java.util.List;
import java.util.ArrayList;

public enum EventType {

    BROKEN_SYMLINK                 ("br-sym"              , false, false, "broken symlink in source"                ),
    UNUSED_TRANSLATION             ("un-transl"           , false, true , "translation that remained unused"        ),
    TOTAL_FILES_COPIED             ("ttl-files-copied"    , true , false, "total files copied"                      ),
    NFILES_TRANSLATED              ("n-files-trnsl-regexp", false, false, "number of files translated per regexp"   ),
    FILE_TRANSLATION_REGEXP_UNUSED ("fl-tr-regexp-unused" , false, true , "file translation REGEXP was unused"      ),
    FILES_FILTERED_OUT_N           ("fl-fltrd-out-n"      , false, false, "number of files filtered out in package" ),    
    FILES_FILTERED_OUT_ZERO        ("fl-fltrd-out-zero"   , false, true , "zero files filtered out in package"      )
    ;
    public String  code;
    public boolean singular;
    public boolean unsettling;    
    public String  descr;

    private EventType(String code, boolean singular, boolean unsettling, String descr) {
        this.code        = code;
        this.singular    = singular;
        this.unsettling  = unsettling;
        this.descr       = descr;
    }

    public static EventType fromCode(String code) {
        for (EventType event: EventType.values())
            if (event.code.equals(code))
                return event;
        return null;
    }
}
