package hai913i.tp2.model;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class FactDataset {
    private final Set<ClassFact> classes;
    private final Set<MethodFact> methods;
    private final List<CallArcFact> callArcs;

    public FactDataset(Set<ClassFact> classes, Set<MethodFact> methods, List<CallArcFact> callArcs) {
        this.classes = Collections.unmodifiableSet(new TreeSet<>(classes));
        this.methods = Collections.unmodifiableSet(new TreeSet<>(methods));
        this.callArcs = Collections.unmodifiableList(callArcs);
    }

    public Set<ClassFact> getClasses() {
        return classes;
    }

    public Set<MethodFact> getMethods() {
        return methods;
    }

    public List<CallArcFact> getCallArcs() {
        return callArcs;
    }

    public int getClassCount() {
        return classes.size();
    }

    public int getMethodCount() {
        return methods.size();
    }

    public int getArcCount() {
        return callArcs.size();
    }
}