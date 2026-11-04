package hai913i.tp2.input;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hai913i.tp2.model.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class JsonFactLoader implements FactLoader {

    @Override
    public FactDataset loadFacts(Path jsonFile) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonFile.toFile());

        Set<ClassFact> classes = new TreeSet<>();
        Map<String, ClassFact> classMap = new HashMap<>();

        // 1. Classes
        if (root.has("classes")) {
            for (JsonNode node : root.get("classes")) {
                String name = node.has("name") ? node.get("name").asText() : node.asText();
                ClassFact c = new ClassFact(name);
                classes.add(c);
                classMap.put(name, c);
            }
        }

        Set<MethodFact> methods = new TreeSet<>();
        Map<String, MethodFact> methodMap = new HashMap<>();

        // 2. Méthodes ("id" et "owner")
        if (root.has("methods")) {
            for (JsonNode node : root.get("methods")) {
                String key = node.has("id") ? node.get("id").asText() : node.get("method").asText();
                String className = node.get("owner").asText();

                ClassFact declaringClass = classMap.computeIfAbsent(className, ClassFact::new);
                classes.add(declaringClass);

                MethodFact m = new MethodFact(key, declaringClass);
                methods.add(m);
                methodMap.put(key, m);
            }
        }

        Set<CallArcFact> callArcs = new LinkedHashSet<>();

        // 3. Arcs d'appel ("caller" et "callee")
        if (root.has("calls")) {
            for (JsonNode node : root.get("calls")) {
                String callerKey = node.get("caller").asText();
                String targetKey = node.get("callee").asText();

                MethodFact caller = methodMap.get(callerKey);
                MethodFact target = methodMap.get(targetKey);

                if (caller != null && target != null) {
                    callArcs.add(new CallArcFact(caller, target));
                }
            }
        }

        return new FactDataset(classes, methods, new ArrayList<>(callArcs));
    }
}