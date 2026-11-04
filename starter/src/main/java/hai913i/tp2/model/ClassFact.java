package hai913i.tp2.model;

import java.util.Objects;

public record ClassFact(String qualifiedName) implements Comparable<ClassFact> {
    public ClassFact {
        Objects.requireNonNull(qualifiedName, "Le nom de la classe ne peut pas être nul");
    }

    @Override
    public int compareTo(ClassFact o) {
        return this.qualifiedName.compareTo(o.qualifiedName);
    }
}