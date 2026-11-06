package hai913i.tp2;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import hai913i.tp1.model.TypeInfo;
import hai913i.tp1.parse.JdtParser;
import hai913i.tp1.parse.JdtParser.ParsedFile;
import hai913i.tp1.parse.ProjectSources;
import hai913i.tp1.visitor.CallExtractorVisitor;
import hai913i.tp1.visitor.StructureExtractorVisitor;
import hai913i.tp2.clustering.Cluster;
import hai913i.tp2.clustering.HierarchicalClustering;
import hai913i.tp2.input.CanonicalCsvFactLoader;
import hai913i.tp2.input.FactLoader;
import hai913i.tp2.input.JsonFactLoader;
import hai913i.tp2.input.Tp1ModelFactLoader;
import hai913i.tp2.metrics.CouplingMatrix;
import hai913i.tp2.model.ClassFact;
import hai913i.tp2.model.FactDataset;

public class MainTp2 {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage : java -cp ... hai913i.tp2.MainTp2 <CHEMIN_ENTREE>");
            System.exit(1);
        }

        Path inputPath = Path.of(args[0]);
        if (!Files.exists(inputPath)) {
            System.err.println("Erreur : Le chemin spécifié n'existe pas : " + inputPath);
            System.exit(1);
        }

        try {
            FactDataset dataset;

            // 1. Si l'argument pointe vers un fichier JSON
            if (Files.isRegularFile(inputPath) && inputPath.toString().endsWith(".json")) {
                FactLoader loader = new JsonFactLoader();
                dataset = loader.loadFacts(inputPath);
            }
            // 2. Si c'est un dossier contenant du code source Java (Analyseur TP1)
            else if (containsJavaFiles(inputPath)) {
                ProjectSources sources = ProjectSources.of(inputPath);
                List<ParsedFile> files = JdtParser.parse(sources, List.of());
                List<TypeInfo> tp1Model = buildTp1Model(files);
                dataset = Tp1ModelFactLoader.fromTp1Model(tp1Model);
            }
            // 3. Sinon, on tente le chargement d'un dossier CSV canonique
            else {
                FactLoader loader = new CanonicalCsvFactLoader();
                dataset = loader.loadFacts(inputPath);
            }

            System.out.println("=== POINT DE CONTRÔLE A0 (TP2) ===");
            System.out.println("Chemin des faits  : " + inputPath.toAbsolutePath());
            System.out.println("Classes           : " + dataset.getClassCount());
            System.out.println("Méthodes          : " + dataset.getMethodCount());
            System.out.println("Arcs du graphe    : " + dataset.getArcCount());

            CouplingMatrix couplingMatrix = new CouplingMatrix(dataset);

            System.out.println("\n=== POINT DE CONTRÔLE A1 (TP2) ===");
            System.out.println("Arcs inter-classes (E_inter) : " + couplingMatrix.getTotalInterClassArcs());
            System.out.println("Nombre de paires couplées   : " + couplingMatrix.getCouplingScores().size());
            System.out.println("\nCouplages inter-classes triés :");
            for (CouplingMatrix.CouplingEntry entry : couplingMatrix.getSortedCouplingList()) {
                System.out.printf("  %-30s : %d / %d = %.4f%n",
                        entry.getPair(),
                        (int) Math.round(entry.getScore() * couplingMatrix.getTotalInterClassArcs()),
                        couplingMatrix.getTotalInterClassArcs(),
                        entry.getScore());
            }

            System.out.println("\n=== POINT DE CONTRÔLE A2 (TP2) ===");
            couplingMatrix.printFormattedMatrix();
            try {
                java.nio.file.Path csvOut = java.nio.file.Paths.get("coupling-matrix.csv");
                java.nio.file.Path dotOut = java.nio.file.Paths.get("coupling-graph.dot");

                couplingMatrix.exportToCsv(csvOut);
                couplingMatrix.exportToDot(dotOut);

                System.out.println("\nExports A2 générés avec succès :");
                System.out.println("  - CSV : " + csvOut.toAbsolutePath());
                System.out.println("  - DOT : " + dotOut.toAbsolutePath());
            } catch (Exception e) {
                System.err.println("Erreur lors des exports A2 : " + e.getMessage());
            }

            System.out.println("\n=== POINT DE CONTRÔLE B1 (TP2) ===");
            HierarchicalClustering clustering = new HierarchicalClustering(couplingMatrix);
            Cluster dendrogramRoot = clustering.buildDendrogram();

            System.out.println("\n=== POINT DE CONTRÔLE B2 (TP2) ===");
            try {
                java.nio.file.Path nwkOut = java.nio.file.Paths.get("dendrogram.nwk");
                java.nio.file.Path dotDendroOut = java.nio.file.Paths.get("dendrogram.dot");

                dendrogramRoot.exportToNewick(nwkOut);
                dendrogramRoot.exportToDot(dotDendroOut);

                System.out.println("\n=== EXPORTS DENDROGRAMME (B2) ===");
                System.out.println("  - Newick : " + nwkOut.toAbsolutePath());
                System.out.println("  - DOT    : " + dotDendroOut.toAbsolutePath());
                System.out.println("Chaîne Newick : " + dendrogramRoot.toNewick() + ";");
            } catch (Exception e) {
                System.err.println("Erreur d'export B2 : " + e.getMessage());
            }

            System.out.println("\n=== POINT DE CONTRÔLE B3 (TP2) ===");
            double[] controlThresholds = {0.015, 0.025, 0.045};
            for (double cp : controlThresholds) {
                clustering.evaluatePartition(dendrogramRoot, cp);
            }

        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des faits : " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

    }

    private static boolean containsJavaFiles(Path dir) throws Exception {
        if (!Files.isDirectory(dir)) return false;
        try (var stream = Files.walk(dir)) {
            return stream.anyMatch(p -> p.toString().endsWith(".java"));
        }
    }

    private static List<TypeInfo> buildTp1Model(List<ParsedFile> files) {
        StructureExtractorVisitor structureVisitor = new StructureExtractorVisitor();
        for (ParsedFile file : files) {
            structureVisitor.setCurrentUnit(file.unit());
            file.unit().accept(structureVisitor);
        }

        List<TypeInfo> model = structureVisitor.extractedTypes;

        for (ParsedFile file : files) {
            CallExtractorVisitor callVisitor = new CallExtractorVisitor(file.unit());
            file.unit().accept(callVisitor);
        }
        return model;
    }
}