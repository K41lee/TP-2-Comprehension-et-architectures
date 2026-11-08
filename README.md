# HAI913I — Évolution et restructuration des logiciels
## TP 2 : Compréhension et architectures — Notice Technique (README)

**Binôme :** Anthony Hommais (22010461, GL) & Corentin Fay (22013398, GL)  
**Université de Montpellier — Année Universitaire 2026–2027**  
**Dépôt Git :** [https://github.com/K41lee/TP-2-Comprehension-et-architectures](https://github.com/K41lee/TP-2-Comprehension-et-architectures)

---

## 1. Projet

### Architecture et structure des modules
Le projet est structuré en sous-packages modulaires sous `src/main/java/hai913i/tp2/` respectant une séparation stricte des responsabilités :
* `model` : Classes de domaine représentant le système sous forme de faits (`ClassFact`, `MethodFact`, `CallArcFact`, `ClassPair`).
* `input` : Couche d'abstraction d'entrée via l'interface `FactLoader` (`Tp1ModelFactLoader`, `CanonicalCsvFactLoader`, `JsonFactLoader`).
* `metrics` : Moteur de calcul de couplage inter-classes et d'export de matrice (`CouplingMatrix`).
* `clustering` : Moteur de Clustering Hiérarchique Ascendant et d'extraction des partitions (`HierarchicalClustering`, `Cluster`, `LinkageType`).
* `display` : Affichage épuré des journaux et des points de contrôle sur la console (`ConsoleDisplay`).
* `gui` : Interface graphique interactive Swing d'exploration et d'inspection (`InteractiveClusteringViewer`).

### Chemin d'entrée retenu (§ 4)
Le projet implémente les deux chemins du sujet :
1. **Chemin 1 (Recommandé / Option A) :** Intégration directe du parseur AST/JDT du TP1 via `Tp1ModelFactLoader` lorsqu'un dossier de code source Java est fourni.
2. **Chemin 2 (Repli) :** Chargement des faits canoniques CSV (`CanonicalCsvFactLoader`) ou JSON (`JsonFactLoader`) lorsque le chemin pointe vers un dossier de faits ou un fichier JSON.

### Entrées attendues & Sorties produites
* **Entrées :** Répertoire de code Java (`src/`), dossier de fichiers CSV canoniques (`classes.csv`, `methods.csv`, `calls.csv`) ou fichier JSON (`facts-mini.json`).
* **Sorties :**
  * `coupling-matrix.csv` : Matrice de couplage $M \times M$ ré-importable.
  * `coupling-graph.dot` : Graphe de couplage non orienté au format Graphviz DOT.
  * `dendrogram.nwk` / `dendrogram-single.nwk` : Arbre de clustering au format Newick.
  * `dendrogram.dot` / `dendrogram-single.dot` : Dendrogramme au format Graphviz.
  * Trace console des points de contrôle A0, A1, A2, B1, B2, B3 et fenêtre GUI Swing interactive.

### Dépendances et limites connues
* **Dépendances :** Aucune dépendance d'exécution tierce requise pour la ligne de commande (utilisation exclusive du JDK standard). Eclipse JDT est inclus via le module du TP1 pour le chemin 1.
* **Limites connues :** La métrique de couplage par défaut (hors C2) ne prend en compte que les appels de méthodes directs et ignore l'héritage/sous-typage.

---

## 2. Environnement

| Composant | Version / Spécification exacte |
| :--- | :--- |
| **Java Development Kit (JDK)** | OpenJDK 17.0.10 (ou Java 21) |
| **Outil de construction** | Apache Maven 3.9.6 |
| **Graphviz** | Version 2.43.0 (pour la conversion de `.dot` en `.png` / `.svg`) |
| **Système d'exploitation** | Linux Ubuntu 22.04 LTS / POSIX compliant |
| **Environnement de dev / IDE** | IntelliJ IDEA 2023.3 / CLI Bash |

---

## 3. Construction

Depuis une copie fraîche du dépôt Git (sans répertoires générés `target/`), exécuter les commandes suivantes :

```bash
# 1. Se placer à la racine du projet Maven (dossier starter)
cd starter

# 2. Nettoyer et compiler le projet complet
mvn clean package
```

Un fichier JAR exécutable autonome est généré dans `target/hai913i-tp1-analyzer.jar`.

---

## 4. Exécution

### Syntaxes générales
```bash
java -cp target/hai913i-tp1-analyzer.jar hai913i.tp2.MainTp2 <CHEMIN_ENTREE> [single] [c2] [gui]
```

### Exemples complets d'utilisation

#### 1. Exécution standard sur le projet de validation (Lien moyen, B0--B3) :
```bash
java -cp target/hai913i-tp1-analyzer.jar hai913i.tp2.MainTp2 ../validation
```

#### 2. Exécution du Bonus C1 (Lien Simple / Single Linkage) :
```bash
java -cp target/hai913i-tp1-analyzer.jar hai913i.tp2.MainTp2 ../validation single
```

#### 3. Exécution du Bonus C2 (Enrichissement par l'Héritage) :
```bash
java -cp target/hai913i-tp1-analyzer.jar hai913i.tp2.MainTp2 ../validation c2
```

#### 4. Lancement de l'interface graphique interactive Swing (Bonus C3) :
```bash
java -cp target/hai913i-tp1-analyzer.jar hai913i.tp2.MainTp2 ../validation gui
```

#### 5. Compilation des fichiers DOT Graphviz en images PNG :
```bash
dot -Tpng coupling-graph.dot -o coupling-graph.png
dot -Tpng dendrogram.dot -o dendrogram.png
dot -Tpng dendrogram-single.dot -o dendrogram-single.png
```

---

## 5. Validation

Le programme a été validé sur deux jeux de données de référence :
1. **Jeu canonique de validation ($M=15$ classes, 59 méthodes, 44 arcs) :**
   * Point A1 : $|E_{\text{inter}}| = 37$, 19 paires couplées, $C_{\max} = 0{,}1081$ (`Item`, `LoanService`).
   * Point B1 : 14 fusions, fusions [1] à $0{,}1081$, [2] à $0{,}0811$, [3] à $0{,}0608$.
   * Point B3 : $CP=0{,}015 \Rightarrow 4$ modules ; $CP=0{,}025 \Rightarrow 7$ modules (optimal) ; $CP=0{,}045 \Rightarrow 10$ modules (rejeté, $10 > 7$).
2. **Micro jeu de données (`examples/facts-mini.json`) :**
   * Point A0 : 3 classes, 5 méthodes, 4 arcs.
   * Point A1 : $|E_{\text{inter}}| = 3$, $C(A, B) = 0{,}6667$, $C(B, C) = 0{,}3333$.

---

## 6. Dépannage

| Problème constaté | Cause fréquente | Remède / Solution |
| :--- | :--- | :--- |
| `Erreur : Le chemin spécifié n'existe pas` | Argument de ligne de commande incorrect ou relatif au mauvais dossier. | Lancer la commande depuis le dossier `starter` et vérifier la présence de `../validation`. |
| `ClassFormatException` / Erreur de lecture CSV | Encodage de fichier ou séparateur non conforme. | S'assurer d'utiliser les fichiers CSV UTF-8 fournis sans altération de BOM. |
| Différence de similarité de fusion au dernier bit | Comparaison stricte de flottants (`==`) sans tolérance. | L'application utilise une tolérance $\epsilon = 10^{-9}$ lors de la comparaison des scores. |
| Commandes Graphviz `dot` introuvables | Graphviz n'est pas installé ou non présent dans le `PATH`. | Installer Graphviz via `sudo apt install graphviz` sous Linux. |
