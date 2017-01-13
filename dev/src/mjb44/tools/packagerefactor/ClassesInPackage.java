package mjb44.tools.packagerefactor;

import java.util.List;
import org.junit.Assert;

public class ClassesInPackage {

    boolean      all;
    List<String> simpleClassNames;
    private ClassesInPackage(boolean all, List<String> simpleClassNames) {
        Assert.assertTrue( (all  && (simpleClassNames==null)) ||
                           (!all && (simpleClassNames!=null)  && (!simpleClassNames.isEmpty())) );
        this.all              = all;
        this.simpleClassNames = simpleClassNames;
    }

    public static ClassesInPackage some(List<String> simpleClassNames) {
        return new ClassesInPackage(false, simpleClassNames);
    }

    public static ClassesInPackage all() {
        return new ClassesInPackage(true, null);
    }
}
