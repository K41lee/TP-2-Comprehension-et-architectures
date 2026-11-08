package hai913i.tp2.clustering;

import hai913i.tp2.metrics.CouplingMatrix;
import hai913i.tp2.model.ClassFact;
import java.util.*;

public class HierarchicalClustering {

    private final CouplingMatrix couplingMatrix;
    private final LinkageType linkageType;

    public HierarchicalClustering(CouplingMatrix couplingMatrix, LinkageType linkageType) {
        this.couplingMatrix = couplingMatrix;
        this.linkageType = linkageType;
    }

    /**
     * Constructeur par défaut : utilise le Lien Moyen (Average Linkage).
     */
    public HierarchicalClustering(CouplingMatrix couplingMatrix) {
        this(couplingMatrix, LinkageType.AVERAGE);
    }

    public LinkageType getLinkageType() {
        return linkageType;
    }

    /**
     * Calcule le couplage inter-clusters selon le type de linkage configuré.
     */
    public double computeClusterCoupling(Cluster k1, Cluster k2) {
        if (linkageType == LinkageType.SINGLE) {
            return computeSingleLinkage(k1, k2);
        } else {
            return computeAverageLinkage(k1, k2);
        }
    }

    /**
     * Lien Moyen (Average Linkage - Étape B1) :
     * Sum(C(c1, c2)) / (|K1| * |K2|)
     */
    private double computeAverageLinkage(Cluster k1, Cluster k2) {
        double sum = 0.0;
        for (ClassFact c1 : k1.getClasses()) {
            for (ClassFact c2 : k2.getClasses()) {
                sum += couplingMatrix.getCoupling(c1, c2);
            }
        }
        int size1 = k1.getClasses().size();
        int size2 = k2.getClasses().size();
        return sum / (size1 * size2);
    }

    /**
     * Lien Simple (Single Linkage - Bonus C1) :
     * Max(C(c1, c2)) sur l'ensemble des paires (c1, c2).
     */
    private double computeSingleLinkage(Cluster k1, Cluster k2) {
        double max = 0.0;
        for (ClassFact c1 : k1.getClasses()) {
            for (ClassFact c2 : k2.getClasses()) {
                double c = couplingMatrix.getCoupling(c1, c2);
                if (c > max) {
                    max = c;
                }
            }
        }
        return max;
    }

    /**
     * Calcule le couplage interne moyen C(G) au sein d'un module G (Étape B3) :
     * C(G) = Somme(C(ci, cj)) / ( |G| * (|G| - 1) / 2 )
     */
    public double computeInternalCoupling(Cluster g) {
        List<ClassFact> classes = new ArrayList<>(g.getClasses());
        int n = classes.size();
        if (n <= 1) return 0.0;

        double sum = 0.0;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                sum += couplingMatrix.getCoupling(classes.get(i), classes.get(j));
            }
        }
        int pairCount = n * (n - 1) / 2;
        return sum / pairCount;
    }

    /**
     * Exécute le Clustering Hiérarchique Ascendant (CHA) selon les règles imposées.
     */
    public Cluster buildDendrogram() {
        List<ClassFact> sortedClasses = new ArrayList<>(couplingMatrix.getDataset().getClasses());
        Collections.sort(sortedClasses); // Ordre alphabétique sur nom qualifié

        int m = sortedClasses.size();
        List<Cluster> activeClusters = new ArrayList<>();

        // 1. Initialisation des M feuilles avec identifiants 0 à M-1
        for (int i = 0; i < m; i++) {
            activeClusters.add(new Cluster(i, sortedClasses.get(i)));
        }

        int nextClusterId = m; // Prochain identifiant de groupe (M, M+1, ...)

        System.out.printf(Locale.US, "%n=== DÉBUT DU CLUSTERING HIÉRARCHIQUE (%s LINKAGE) ===%n", linkageType);
        System.out.println("Groupes initiaux (Feuilles) : " + activeClusters.size());

        // 2. Boucle de fusion jusqu'à obtenir 1 seul cluster racine
        while (activeClusters.size() > 1) {
            Cluster bestC1 = null;
            Cluster bestC2 = null;
            double maxCoupling = -1.0;

            int n = activeClusters.size();
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    Cluster c1 = activeClusters.get(i);
                    Cluster c2 = activeClusters.get(j);

                    // Garantie : min(id1, id2) < max(id1, id2)
                    Cluster minCluster = c1.getId() < c2.getId() ? c1 : c2;
                    Cluster maxCluster = c1.getId() < c2.getId() ? c2 : c1;

                    double coupling = computeClusterCoupling(minCluster, maxCluster);

                    // RÈGLE DE DÉPARTAGE
                    if (coupling > maxCoupling + 1e-9) {
                        maxCoupling = coupling;
                        bestC1 = minCluster;
                        bestC2 = maxCluster;
                    } else if (Math.abs(coupling - maxCoupling) <= 1e-9 && bestC1 != null) {
                        // Égalité de couplage : comparer les tuples (min_id, max_id) lexicographiquement
                        if (shouldReplace(minCluster, maxCluster, bestC1, bestC2)) {
                            bestC1 = minCluster;
                            bestC2 = maxCluster;
                        }
                    }
                }
            }

            // 3. Effectuer la fusion de la meilleure paire
            Cluster merged = new Cluster(nextClusterId++, bestC1, bestC2, maxCoupling);

            System.out.printf(Locale.US, "Fusion [%d] : %s (id %d) + %s (id %d) => id %d | Couplage = %.4f%n",
                    merged.getId() - m + 1,
                    bestC1.getLabel(), bestC1.getId(),
                    bestC2.getLabel(), bestC2.getId(),
                    merged.getId(), maxCoupling);

            activeClusters.remove(bestC1);
            activeClusters.remove(bestC2);
            activeClusters.add(merged);
        }

        return activeClusters.get(0); // Le cluster racine du dendrogramme
    }

    /**
     * Compare les tuples (c1.id, c2.id) avec (best1.id, best2.id)
     * Retient le tuple le plus petit lexicographiquement.
     */
    private boolean shouldReplace(Cluster c1, Cluster c2, Cluster best1, Cluster best2) {
        if (c1.getId() < best1.getId()) {
            return true;
        } else if (c1.getId() == best1.getId()) {
            return c2.getId() < best2.getId();
        }
        return false;
    }

    /**
     * Extrait une partition du dendrogramme selon un seuil de couplage interne CP (Étape B3).
     */
    public List<Cluster> extractPartitionByThreshold(Cluster node, double cp) {
        List<Cluster> partition = new ArrayList<>();
        collectPartition(node, cp, partition);
        return partition;
    }

    private void collectPartition(Cluster node, double cp, List<Cluster> partition) {
        double internalCoupling = computeInternalCoupling(node);
        if (node.isLeaf() || internalCoupling >= cp) {
            partition.add(node);
        } else {
            collectPartition(node.getLeft(), cp, partition);
            collectPartition(node.getRight(), cp, partition);
        }
    }

    /**
     * Évalue et affiche une partition selon le seuil CP (Étape B3).
     */
    public void evaluatePartition(Cluster root, double cp) {
        List<Cluster> partition = extractPartitionByThreshold(root, cp);
        int totalClasses = root.getClasses().size();
        int maxAllowedModules = totalClasses / 2; // Contrainte M/2 (ex: 15 / 2 = 7)
        boolean isViolated = partition.size() > maxAllowedModules;

        System.out.printf(Locale.US, "%n--- Étape B3 (%s) : Coupe pour CP = %.3f ---%n", linkageType, cp);
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
            double internalCoupling = computeInternalCoupling(mod);

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