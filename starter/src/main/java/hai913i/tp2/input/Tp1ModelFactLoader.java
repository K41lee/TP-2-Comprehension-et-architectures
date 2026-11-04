package hai913i.tp2.input;

import java.util.*;
import hai913i.tp1.model.AppelMethodInfo;
import hai913i.tp1.model.MethodeInfo;
import hai913i.tp1.model.TypeInfo;
import hai913i.tp2.model.*;

public class Tp1ModelFactLoader {

    public static FactDataset fromTp1Model(List<TypeInfo> model) {
        Set<ClassFact> classes = new TreeSet<>();
        Set<MethodFact> methods = new TreeSet<>();
        Map<String, MethodFact> methodMap = new HashMap<>();
        Set<CallArcFact> callArcsSet = new LinkedHashSet<>(); // Garantit l'unicité des arcs

        // 1. Extraire toutes les classes et leurs méthodes
        for (TypeInfo type : model) {
            ClassFact classFact = new ClassFact(type.qualifiedName());
            classes.add(classFact);

            for (MethodeInfo m : type.methods()) {
                String methodKey = type.qualifiedName() + "#" + m.name() + "(" + m.parameterCount() + ")";
                MethodFact methodFact = new MethodFact(methodKey, classFact);
                methods.add(methodFact);
                methodMap.put(methodKey, methodFact);
            }
        }

        // 2. Extraire les arcs d'appel internes uniques (caller -> target)
        for (TypeInfo type : model) {
            for (MethodeInfo m : type.methods()) {
                String callerKey = type.qualifiedName() + "#" + m.name() + "(" + m.parameterCount() + ")";
                MethodFact caller = methodMap.get(callerKey);

                if (caller == null) continue;

                for (AppelMethodInfo call : m.calls()) {
                    if (call.isInternal()) {
                        String targetKey = call.targetClassName() + "#" + call.targetMethodName() + "(" + call.targetParamCount() + ")";
                        MethodFact target = methodMap.get(targetKey);
                        if (target != null) {
                            callArcsSet.add(new CallArcFact(caller, target));
                        }
                    }
                }
            }
        }

        return new FactDataset(classes, methods, new ArrayList<>(callArcsSet));
    }
}