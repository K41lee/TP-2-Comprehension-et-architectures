package hai913i.tp2.display;

import hai913i.tp2.clustering.Cluster;
import hai913i.tp2.clustering.HierarchicalClustering;
import hai913i.tp2.clustering.LinkageType;
import hai913i.tp2.metrics.CouplingMatrix;
import hai913i.tp2.model.ClassFact;
import hai913i.tp2.model.FactDataset;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ConsoleDisplay {

    public static void displayCheckpointA0(Path inputPath, FactDataset dataset, boolean includeInheritance) {
        System.out.println("=== POINT DE CONTRÔLE A0 (TP2) ===");
        System.out.println("Chemin des faits   : " + inputPath.toAbsolutePath());
        System.out.println("Classes            : " + dataset.getClassCount());
        System.out.println("Méthodes           : " + dataset.getMethodCount());
        System.out.println("Arcs du graphe     : " + dataset.getArcCount());
        System.out.println("Mode Héritage (C2) : " + (includeInheritance ? "ACTIVÉ" : "DÉSACTIVÉ"));
    }

    public static void displayCheckpointA1(CouplingMatrix couplingMatrix) {
        System.out.println("\n=== POINT DE CONTRÔLE A1 (TP2) ===");
        System.out.println("Arcs inter-classes (E_inter) : " + couplingMatrix.getTotalInterClassArcs());
        System.out.println("Nombre de paires couplées   : " + couplingMatrix.getCouplingScores().size());
        System.out.println("\nCouplages inter-classes triés :");
        for (CouplingMatrix.CouplingEntry entry : couplingMatrix.getSortedCouplingList()) {
            System.out.printf(Locale.US, "  %-30s : %d / %d = %.4f%n",
                    entry.getPair(),
                    (int) Math.round(entry.getScore() * couplingMatrix.getTotalInterClassArcs()),
                    couplingMatrix.getTotalInterClassArcs(),
                    entry.getScore());
        }
    }

    public static void displayCheckpointA2(CouplingMatrix couplingMatrix, Path csvOut, Path dotOut) {
        System.out.println("\n=== POINT DE CONTRÔLE A2 (TP2) ===");
        couplingMatrix.printFormattedMatrix();
        System.out.println("\nExports A2 générés avec succès :");
        System.out.println("  - CSV : " + csvOut.toAbsolutePath());
        System.out.println("  - DOT : " + dotOut.toAbsolutePath());
    }

    public static void displayCheckpointB2(LinkageType linkage, Path nwkOut, Path dotDendroOut, String newickString) {
        System.out.println("\n=== POINT DE CONTRÔLE B2 (TP2 - " + linkage + ") ===");
        System.out.println("Exports dendrogramme :");
        System.out.println("  - Newick : " + nwkOut.toAbsolutePath());
        System.out.println("  - DOT    : " + dotDendroOut.toAbsolutePath());
        System.out.println("Chaîne Newick : " + newickString + ";");
    }

    public static void displayPartitionEvaluation(HierarchicalClustering clustering, Cluster root, double cp) {
        List<Cluster> partition = clustering.extractPartitionByThreshold(root, cp);
        int totalClasses = root.getClasses().size();
        int maxAllowedModules = totalClasses / 2;
        boolean isViolated = partition.size() > maxAllowedModules;

        System.out.printf(Locale.US, "%n--- Étape B3 (%s) : Coupe pour CP = %.3f ---%n", clustering.getLinkageType(), cp);
        System.out.printf("Nombre de modules obtenus : %d | Limite autorisée (M/2 = %d/2) : %d%n",
                partition.size(), totalClasses, maxAllowedModules);

        if (isViolated) {
            System.out.printf(Locale.US, "CONTRAINTE M/2 VIOLÉE (%d > %d) : la coupe doit être rejetée !%n",
                    partition.size(), maxAllowedModules);
        } else {
            System.out.printf(Locale.US, "CONTRAINTE M/2 RESPECTÉE (%d <= %d).%n",
                    partition.size(), maxAllowedModules);
        }

        System.out.println("Composition des modules :");
        for (int i = 0; i < partition.size(); i++) {
            Cluster mod = partition.get(i);
            double internalCoupling = clustering.computeInternalCoupling(mod);

            List<String> simpleClassNames = new ArrayList<>();
            for (ClassFact c : mod.getClasses()) {
                String qName = c.qualifiedName();
                int lastDot = qName.lastIndexOf('.');
                simpleClassNames.add(lastDot >= 0 ? qName.substring(lastDot + 1) : qName);
            }

            System.out.printf(Locale.US, "  Module %-2d [id %-2d, %2d classe(s), C_interne = %.4f] : %s%n",
                    i + 1, mod.getId(), mod.getClasses().size(), internalCoupling, simpleClassNames);
        }
    }
}