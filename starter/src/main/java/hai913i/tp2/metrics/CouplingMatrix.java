package hai913i.tp2.metrics;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import hai913i.tp2.model.CallArcFact;
import hai913i.tp2.model.ClassFact;
import hai913i.tp2.model.ClassPair;
import hai913i.tp2.model.FactDataset;

public class CouplingMatrix {

    public static class CouplingEntry implements Comparable<CouplingEntry> {
        private final ClassPair pair;
        private final double score;

        public CouplingEntry(ClassPair pair, double score) {
            this.pair = pair;
            this.score = score;
        }

        public ClassPair getPair() { return pair; }
        public double getScore() { return score; }

        @Override
        public int compareTo(CouplingEntry o) {
            int cmp = Double.compare(o.score, this.score);
            if (cmp != 0) {
                return cmp;
            }
            return this.pair.compareTo(o.pair);
        }

        @Override
        public String toString() {
            return pair + " = " + score;
        }
    }

    private final FactDataset dataset;
    private final Map<ClassPair, Integer> rawRelations = new HashMap<>();
    private final Map<ClassPair, Double> couplingScores = new HashMap<>();
    private int totalInterClassArcs = 0;

    public CouplingMatrix(FactDataset dataset) {
        this.dataset = dataset;
        computeCoupling();
        validateInvariants();
    }

    private void computeCoupling() {
        for (CallArcFact arc : dataset.getCallArcs()) {
            ClassFact callerClass = arc.caller().declaringClass();
            ClassFact targetClass = arc.target().declaringClass();

            if (!callerClass.equals(targetClass)) {
                totalInterClassArcs++;
                ClassPair pair = new ClassPair(callerClass, targetClass);
                rawRelations.put(pair, rawRelations.getOrDefault(pair, 0) + 1);
            }
        }

        if (totalInterClassArcs > 0) {
            for (Map.Entry<ClassPair, Integer> entry : rawRelations.entrySet()) {
                double score = (double) entry.getValue() / totalInterClassArcs;
                couplingScores.put(entry.getKey(), score);
            }
        }
    }

    private void validateInvariants() {
        if (totalInterClassArcs > 0) {
            double sum = 0.0;
            for (double score : couplingScores.values()) {
                sum += score;
            }
            if (Math.abs(sum - 1.0) > 1e-6) {
                throw new IllegalStateException("Invariant violé : la somme vaut " + sum);
            }
        }
    }

    public FactDataset getDataset() {
        return dataset;
    }

    public int getTotalInterClassArcs() {
        return totalInterClassArcs;
    }

    public double getCoupling(ClassFact c1, ClassFact c2) {
        if (c1.equals(c2)) return 0.0;
        ClassPair pair = new ClassPair(c1, c2);
        return couplingScores.getOrDefault(pair, 0.0);
    }

    public Map<ClassPair, Double> getCouplingScores() {
        return Collections.unmodifiableMap(couplingScores);
    }

    public List<CouplingEntry> getSortedCouplingList() {
        List<CouplingEntry> list = new ArrayList<>();
        for (Map.Entry<ClassPair, Double> entry : couplingScores.entrySet()) {
            list.add(new CouplingEntry(entry.getKey(), entry.getValue()));
        }
        Collections.sort(list);
        return list;
    }

    public void printFormattedMatrix() {
        List<ClassFact> classes = new ArrayList<>(dataset.getClasses());
        Collections.sort(classes);
        int n = classes.size();

        List<String> simpleNames = new ArrayList<>();
        for (ClassFact c : classes) {
            String qName = c.qualifiedName();
            int lastDot = qName.lastIndexOf('.');
            simpleNames.add(lastDot >= 0 ? qName.substring(lastDot + 1) : qName);
        }

        System.out.println("\n=== MATRICE DE COUPLAGE COMPLÈTE (A2) ===");

        System.out.printf("%-18s", "");
        for (String name : simpleNames) {
            String header = name.length() > 9 ? name.substring(0, 9) : name;
            System.out.printf("%-10s", header);
        }
        System.out.println();

        for (int i = 0; i < n; i++) {
            ClassFact c1 = classes.get(i);
            String rowName = simpleNames.get(i);
            String rowHeader = rowName.length() > 17 ? rowName.substring(0, 17) : rowName;
            System.out.printf("%-18s", rowHeader);

            for (int j = 0; j < n; j++) {
                ClassFact c2 = classes.get(j);
                double score = getCoupling(c1, c2);
                if (score == 0.0) {
                    System.out.printf("%-10s", "  .  ");
                } else {
                    System.out.printf("%-10.4f", score);
                }
            }
            System.out.println();
        }
    }

    public void exportToCsv(Path outputPath) throws IOException {
        List<ClassFact> classes = new ArrayList<>(dataset.getClasses());
        Collections.sort(classes);

        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(outputPath))) {
            writer.print("class");
            for (ClassFact c : classes) {
                writer.print(";" + c.qualifiedName());
            }
            writer.println();

            for (ClassFact c1 : classes) {
                writer.print(c1.qualifiedName());
                for (ClassFact c2 : classes) {
                    double score = getCoupling(c1, c2);
                    writer.printf(Locale.US, ";%.4f", score);
                }
                writer.println();
            }
        }
    }

    public void exportToDot(Path outputPath) throws IOException {
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(outputPath))) {
            writer.println("graph CouplingGraph {");
            writer.println("  rankdir=LR;");
            writer.println("  node [shape=box, style=\"rounded,filled\", fillcolor=\"#f8f9fa\", fontname=\"Helvetica\"];");
            writer.println("  edge [fontname=\"Helvetica\", fontsize=10];");
            writer.println();

            for (Map.Entry<ClassPair, Double> entry : couplingScores.entrySet()) {
                ClassPair pair = entry.getKey();
                double score = entry.getValue();

                String name1 = pair.getFirst().qualifiedName();
                String name2 = pair.getSecond().qualifiedName();

                writer.printf(Locale.US,
                        "  \"%s\" -- \"%s\" [label=\"%.4f\", penwidth=%.2f];%n",
                        name1, name2, score, 1.0 + (score * 5.0));
            }

            writer.println("}");
        }
    }
}