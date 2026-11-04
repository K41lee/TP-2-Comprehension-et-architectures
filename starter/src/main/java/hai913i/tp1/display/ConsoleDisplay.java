package hai913i.tp1.display;

import hai913i.tp1.graph.CallGraph;
import hai913i.tp1.metrics.MetricsCalculator;
import hai913i.tp1.model.MethodeInfo;
import hai913i.tp1.model.TypeInfo;
import hai913i.tp1.model.AppelMethodInfo;

import java.util.List;
import java.util.Map;

public class ConsoleDisplay {

    // Affichage de l'arbre AST (A1)
    public static void printAst(List<hai913i.tp1.parse.JdtParser.ParsedFile> files) {
        System.out.println("\n=== AFFICHAGE DE L'ARBRE AST (A1) ===");
        hai913i.tp1.visitor.ASTStructurePrinterVisitor printerVisitor = new hai913i.tp1.visitor.ASTStructurePrinterVisitor();

        for (hai913i.tp1.parse.JdtParser.ParsedFile file : files) {
            String fileName = file.path().getFileName().toString();
            // On se limite aux fichiers demandés dans le TP pour ne pas inonder la console
            if (fileName.equals("Dvd.java") || fileName.equals("Category.java")) {
                System.out.println("\n--- Arbre pour " + fileName + " ---");
                file.unit().accept(printerVisitor);
            }
        }
    }

    // Affichage de la structure (A2 / B1)
    public static void printStructure(List<TypeInfo> model) {
        System.out.println("\n=== BILAN DE LA STRUCTURE (A2/B1) ===");
        int totalClasses = 0, totalInterfaces = 0, totalEnums = 0;
        int totalMethods = 0, totalConstructors = 0, totalFields = 0;

        for (TypeInfo type : model) {
            if ("interface".equals(type.genre())) totalInterfaces++;
            else if ("enum".equals(type.genre())) totalEnums++;
            else totalClasses++;

            totalFields += type.fields().size();
            for (MethodeInfo m : type.methods()) {
                totalMethods++;
                if (m.isConstructor()) totalConstructors++;
            }
        }

        System.out.println("Nombre total de types    : " + model.size() + " (Classes: " + totalClasses + ", Interfaces: " + totalInterfaces + ", Enums: " + totalEnums + ")");
        System.out.println("Nombre total de méthodes : " + totalMethods + " (dont constructeurs: " + totalConstructors + ")");
        System.out.println("Nombre total d'attributs : " + totalFields);
    }

    // Affichage des appels (A3)
    public static void printCalls(List<TypeInfo> model) {
        System.out.println("\n=== BILAN DES APPELS (A3) ===");
        int totalInternal = 0, totalExternal = 0;

        for (TypeInfo type : model) {
            for (MethodeInfo m : type.methods()) {
                for (AppelMethodInfo call : m.calls()) {
                    if (call.isInternal()) totalInternal++;
                    else totalExternal++;
                }
            }
        }

        System.out.println("Nombre total d'appels enregistrés : " + (totalInternal + totalExternal));
        System.out.println("Appels internes au projet         : " + totalInternal);
        System.out.println("Appels externes (JDK/lib)         : " + totalExternal);
    }

    // Affichage des métriques (B2)
    public static void printMetrics(MetricsCalculator metrics, int thresholdX) {
        System.out.println("\n=== BILAN DES MÉTRIQUES (B2) ===");
        System.out.println("1. Nombre de classes                 : " + metrics.getClassCount());
        System.out.println("2. Nombre de lignes de code (LOC)    : " + metrics.getTotalAppLinesOfCode());
        System.out.println("3. Nombre total de méthodes          : " + metrics.getTotalMethods());
        System.out.println("4. Nombre total de paquetages        : " + metrics.getTotalPackages());
        System.out.println("5. Moyenne méthodes / classe         : " + String.format("%.2f", metrics.getAverageMethodsPerClass()));
        System.out.println("6. Moyenne LOC / méthode (avec corps): " + String.format("%.2f", metrics.getAverageLinesOfCodePerMethodWithBody()));
        System.out.println("7. Moyenne attributs / classe        : " + String.format("%.2f", metrics.getAverageFieldsPerClass()));

        System.out.println("\n8. Top 10% des classes (méthodes) :");
        metrics.getTop10PercentClassesByMethods().forEach(t -> System.out.println("   - " + t.qualifiedName() + " (" + t.methods().size() + ")"));

        System.out.println("\n9. Top 10% des classes (attributs) :");
        metrics.getTop10PercentClassesByFields().forEach(t -> System.out.println("   - " + t.qualifiedName() + " (" + t.fields().size() + ")"));

        System.out.println("\n10. Intersection des deux catégories :");
        metrics.getClassesInBothTop10Percent().forEach(t -> System.out.println("   - " + t.qualifiedName()));

        System.out.println("\n11. Classes ayant plus de " + thresholdX + " méthodes :");
        metrics.getClassesWithMoreThanXMethods(thresholdX).forEach(t -> System.out.println("   - " + t.qualifiedName() + " (" + t.methods().size() + ")"));

        System.out.println("\n12. Top 10% des méthodes les plus longues par classe :");
        for (Map.Entry<String, List<MethodeInfo>> entry : metrics.getTop10PercentMethodsByLocPerClass().entrySet()) {
            if (!entry.getValue().isEmpty()) {
                System.out.println("   * " + entry.getKey() + " :");
                entry.getValue().forEach(m -> System.out.println("     - " + m.name() + " (" + m.linesOfCode() + " LOC)"));
            }
        }

        System.out.println("\n13. Nombre maximal de paramètres : " + metrics.getMaxParametersCount());
        metrics.getMethodsWithMaxParameters().forEach(m -> System.out.println("   - " + m));
    }

    // Affichage du graphe d'appel (B3)
    public static void printGraph(CallGraph callGraph) {
        System.out.println("\n=== BILAN DU GRAPHE D'APPEL (B3) ===");
        System.out.println("Nombre de noeuds (méthodes)   : " + callGraph.getNodeCount());
        System.out.println("Nombre d'arcs orientés       : " + callGraph.getEdgeCount());
        System.out.println("Sites d'appels internes      : " + callGraph.getTotalInternalCallSites());
        System.out.println("Appels externes (JDK/lib)    : " + callGraph.getExternalCallsCount());
        System.out.println("Appels non résolus           : " + callGraph.getUnresolvedCallsCount());
    }
}