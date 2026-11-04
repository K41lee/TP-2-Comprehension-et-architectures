package hai913i.tp1.metrics;

import hai913i.tp1.model.MethodeInfo;
import hai913i.tp1.model.TypeInfo;

import java.util.*;

public class MetricsCalculator {

    private List<TypeInfo> model;
    private int totalAppLinesOfCode;

    public MetricsCalculator(List<TypeInfo> model, int totalAppLinesOfCode) {
        this.model = model;
        this.totalAppLinesOfCode = totalAppLinesOfCode;
    }

    // 1. Nombre de classes (au sens large du sujet : tous les types = 15)
    public int getClassCount() {
        return model.size();
    }

    // 2. Lignes de code
    public int getTotalAppLinesOfCode() {
        return totalAppLinesOfCode;
    }

    // 3. Nombre total de méthodes
    public int getTotalMethods() {
        int total = 0;
        for (TypeInfo type : model) {
            total += type.methods().size();
        }
        return total;
    }

    // 4. Nombre total de paquetages
    public int getTotalPackages() {
        Set<String> packages = new HashSet<>();
        for (TypeInfo type : model) {
            packages.add(type.packageName());
        }
        return packages.size();
    }

    // 5. Moyenne méthodes / classe (divisé par 15)
    public double getAverageMethodsPerClass() {
        int classes = getClassCount();
        if (classes == 0) {
            return 0.0;
        } else {
            return (double) getTotalMethods() / classes;
        }
    }

    // 6. Moyenne LOC par méthode (ayant un corps)
    public double getAverageLinesOfCodePerMethodWithBody() {
        int totalLoc = 0;
        int countWithBody = 0;
        for (TypeInfo type : model) {
            for (MethodeInfo method : type.methods()) {
                if (method.hasBody()) {
                    totalLoc += method.linesOfCode();
                    countWithBody++;
                }
            }
        }
        if (countWithBody == 0) {
            return 0.0;
        } else {
            return (double) totalLoc / countWithBody;
        }
    }

    // 7. Moyenne attributs / classe (divisé par 15)
    public double getAverageFieldsPerClass() {
        int classes = getClassCount();
        int totalFields = 0;
        for (TypeInfo type : model) {
            totalFields += type.fields().size();
        }
        if (classes == 0) {
            return 0.0;
        } else {
            return (double) totalFields / classes;
        }
    }

    // 8. Top 10% des classes selon le nombre de méthodes (avec gestion des ex-æquo)
    public List<TypeInfo> getTop10PercentClassesByMethods() {
        // 1. Filtrer les candidats à 0
        List<TypeInfo> sorted = new ArrayList<>(model);
        for (TypeInfo type : model) {
            if (type.fields().size() > 0) {
                sorted.add(type);
            }
        }
        sorted.sort(new Comparator<TypeInfo>() {
            @Override
            public int compare(TypeInfo t1, TypeInfo t2) {
                return Integer.compare(t2.methods().size(), t1.methods().size());
            }
        });

        int k = (int) Math.ceil(sorted.size() * 0.10); // k = 2
        if (k < 1 && !sorted.isEmpty()) {
            k = 1;
        }

        List<TypeInfo> result = new ArrayList<>();
        if (sorted.isEmpty()) return result;

        int cutoffValue = 0;
        for (int i = 0; i < sorted.size(); i++) {
            if (i < k) {
                result.add(sorted.get(i));
                cutoffValue = sorted.get(i).methods().size();
            } else {
                // Si égalité au rang du seuil, on l'ajoute aussi
                if (sorted.get(i).methods().size() == cutoffValue) {
                    result.add(sorted.get(i));
                } else {
                    break;
                }
            }
        }
        return result;
    }

