package hai913i.tp1.visitor;

import hai913i.tp1.model.AppelMethodInfo;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.List;

public class CallExtractorVisitor extends ASTVisitor {

    public int totalCalls = 0;
    public int methodInvocationCount = 0;
    public int superMethodInvocationCount = 0;
    public int internalCalls = 0;
    public int externalCalls = 0;
    public int unresolvedCalls = 0;

    public List<AppelMethodInfo> extractedCalls = new ArrayList<>();

    // Pour se souvenir dans quelle méthode/classe on se trouve lors du parcours
    private String currentMethodName = "";
    private int currentMethodParamCount = 0;
    private CompilationUnit currentUnit;

    public CallExtractorVisitor(CompilationUnit unit) {
        this.currentUnit = unit;
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        currentMethodName = node.getName().getIdentifier();
        currentMethodParamCount = node.parameters().size();
        return super.visit(node);
    }

    @Override
    public boolean visit(MethodInvocation node) {
        totalCalls++;
        methodInvocationCount++;

        // 1. Numéro de ligne de l'appel
        int lineNumber = currentUnit.getLineNumber(node.getStartPosition());

        // 2. Résolution du binding de la méthode appelée
        IMethodBinding methodBinding = node.resolveMethodBinding();

        if (methodBinding == null) {
            unresolvedCalls++;
        //    System.out.println("  [Ligne " + lineNumber + "] Appel non résolu : " + node.getName().getIdentifier());
            return super.visit(node);
        }

        // 3. Identification de la cible (méthode résolue)
        // getMethodDeclaration() remonte à la déclaration générique si applicable (ex: List<Item> -> List)
        IMethodBinding targetDecl = methodBinding.getMethodDeclaration();
        String targetClassName = targetDecl.getDeclaringClass().getQualifiedName();
        String targetMethodName = targetDecl.getName();
        int targetParamCount = targetDecl.getParameterTypes().length;

        // 4. Calcul du type statique du receveur (§ 4.4)
        String receiverType = "";
        Expression expression = node.getExpression();

        if (expression != null) {
            // Receveur explicite : type statique de l'expression (ex: loanable dans loanable.checkOut())
            ITypeBinding typeBinding = expression.resolveTypeBinding();
            if (typeBinding != null) {
                receiverType = typeBinding.getErasure().getQualifiedName(); // Effacement pour les génériques
            }
        } else {
            // Sans receveur explicite (ex: add(item))
            if (Modifier.isStatic(targetDecl.getModifiers())) {
                receiverType = targetDecl.getDeclaringClass().getQualifiedName();
            } else {
                // Méthode d'instance : classe englobante (this implicite)
                receiverType = targetDecl.getDeclaringClass().getQualifiedName();
            }
        }

        // 5. Classification : interne au projet vs externe (JDK / bibliothèques)
        // Une méthode est interne si sa classe appartient à l'un des paquetages du projet (ex: library.*)
        boolean isInternal = targetClassName.startsWith("library.");
        if (isInternal) {
            internalCalls++;
        } else {
            externalCalls++;
        }

        // Enregistrement dans le modèle de faits (B1)
        extractedCalls.add(new AppelMethodInfo(
                currentMethodName,
                targetClassName,
                targetMethodName,
                targetParamCount,
                receiverType,
                lineNumber,
                isInternal
        ));

        // System.out.println("  [Ligne " + lineNumber + "] Appel dans " + currentMethodName + "() -> Receveur: "
        //        + receiverType + " | Cible: " + targetClassName + "#" + targetMethodName);

        return super.visit(node);
    }

    @Override
    public boolean visit(SuperMethodInvocation node) {
        totalCalls++;
        superMethodInvocationCount++;

        int lineNumber = currentUnit.getLineNumber(node.getStartPosition());
        IMethodBinding methodBinding = node.resolveMethodBinding();

        if (methodBinding == null) {
            unresolvedCalls++;
            // System.out.println("  [Ligne " + lineNumber + "] Appel super non résolu : " + node.getName().getIdentifier());
            return super.visit(node);
        }

        IMethodBinding targetDecl = methodBinding.getMethodDeclaration();
        String targetClassName = targetDecl.getDeclaringClass().getQualifiedName();
        String targetMethodName = targetDecl.getName();
        int targetParamCount = targetDecl.getParameterTypes().length;

        // Le receveur d'un super.m() est la superclasse de la classe appelante
        String receiverType = targetClassName;

        boolean isInternal = targetClassName.startsWith("library.");
        if (isInternal) {
            internalCalls++;
        } else {
            externalCalls++;
        }

        // Enregistrement dans le modèle de faits (B1)
        extractedCalls.add(new AppelMethodInfo(
                currentMethodName,
                targetClassName,
                targetMethodName,
                targetParamCount,
                receiverType,
                lineNumber,
                isInternal
        ));

        // System.out.println("  [Ligne " + lineNumber + "] Appel super dans " + currentMethodName + "() -> Receveur: "
        //        + receiverType + " | Cible: " + targetClassName + "#" + targetMethodName);

        return super.visit(node);
    }
}