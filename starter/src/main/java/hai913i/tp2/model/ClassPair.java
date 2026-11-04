package hai913i.tp2.model;

import java.util.Objects;

public class ClassPair implements Comparable<ClassPair> {
    private final ClassFact first;
    private final ClassFact second;

    public ClassPair(ClassFact c1, ClassFact c2) {
        if (c1.compareTo(c2) <= 0) {
            this.first = c1;
            this.second = c2;
        } else {
            this.first = c2;
            this.second = c1;
        }
    }

    public ClassFact getFirst() { return first; }
    public ClassFact getSecond() { return second; }

    public boolean contains(ClassFact c) {
        return first.equals(c) || second.equals(c);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClassPair)) return false;
        ClassPair classPair = (ClassPair) o;
        return Objects.equals(first, classPair.first) && Objects.equals(second, classPair.second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }

    @Override
    public int compareTo(ClassPair o) {
        int cmp = this.first.compareTo(o.first);
        if (cmp != 0) return cmp;
        return this.second.compareTo(o.second);
    }

    @Override
    public String toString() {
        return "{" + first.qualifiedName() + ", " + second.qualifiedName() + "}";
    }
}