    // 9. Top 10% des classes selon le nombre d'attributs (avec gestion des ex-æquo)
    public List<TypeInfo> getTop10PercentClassesByFields() {
        // 1. Filtrer les candidats à 0
        List<TypeInfo> sorted = new ArrayList<>(model);
        for (TypeInfo type : model) {
            if (type.fields().size() > 0) {
                sorted.add(type);
            }
        }        sorted.sort(new Comparator<TypeInfo>() {
            @Override
            public int compare(TypeInfo t1, TypeInfo t2) {
                return Integer.compare(t2.fields().size(), t1.fields().size());
            }
        });

        int k = (int) Math.ceil(sorted.size() * 0.10); // k = 2
        if (k < 1 && !sorted.isEmpty()) {
            k = 1;
        }

        List<TypeInfo> result = new ArrayList<>();
        if (sorted.isEmpty()) return result;

        int cutoffValue = 0;
        for (int i = 0; i < sorted.size(); i++) {
            if (i < k) {
                result.add(sorted.get(i));
                cutoffValue = sorted.get(i).fields().size();
            } else {
                if (sorted.get(i).fields().size() == cutoffValue) {
                    result.add(sorted.get(i));
                } else {
                    break;
                }
            }
        }
        return result;
    }

    // 10. Intersection des deux tops 10%
    public List<TypeInfo> getClassesInBothTop10Percent() {
        List<TypeInfo> topM = getTop10PercentClassesByMethods();
        List<TypeInfo> topF = getTop10PercentClassesByFields();

        List<TypeInfo> intersection = new ArrayList<>();
        for (TypeInfo tm : topM) {
            for (TypeInfo tf : topF) {
                if (tm.qualifiedName().equals(tf.qualifiedName())) {
                    intersection.add(tm);
                }
            }
        }
        return intersection;
    }

    // 11. Classes ayant strictement plus de X méthodes
    public List<TypeInfo> getClassesWithMoreThanXMethods(int x) {
        List<TypeInfo> result = new ArrayList<>();
        for (TypeInfo type : model) {
            if (type.methods().size() > x) {
                result.add(type);
            }
        }
        return result;
    }

    // 12. Top 10% des méthodes par classe (ignore les méthodes sans corps à 0 LOC)
    public Map<String, List<MethodeInfo>> getTop10PercentMethodsByLocPerClass() {
        Map<String, List<MethodeInfo>> result = new HashMap<>();

        for (TypeInfo type : model) {
            List<MethodeInfo> methodsWithBody = new ArrayList<>();
            for (MethodeInfo m : type.methods()) {
                if (m.hasBody() && m.linesOfCode() > 0) {
                    methodsWithBody.add(m);
                }
            }

            methodsWithBody.sort(new Comparator<MethodeInfo>() {
                @Override
                public int compare(MethodeInfo m1, MethodeInfo m2) {
                    return Integer.compare(m2.linesOfCode(), m1.linesOfCode());
                }
            });

            int k = (int) Math.ceil(methodsWithBody.size() * 0.10);
            if (k < 1 && !methodsWithBody.isEmpty()) {
                k = 1;
            }

            List<MethodeInfo> topMethods = new ArrayList<>();
            if (!methodsWithBody.isEmpty()) {
                int cutoffValue = 0;
                for (int i = 0; i < methodsWithBody.size(); i++) {
                    if (i < k) {
                        topMethods.add(methodsWithBody.get(i));
                        cutoffValue = methodsWithBody.get(i).linesOfCode();
                    } else {
                        if (methodsWithBody.get(i).linesOfCode() == cutoffValue) {
                            topMethods.add(methodsWithBody.get(i));
                        } else {
                            break;
                        }
                    }
                }
            }
            result.put(type.qualifiedName(), topMethods);
        }
        return result;
    }

    // 13. Max paramètres
    public int getMaxParametersCount() {
        int max = 0;
        for (TypeInfo type : model) {
            for (MethodeInfo method : type.methods()) {
                if (method.parameterCount() > max) {
                    max = method.parameterCount();
                }
            }
        }
        return max;
    }

    public List<String> getMethodsWithMaxParameters() {
        int max = getMaxParametersCount();
        List<String> result = new ArrayList<>();

        for (TypeInfo type : model) {
            for (MethodeInfo method : type.methods()) {
                if (method.parameterCount() == max) {
                    result.add(type.qualifiedName() + "#" + method.name() + "(" + method.parameterCount() + " params)");
                }
            }
        }
        return result;
    }
}