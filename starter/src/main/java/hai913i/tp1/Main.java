package hai913i.tp1;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import hai913i.tp1.display.ConsoleDisplay;
import hai913i.tp1.graph.CallGraph;
import hai913i.tp1.metrics.MetricsCalculator;
import hai913i.tp1.model.TypeInfo;
import hai913i.tp1.visitor.CallExtractorVisitor;
import hai913i.tp1.visitor.StructureExtractorVisitor;
import org.eclipse.jdt.core.compiler.IProblem;

import hai913i.tp1.parse.JdtParser;
import hai913i.tp1.parse.JdtParser.ParsedFile;
import hai913i.tp1.parse.ProjectSources;

public final class Main {

    private Main() {}

    public static void main(String[] args) {
        // 1. Analyse et validation des arguments
        if (args.length < 2) {
            printUsageAndExit();
        }

        String projectPathStr = args[0];
        String action = args[1].toLowerCase();
        int thresholdX = 5;

        if (args.length >= 3) {
            try {
                thresholdX = Integer.parseInt(args[2]);
                if (thresholdX < 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                System.err.println("Erreur : Le seuil X doit être un entier positif (reçu : '" + args[2] + "').");
                System.exit(1);
            }
        }

        // 2. Validation du chemin et accès aux sources
        Path projectPath = Path.of(projectPathStr);
        if (!Files.exists(projectPath) || !Files.isDirectory(projectPath)) {
            System.err.println("Erreur : Le chemin fourni n'existe pas ou n'est pas un dossier valide (" + projectPathStr + ").");
            System.exit(1);
        }

        ProjectSources sources = null;
        try {
            sources = ProjectSources.of(projectPath);
        } catch (IllegalArgumentException | IOException e) {
            System.err.println("Erreur d'accès aux sources : " + e.getMessage());
            System.exit(1);
        }

        // 3. Extraction et Parsing (Composant Analyse)
        List<ParsedFile> files = JdtParser.parse(sources, List.of());
        if (files.isEmpty()) {
            System.err.println("Erreur : Aucun fichier Java trouvé dans le répertoire " + projectPathStr);
            System.exit(1);
        }
        handleCompilationErrors(files);

        // 4. Constitution du modèle de faits (Composant Faits)
        List<TypeInfo> projectModel = buildFactModel(files);
        int totalAppLinesOfCode = calculateTotalLinesOfCode(files);

        // 5. Délégation à l'Affichage et aux composants spécialisés selon l'action demandée
        boolean actionFound = false;

        if (action.equals("ast") || action.equals("all")) {
            ConsoleDisplay.printAst(files);
            actionFound = true;
        }

        if (action.equals("structure") || action.equals("all")) {
            ConsoleDisplay.printStructure(projectModel);
            actionFound = true;
        }

        if (action.equals("appels") || action.equals("all")) {
            ConsoleDisplay.printCalls(projectModel);
            actionFound = true;
        }

        if (action.equals("metrics") || action.equals("all")) {
            MetricsCalculator metrics = new MetricsCalculator(projectModel, totalAppLinesOfCode);
            ConsoleDisplay.printMetrics(metrics, thresholdX);
            actionFound = true;
        }

        if (action.equals("graph") || action.equals("all")) {
            CallGraph callGraph = new CallGraph(projectModel);
            ConsoleDisplay.printGraph(callGraph);
            callGraph.exportToDot("graph.dot"); // Export optionnel
            actionFound = true;
        }

        if (!actionFound) {
            System.err.println("Erreur : Action inconnue '" + action + "'.");
            printUsageAndExit();
        }
    }

    private static void printUsageAndExit() {
        System.err.println("Usage : java -jar target/hai913i-tp1-analyzer.jar <CHEMIN_DU_PROJET> <ACTION> [SEUIL_X]");
        System.err.println("  <ACTION>  : ast | structure | appels | metrics | graph | all");
        System.err.println("  [SEUIL_X] : (Optionnel) Entier positif pour la métrique 11 (défaut = 5).");
        System.exit(1);
    }

    private static void handleCompilationErrors(List<ParsedFile> files) {
        int errors = 0;
        for (ParsedFile file : files) {
            for (IProblem problem : file.unit().getProblems()) {
                if (problem.isError()) {
                    errors++;
                    System.err.println("[Avertissement de syntaxe] " + file.path().getFileName() + ":" + problem.getSourceLineNumber() + " " + problem.getMessage());
                }
            }
        }
        if (errors > 0) {
            System.out.println("Analyse de " + files.size() + " unités de compilation terminées (" + errors + " erreurs de syntaxe ignorées).");
        }
    }

    private static List<TypeInfo> buildFactModel(List<ParsedFile> files) {
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

    private static int calculateTotalLinesOfCode(List<ParsedFile> files) {
        int total = 0;
        for (ParsedFile file : files) {
            try {
                total += Files.readAllLines(file.path()).size();
            } catch (Exception e) {
                // Ignore silent failure for unreadable lines
            }
        }
        return total;
    }
}