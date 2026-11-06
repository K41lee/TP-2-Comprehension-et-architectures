package hai913i.tp2.clustering;

import hai913i.tp2.model.ClassFact;
import java.util.*;

public class Cluster implements Comparable<Cluster> {
    private final int id;
    private final String label;
    private final Set<ClassFact> classes;
    private final Cluster left;
    private final Cluster right;
    private final double mergeScore;

    // Constructeur pour une FEUILLE (Classe initiale avec son ID attribué 0..M-1)
    public Cluster(int id, ClassFact classFact) {
        this.id = id;
        this.label = classFact.qualifiedName();
        this.classes = Collections.singleton(classFact);
        this.left = null;
        this.right = null;
        this.mergeScore = 0.0;
    }

    // Constructeur pour une FUSION (Groupe avec un nouvel ID M, M+1, ...)
    public Cluster(int id, Cluster c1, Cluster c2, double mergeScore) {
        this.id = id;
        // La paire est toujours ordonnée : min(id1, id2) à gauche, max(id1, id2) à droite
        if (c1.getId() < c2.getId()) {
            this.left = c1;
            this.right = c2;
        } else {
            this.left = c2;
            this.right = c1;
        }

        Set<ClassFact> merged = new TreeSet<>(this.left.classes);
        merged.addAll(this.right.classes);
        this.classes = Collections.unmodifiableSet(merged);

        this.mergeScore = mergeScore;
        this.label = "Cluster_" + id;
    }

    public int getId() { return id; }
    public String getLabel() { return label; }
    public Set<ClassFact> getClasses() { return classes; }
    public boolean isLeaf() { return left == null && right == null; }
    public Cluster getLeft() { return left; }
    public Cluster getRight() { return right; }
    public double getMergeScore() { return mergeScore; }

    @Override
    public int compareTo(Cluster o) {
        return Integer.compare(this.id, o.id);
    }

    @Override
    public String toString() {
        if (isLeaf()) {
            return String.format("[%d: %s]", id, label);
        }
        return String.format("[%d: (%s + %s) @ %.4f]", id, left.getLabel(), right.getLabel(), mergeScore);
    }

    /**
     * Représentation au format Newick récursif : ((A,B):score,C):score;
     */
    public String toNewick() {
        if (isLeaf()) {
            return label;
        }
        return String.format(java.util.Locale.US, "(%s,%s):%.4f", left.toNewick(), right.toNewick(), mergeScore);
    }

    /**
     * Export du dendrogramme au format Graphviz DOT.
     */
    public void exportToDot(java.nio.file.Path outputPath) throws java.io.IOException {
        try (java.io.PrintWriter writer = new java.io.PrintWriter(java.nio.file.Files.newBufferedWriter(outputPath))) {
            writer.println("digraph Dendrogram {");
            writer.println("  rankdir=TB;");
            writer.println("  node [fontname=\"Helvetica\", fontsize=10];");
            writer.println("  edge [fontname=\"Helvetica\", fontsize=9];");
            writer.println();

            writeDotNodesAndEdges(writer);

            writer.println("}");
        }
    }

    private void writeDotNodesAndEdges(java.io.PrintWriter writer) {
        if (isLeaf()) {
            writer.printf(java.util.Locale.US,
                    "  node_%d [label=\"%s\\n(id %d)\", shape=box, style=\"rounded,filled\", fillcolor=\"#e3f2fd\"];%n",
                    id, label, id);
        } else {
            writer.printf(java.util.Locale.US,
                    "  node_%d [label=\"Cluster_%d\\nC = %.4f\", shape=ellipse, style=filled, fillcolor=\"#fff3e0\"];%n",
                    id, id, mergeScore);

            // Arête vers l'enfant gauche
            writer.printf(java.util.Locale.US, "  node_%d -> node_%d;%n", id, left.getId());
            left.writeDotNodesAndEdges(writer);

            // Arête vers l'enfant droit
            writer.printf(java.util.Locale.US, "  node_%d -> node_%d;%n", id, right.getId());
            right.writeDotNodesAndEdges(writer);
        }
    }

    /**
     * Export Newick vers fichier texte.
     */
    public void exportToNewick(java.nio.file.Path outputPath) throws java.io.IOException {
        try (java.io.PrintWriter writer = new java.io.PrintWriter(java.nio.file.Files.newBufferedWriter(outputPath))) {
            writer.println(toNewick() + ";");
        }
    }
}