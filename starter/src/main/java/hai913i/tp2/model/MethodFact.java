package hai913i.tp2.model;

import java.util.Objects;

public record MethodFact(String methodKey, ClassFact declaringClass) implements Comparable<MethodFact> {
    public MethodFact {
        Objects.requireNonNull(methodKey, "La clé de méthode ne peut pas être nulle");
        Objects.requireNonNull(declaringClass, "La classe déclaratrice ne peut pas être nulle");
    }

    @Override
    public int compareTo(MethodFact o) {
        return this.methodKey.compareTo(o.methodKey);
    }
}