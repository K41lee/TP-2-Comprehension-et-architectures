package hai913i.tp1.graph;

import hai913i.tp1.model.AppelMethodInfo;
import hai913i.tp1.model.MethodeInfo;
import hai913i.tp1.model.TypeInfo;

import java.util.*;

public class CallGraph {

    public static record Edge(String source, String target, int weight) {}

    private final Set<String> nodes = new HashSet<>();
    private final Map<String, Map<String, Integer>> adjacencyMap = new HashMap<>();

    private int externalCallsCount = 0;
    private int unresolvedCallsCount = 0;
    private int totalInternalCallSites = 0;

    public CallGraph(List<TypeInfo> model) {
        buildGraph(model);
    }

    private void buildGraph(List<TypeInfo> model) {
        // 1. Identification des méthodes surchargées par classe
        Map<String, Set<String>> overloadedMethods = new HashMap<>();
        for (TypeInfo type : model) {
            Map<String, Integer> counts = new HashMap<>();
            for (MethodeInfo m : type.methods()) {
                counts.put(m.name(), counts.getOrDefault(m.name(), 0) + 1);
            }
            Set<String> overloaded = new HashSet<>();
            for (String mName : counts.keySet()) {
                if (counts.get(mName) > 1) {
                    overloaded.add(mName);
                }
            }
            overloadedMethods.put(type.qualifiedName(), overloaded);
        }

        // 2. Initialisation des noeuds
        for (TypeInfo type : model) {
            Set<String> overloaded = overloadedMethods.getOrDefault(type.qualifiedName(), Collections.emptySet());
            for (MethodeInfo method : type.methods()) {
                String key = buildKey(type.qualifiedName(), method.name(), method.parameterCount(), overloaded.contains(method.name()));
                nodes.add(key);
                adjacencyMap.putIfAbsent(key, new LinkedHashMap<>());
            }
        }

        // 3. Construction des arcs
        for (TypeInfo type : model) {
            Set<String> callerOverloaded = overloadedMethods.getOrDefault(type.qualifiedName(), Collections.emptySet());
            for (MethodeInfo method : type.methods()) {
                String callerKey = buildKey(type.qualifiedName(), method.name(), method.parameterCount(), callerOverloaded.contains(method.name()));

                for (AppelMethodInfo call : method.calls()) {
                    if (call.isInternal()) {
                        totalInternalCallSites++;

                        Set<String> targetOverloaded = overloadedMethods.getOrDefault(call.targetClassName(), Collections.emptySet());
                        boolean isOverloaded = targetOverloaded.contains(call.targetMethodName());

                        // Détermination de la clé de la cible
                        String targetKey = resolveTargetKey(model, call, isOverloaded);

                        nodes.add(targetKey);
                        adjacencyMap.putIfAbsent(targetKey, new LinkedHashMap<>());

                        Map<String, Integer> targets = adjacencyMap.get(callerKey);
                        int currentWeight = targets.getOrDefault(targetKey, 0);
                        targets.put(targetKey, currentWeight + 1);
                    } else {
                        externalCallsCount++;
                    }
                }
            }
        }
    }

    private String buildKey(String className, String methodName, int paramCount, boolean isOverloaded) {
        if (isOverloaded) {
            return className + "#" + methodName + "(" + paramCount + ")";
        } else {
            return className + "#" + methodName;
        }
    }

    private String resolveTargetKey(List<TypeInfo> model, AppelMethodInfo call, boolean isOverloaded) {
        if (!isOverloaded) {
            return call.targetClassName() + "#" + call.targetMethodName();
        }
        // GOn cible directement la bonne surcharge
        return call.targetClassName() + "#" + call.targetMethodName() + "(" + call.targetParamCount() + ")";
    }

    public Set<String> getNodes() {
        return nodes;
    }

    public int getNodeCount() {
        return nodes.size();
    }

    public List<Edge> getEdges() {
        List<Edge> edges = new ArrayList<>();
        for (String source : adjacencyMap.keySet()) {
            Map<String, Integer> targets = adjacencyMap.get(source);
            for (String target : targets.keySet()) {
                edges.add(new Edge(source, target, targets.get(target)));
            }
        }
        return edges;
    }

    public int getEdgeCount() {
        return getEdges().size();
    }

    public int getTotalInternalCallSites() {
        return totalInternalCallSites;
    }

    public int getExternalCallsCount() {
        return externalCallsCount;
    }

    public int getUnresolvedCallsCount() {
        return unresolvedCallsCount;
    }

    public Map<String, Integer> getCallees(String caller) {
        return adjacencyMap.getOrDefault(caller, Collections.emptyMap());
    }

    public Map<String, Integer> getCallers(String target) {
        Map<String, Integer> callers = new LinkedHashMap<>();
        for (String source : adjacencyMap.keySet()) {
            Map<String, Integer> targets = adjacencyMap.get(source);
            for (String tKey : targets.keySet()) {
                if (tKey.startsWith(target)) {
                    callers.put(source, targets.get(tKey));
                }
            }
        }
        return callers;
    }

    public void exportToDot(String filePath) {
        try (java.io.PrintWriter out = new java.io.PrintWriter(filePath)) {
            out.println("digraph CallGraph {");
            out.println("  node [shape=box, style=filled, color=lightgrey];");
            for (Edge edge : getEdges()) {
                out.println("  \"" + edge.source() + "\" -> \"" + edge.target() + "\" [label=\"" + edge.weight() + "\"];");
            }
            out.println("}");
            System.out.println("\nFichier exporté avec succès : " + filePath);
        } catch (java.io.FileNotFoundException e) {
            System.err.println("Erreur lors de l'export : " + e.getMessage());
        }
    }
}