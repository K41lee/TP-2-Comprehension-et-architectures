package hai913i.tp2.gui;

import hai913i.tp2.clustering.Cluster;
import hai913i.tp2.clustering.HierarchicalClustering;
import hai913i.tp2.metrics.CouplingMatrix;
import hai913i.tp2.model.ClassFact;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

public class InteractiveClusteringViewer extends JFrame {

    private final HierarchicalClustering clustering;
    private final Cluster rootCluster;

    // Composants d'interaction (C3)
    private final JSlider cpSlider;
    private final JLabel cpValueLabel;
    private final JLabel statusLabel;

    private final JTextField searchField;
    private final JSpinner minSizeSpinner;
    private final JSpinner minCouplingSpinner;

    private final JTree dendrogramTree;
    private final DefaultTreeModel treeModel;
    private final JList<String> modulesList;
    private final DefaultListModel<String> modulesListModel;
    private final JTextArea inspectorTextArea;

    private List<Cluster> currentPartition = new ArrayList<>();

    public InteractiveClusteringViewer(CouplingMatrix matrix, Cluster root) {
        super("TP2 - Explorer Graphique & Interactif (Bonus C3)");
        this.clustering = new HierarchicalClustering(matrix);
        this.rootCluster = root;

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // --- 1. BARRE DE CONTRÔLE SUPÉRIEURE (CP, Recherche, Filtres) ---
        JPanel topContainer = new JPanel(new GridLayout(2, 1, 5, 5));
        topContainer.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Ligne 1 : Seuil CP et Statut M/2
        JPanel cpPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        cpPanel.add(new JLabel("Seuil CP :"));

        cpSlider = new JSlider(0, 100, 25);
        cpSlider.setPreferredSize(new Dimension(200, 25));
        cpValueLabel = new JLabel("0.025");
        cpValueLabel.setFont(new Font("Monospaced", Font.BOLD, 14));

        statusLabel = new JLabel("Statut M/2 : INITIALISÉ");
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 12));

        cpPanel.add(cpSlider);
        cpPanel.add(cpValueLabel);
        cpPanel.add(Box.createHorizontalStrut(20));
        cpPanel.add(statusLabel);

        // Ligne 2 : Recherche et Filtres (Taille & Couplage)
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));

        filterPanel.add(new JLabel("Rechercher une classe :"));
        searchField = new JTextField(15);
        filterPanel.add(searchField);

        filterPanel.add(Box.createHorizontalStrut(15));
        filterPanel.add(new JLabel("Taille min module :"));
        minSizeSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 15, 1));
        filterPanel.add(minSizeSpinner);

        filterPanel.add(Box.createHorizontalStrut(15));
        filterPanel.add(new JLabel("Couplage min :"));
        minCouplingSpinner = new JSpinner(new SpinnerNumberModel(0.00, 0.00, 0.10, 0.005));
        filterPanel.add(minCouplingSpinner);

        topContainer.add(cpPanel);
        topContainer.add(filterPanel);
        add(topContainer, BorderLayout.NORTH);

        // --- 2. PANNEAU CENTRAL EN VUES MULTIPLES (Onglets / Splitted) ---
        JTabbedPane tabbedPane = new JTabbedPane();

        // VUE 1 : Partition des Modules & Inspecteur
        JPanel modulesViewPanel = new JPanel(new BorderLayout(5, 5));
        modulesListModel = new DefaultListModel<>();
        modulesList = new JList<>(modulesListModel);
        modulesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane modulesScroll = new JScrollPane(modulesList);
        modulesScroll.setBorder(BorderFactory.createTitledBorder("Modules Candidats"));

        inspectorTextArea = new JTextArea();
        inspectorTextArea.setEditable(false);
        inspectorTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane inspectorScroll = new JScrollPane(inspectorTextArea);
        inspectorScroll.setBorder(BorderFactory.createTitledBorder("Inspecteur de Nœud / Module"));

        JSplitPane splitModules = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, modulesScroll, inspectorScroll);
        splitModules.setDividerLocation(350);
        modulesViewPanel.add(splitModules, BorderLayout.CENTER);

        // VUE 2 : Navigation dans le Dendrogramme (JTree)
        DefaultMutableTreeNode treeRootNode = buildTreeModel(rootCluster);
        treeModel = new DefaultTreeModel(treeRootNode);
        dendrogramTree = new JTree(treeModel);
        JScrollPane treeScroll = new JScrollPane(dendrogramTree);
        treeScroll.setBorder(BorderFactory.createTitledBorder("Arbre de Clustering (Dendrogramme)"));

        tabbedPane.addTab("Vue Partition & Inspecteur", modulesViewPanel);
        tabbedPane.addTab("Vue Arbre complet (Dendrogramme)", treeScroll);

        add(tabbedPane, BorderLayout.CENTER);

        // --- 3. ÉVÉNEMENTS & ÉCOUTEURS ---

        // Changement de seuil CP
        cpSlider.addChangeListener(e -> {
            double cp = cpSlider.getValue() / 1000.0;
            cpValueLabel.setText(String.format(Locale.US, "%.3f", cp));
            refreshView();
        });

        // Recherche dynamique de classe
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applySearch(); }
            public void removeUpdate(DocumentEvent e) { applySearch(); }
            public void changedUpdate(DocumentEvent e) { applySearch(); }
        });

        // Changement des filtres
        minSizeSpinner.addChangeListener(e -> refreshView());
        minCouplingSpinner.addChangeListener(e -> refreshView());

        // Sélection d'un module dans la liste -> Inspection détaillée
        modulesList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                inspectSelectedModule();
            }
        });

        // Sélection dans l'arbre JTree -> Inspection du nœud
        dendrogramTree.addTreeSelectionListener(e -> inspectSelectedTreeNode());

        // Initialisation de la vue
        refreshView();
    }

    /**
     * Construit récursivement le modèle de l'arbre JTree.
     */
    private DefaultMutableTreeNode buildTreeModel(Cluster node) {
        String label = node.isLeaf() ?
                node.getLabel() :
                String.format(Locale.US, "Fusion ID %d (Score: %.4f)", node.getId(), node.getMergeScore());

        DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(label);
        treeNode.setUserObject(node);

        if (!node.isLeaf()) {
            treeNode.add(buildTreeModel(node.getLeft()));
            treeNode.add(buildTreeModel(node.getRight()));
        }
        return treeNode;
    }

    /**
     * Rafraîchit l'affichage complet selon le CP et les filtres.
     */
    private void refreshView() {
        double cp = cpSlider.getValue() / 1000.0;
        int minSize = (Integer) minSizeSpinner.getValue();
        double minCoupling = (Double) minCouplingSpinner.getValue();

        currentPartition = clustering.extractPartitionByThreshold(rootCluster, cp);

        // Mise à jour du statut M/2
        int totalClasses = rootCluster.getClasses().size();
        int maxAllowed = totalClasses / 2;
        boolean isViolated = currentPartition.size() > maxAllowed;

        if (isViolated) {
            statusLabel.setText(String.format("Statut : %d modules (M/2 = %d) -> VIOLÉE (Coupe rejetée)",
                    currentPartition.size(), maxAllowed));
            statusLabel.setForeground(Color.RED);
        } else {
            statusLabel.setText(String.format("Statut : %d modules (M/2 = %d) -> RESPECTÉE",
                    currentPartition.size(), maxAllowed));
            statusLabel.setForeground(new Color(0, 128, 0));
        }

        // Filtrage et peuplement de la JList
        modulesListModel.clear();
        for (int i = 0; i < currentPartition.size(); i++) {
            Cluster mod = currentPartition.get(i);
            double internalC = clustering.computeInternalCoupling(mod);

            // Filtrage sur la taille et le couplage
            if (mod.getClasses().size() >= minSize && internalC >= minCoupling) {
                String entry = String.format(Locale.US, "Module %d [ID: %d | %d classe(s) | C_int = %.4f]",
                        i + 1, mod.getId(), mod.getClasses().size(), internalC);
                modulesListModel.addElement(entry);
            }
        }

        applySearch();
    }

    /**
     * Recherche une classe et sélectionne le module correspondant.
     */
    private void applySearch() {
        String query = searchField.getText().trim().toLowerCase();
        if (query.isEmpty()) return;

        for (int i = 0; i < currentPartition.size(); i++) {
            Cluster mod = currentPartition.get(i);
            for (ClassFact c : mod.getClasses()) {
                if (c.qualifiedName().toLowerCase().contains(query)) {
                    // Sélectionner dans la liste si présent
                    for (int j = 0; j < modulesListModel.getSize(); j++) {
                        if (modulesListModel.getElementAt(j).contains("ID: " + mod.getId() + " ")) {
                            modulesList.setSelectedIndex(j);
                            modulesList.ensureIndexIsVisible(j);
                            break;
                        }
                    }
                    return;
                }
            }
        }
    }

    /**
     * Affiche les détails complets du module sélectionné dans l'inspecteur.
     */
    private void inspectSelectedModule() {
        int idx = modulesList.getSelectedIndex();
        if (idx < 0) return;

        String selectedItem = modulesListModel.getElementAt(idx);
        // Extraire l'ID du module
        int idStart = selectedItem.indexOf("ID: ") + 4;
        int idEnd = selectedItem.indexOf(" |", idStart);
        int modId = Integer.parseInt(selectedItem.substring(idStart, idEnd));

        Cluster selectedCluster = null;
        for (Cluster c : currentPartition) {
            if (c.getId() == modId) {
                selectedCluster = c;
                break;
            }
        }

        if (selectedCluster != null) {
            displayClusterDetails("MODULE SÉLECTIONNÉ", selectedCluster);
        }
    }

    /**
     * Affiche les détails du nœud de l'arbre JTree dans l'inspecteur.
     */
    private void inspectSelectedTreeNode() {
        TreePath path = dendrogramTree.getSelectionPath();
        if (path == null) return;

        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object obj = treeNode.getUserObject();
        if (obj instanceof Cluster node) {
            displayClusterDetails("NŒUD DU DENDROGRAMME", node);
        }
    }

    private void displayClusterDetails(String header, Cluster cluster) {
        double internalC = clustering.computeInternalCoupling(cluster);
        StringBuilder sb = new StringBuilder();
        sb.append("=== ").append(header).append(" ===\n");
        sb.append(String.format(Locale.US, "ID Nœud               : %d%n", cluster.getId()));
        sb.append(String.format(Locale.US, "Nombre de classes      : %d%n", cluster.getClasses().size()));
        sb.append(String.format(Locale.US, "Couplage interne C(G)  : %.4f%n", internalC));
        sb.append(String.format(Locale.US, "Score de fusion       : %.4f%n", cluster.getMergeScore()));
        sb.append(String.format("Est une feuille        : %s%n%n", cluster.isLeaf() ? "Oui" : "Non"));

        sb.append("Classes constitutives :\n");
        for (ClassFact c : cluster.getClasses()) {
            sb.append(" - ").append(c.qualifiedName()).append("\n");
        }

        inspectorTextArea.setText(sb.toString());
        inspectorTextArea.setCaretPosition(0);
    }
}