package hai913i.tp2;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import hai913i.tp1.model.TypeInfo;
import hai913i.tp1.parse.JdtParser;
import hai913i.tp1.parse.JdtParser.ParsedFile;
import hai913i.tp1.parse.ProjectSources;
import hai913i.tp1.visitor.CallExtractorVisitor;
import hai913i.tp1.visitor.StructureExtractorVisitor;
import hai913i.tp2.clustering.Cluster;
import hai913i.tp2.clustering.HierarchicalClustering;
import hai913i.tp2.clustering.LinkageType;
import hai913i.tp2.gui.InteractiveClusteringViewer;
import hai913i.tp2.input.CanonicalCsvFactLoader;
import hai913i.tp2.input.FactLoader;
import hai913i.tp2.input.JsonFactLoader;
import hai913i.tp2.input.Tp1ModelFactLoader;
import hai913i.tp2.metrics.CouplingMatrix;
import hai913i.tp2.model.FactDataset;
import hai913i.tp2.display.ConsoleDisplay;

public class MainTp2 {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage : java -cp ... hai913i.tp2.MainTp2 <CHEMIN_ENTREE> [single] [c2] [gui]");
            System.exit(1);
        }

        Path inputPath = Path.of(args[0]);
        if (!Files.exists(inputPath)) {
            System.err.println("Erreur : Le chemin spécifié n'existe pas : " + inputPath);
            System.exit(1);
        }

        // Options CLI (C1: single, C2: c2, C3: gui)
        LinkageType requestedLinkage = LinkageType.AVERAGE;
        boolean includeInheritance = false;
        boolean launchGui = false;

        for (String arg : args) {
            if (arg.equalsIgnoreCase("single") || arg.equalsIgnoreCase("--single")) requestedLinkage = LinkageType.SINGLE;
            if (arg.equalsIgnoreCase("c2") || arg.equalsIgnoreCase("--c2")) includeInheritance = true;
            if (arg.equalsIgnoreCase("gui") || arg.equalsIgnoreCase("--gui")) launchGui = true;
        }

        try {
            // 1. Chargement des faits
            FactDataset dataset = loadDataset(inputPath);
            ConsoleDisplay.displayCheckpointA0(inputPath, dataset, includeInheritance);

            // 2. Calcul du couplage (A1)
            CouplingMatrix couplingMatrix = new CouplingMatrix(dataset, includeInheritance);
            ConsoleDisplay.displayCheckpointA1(couplingMatrix);

            // 3. Exports du graphe (A2)
            Path csvOut = Paths.get("coupling-matrix.csv");
            Path dotOut = Paths.get("coupling-graph.dot");
            couplingMatrix.exportToCsv(csvOut);
            couplingMatrix.exportToDot(dotOut);
            ConsoleDisplay.displayCheckpointA2(couplingMatrix, csvOut, dotOut);

            // 4. Clustering (B1)
            System.out.printf("%n=== POINT DE CONTRÔLE B1 (TP2 - Mode: %s) ===%n", requestedLinkage);
            HierarchicalClustering clustering = new HierarchicalClustering(couplingMatrix, requestedLinkage);
            Cluster dendrogramRoot = clustering.buildDendrogram();

            // 5. Exports du Dendrogramme (B2)
            String prefix = (requestedLinkage == LinkageType.SINGLE) ? "dendrogram-single" : "dendrogram";
            Path nwkOut = Paths.get(prefix + ".nwk");
            Path dotDendroOut = Paths.get(prefix + ".dot");
            dendrogramRoot.exportToNewick(nwkOut);
            dendrogramRoot.exportToDot(dotDendroOut);
            ConsoleDisplay.displayCheckpointB2(requestedLinkage, nwkOut, dotDendroOut, dendrogramRoot.toNewick());

            // 6. Évaluation des Partitions (B3)
            System.out.println("\n=== POINT DE CONTRÔLE B3 (TP2) ===");
            double[] controlThresholds = {0.015, 0.025, 0.045};
            for (double cp : controlThresholds) {
                ConsoleDisplay.displayPartitionEvaluation(clustering, dendrogramRoot, cp);
            }

            // 7. Lancement GUI optionnelle (C3)
            if (launchGui) {
                javax.swing.SwingUtilities.invokeLater(() -> {
                    var viewer = new InteractiveClusteringViewer(couplingMatrix, dendrogramRoot);
                    viewer.setVisible(true);
                });
            }

        } catch (Exception e) {
            System.err.println("Erreur lors de l'exécution du TP2 : " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static FactDataset loadDataset(Path inputPath) throws Exception {
        if (Files.isRegularFile(inputPath) && inputPath.toString().endsWith(".json")) {
            return new JsonFactLoader().loadFacts(inputPath);
        } else if (containsJavaFiles(inputPath)) {
            ProjectSources sources = ProjectSources.of(inputPath);
            List<ParsedFile> files = JdtParser.parse(sources, List.of());
            List<TypeInfo> tp1Model = buildTp1Model(files);
            return Tp1ModelFactLoader.fromTp1Model(tp1Model);
        } else {
            return new CanonicalCsvFactLoader().loadFacts(inputPath);
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