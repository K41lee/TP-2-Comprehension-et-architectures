package hai913i.tp1.visitor;

import org.eclipse.jdt.core.dom.*;

// Visiteur pour afficher la structure de l'AST avec indentation.
// Exercice A1
public class ASTStructurePrinterVisitor extends ASTVisitor {
    private int indentLevel = 0;

    private void printIndent() {
        for (int i = 0; i < indentLevel; i++) {
            System.out.print("  ");
        }
    }

    @Override
    public boolean preVisit2(ASTNode node) {
        printIndent();

        String nodeType = node.getClass().getSimpleName();
        String label = getNodeLabel(node);

        if (label != null && !label.isEmpty()) {
            System.out.println(nodeType + " (" + label + ")");
        } else {
            System.out.println(nodeType);
        }

        indentLevel++;
        return true;
    }

    @Override
    public void postVisit(ASTNode node) {
        indentLevel--;
    }

    /**
     * Méthode utilitaire utilisé dans preVisit2,
     * Récupère un label descriptif du noeud AST.
     */
    private String getNodeLabel(ASTNode node) {
        if (node instanceof TypeDeclaration td) {
            return td.getName().getIdentifier();
        }
        else if (node instanceof EnumDeclaration ed) {
            return ed.getName().getIdentifier();
        }
        else if (node instanceof RecordDeclaration rd) {
            return rd.getName().getIdentifier();
        }
        else if (node instanceof MethodDeclaration md) {
            return md.getName().getIdentifier();
        }
        else if (node instanceof VariableDeclarationFragment vdf) {
            return vdf.getName().getIdentifier();
        }
        else if (node instanceof SingleVariableDeclaration svd) {
            return svd.getName().getIdentifier();
        }
        else if (node instanceof SimpleName sn) {
            return sn.getIdentifier();
        }
        else if (node instanceof PrimitiveType pt) {
            return pt.getPrimitiveTypeCode().toString();
        }
        else if (node instanceof Modifier m) {
            return m.getKeyword().toString();
        }
        else if (node instanceof PackageDeclaration pd) {
            return pd.getName().getFullyQualifiedName();
        }
        else if (node instanceof MethodInvocation mi) {
            return mi.getName().getIdentifier() + "()";
        }

        return null;
    }
}