package hai913i.tp2.input;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import hai913i.tp2.model.CallArcFact;
import hai913i.tp2.model.ClassFact;
import hai913i.tp2.model.FactDataset;
import hai913i.tp2.model.MethodFact;

public class CanonicalCsvFactLoader implements FactLoader {

    @Override
    public FactDataset loadFacts(Path inputDir) throws IOException {
        Path classesFile = findFile(inputDir, "classes");
        Path methodsFile = findFile(inputDir, "methods");
        Path callsFile = findFile(inputDir, "calls");

        Set<ClassFact> classes = new TreeSet<>();
        Map<String, ClassFact> classMap = new HashMap<>();

        // 1. Classes
        for (String line : Files.readAllLines(classesFile)) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("class;")) continue;
            String className = line.split(";")[0].trim();
            ClassFact c = new ClassFact(className);
            classes.add(c);
            classMap.put(className, c);
        }

        Set<MethodFact> methods = new TreeSet<>();
        Map<String, MethodFact> methodMap = new HashMap<>();

        // 2. Méthodes (method;owner)
        for (String line : Files.readAllLines(methodsFile)) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("method;")) continue;
            String[] parts = line.split(";");
            if (parts.length < 2) continue;
            String methodKey = parts[0].trim();
            String className = parts[1].trim();

            ClassFact declaringClass = classMap.computeIfAbsent(className, ClassFact::new);
            classes.add(declaringClass);

            MethodFact m = new MethodFact(methodKey, declaringClass);
            methods.add(m);
            methodMap.put(methodKey, m);
        }

        Set<CallArcFact> callArcsSet = new LinkedHashSet<>();

        // 3. Arcs d'appel (caller;callerClass;callee;calleeClass;sites)
        for (String line : Files.readAllLines(callsFile)) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("caller;")) continue;
            String[] parts = line.split(";");
            if (parts.length < 3) continue;
            String callerKey = parts[0].trim();
            String targetKey = parts[2].trim(); // 3ème colonne : callee

            MethodFact caller = methodMap.get(callerKey);
            MethodFact target = methodMap.get(targetKey);

            if (caller != null && target != null) {
                callArcsSet.add(new CallArcFact(caller, target));
            }
        }

        return new FactDataset(classes, methods, new ArrayList<>(callArcsSet));
    }

    private Path findFile(Path dir, String keyword) throws IOException {
        if (Files.isRegularFile(dir)) {
            return dir;
        }
        try (var stream = Files.list(dir)) {
            return stream.filter(p -> p.getFileName().toString().contains(keyword) && p.getFileName().toString().endsWith(".csv"))
                    .findFirst()
                    .orElseThrow(() -> new IOException("Fichier CSV non trouvé pour le mot-clé : " + keyword));
        }
    }
}