package hai913i.tp1.visitor;

import hai913i.tp1.model.AttributInfo;
import hai913i.tp1.model.MethodeInfo;
import hai913i.tp1.model.TypeInfo;
import org.eclipse.jdt.core.dom.*;
import java.util.*;

// Visiteur pour extraire la structure des types (classes, interfaces, enums) et de leurs membres.
// Exercice A2
public class StructureExtractorVisitor extends ASTVisitor {

    // Compteurs globaux pour valider le point de contrôle A2
    public int typeCount = 0;
    public int classCount = 0;
    public int interfaceCount = 0;
    public int enumCount = 0;
    public int methodCount = 0;
    public int constructorCount = 0;
    public int fieldCount = 0;

    public List<TypeInfo> extractedTypes = new ArrayList<>();
    private CompilationUnit currentUnit;

    public StructureExtractorVisitor() {
    }

    public void setCurrentUnit(CompilationUnit unit) {
        this.currentUnit = unit;
    }

    // Constructeur pour recevoir l'unité de compilation (nécessaire pour les numéros de ligne des appels)
    public StructureExtractorVisitor(CompilationUnit unit) {
        this.currentUnit = unit;
    }

    // Helper pour récupérer la visibilité d'un noeud
    private String getVisibility(BodyDeclaration node) {
        int modifiers = node.getModifiers();
        if (Modifier.isPublic(modifiers)) {
            return "public";
        }
        if (Modifier.isProtected(modifiers)) {
            return "protected";
        }
        if (Modifier.isPrivate(modifiers)) {
            return "private";
        }
        return "package";
    }

    // Traitement d'une classe ou d'une interface
    @Override
    public boolean visit(TypeDeclaration node) {
        ITypeBinding binding = node.resolveBinding();
        if (binding == null) {
            return super.visit(node);
        }

        typeCount++;
        if (node.isInterface()) {
            interfaceCount++;
        } else {
            classCount++;
        }

        String qualifiedName = binding.getQualifiedName();

        // Détermination du paquetage
        String packageName = "(défaut)";
        if (binding.getPackage() != null) {
            packageName = binding.getPackage().getName();
        }

        // Détermination du genre
        String genre = "classe";
        if (node.isInterface()) {
            genre = "interface";
        }

        // System.out.println("Type trouvé : " + qualifiedName + " [" + genre + "] (Paquetage: " + packageName + ")");

        // Interfaces directes
        List<String> interfaces = new ArrayList<>();
        for (ITypeBinding superInterface : binding.getInterfaces()) {
            interfaces.add(superInterface.getQualifiedName());
        }
        if (!interfaces.isEmpty()) {
            // System.out.println("  Interfaces directes : " + String.join(", ", interfaces));
        }

        // Chaîne des superclasses
        ITypeBinding currentSuper = binding.getSuperclass();
        List<String> superClasses = new ArrayList<>();
        while (currentSuper != null && !currentSuper.getQualifiedName().equals("java.lang.Object")) {
            superClasses.add(currentSuper.getQualifiedName());
            currentSuper = currentSuper.getSuperclass();
        }

        if (!superClasses.isEmpty()) {
            // System.out.println("  Superclasses : " + String.join(" -> ", superClasses));
        }

        // Listes pour alimenter le modèle de faits (B1)
        List<AttributInfo> fields = new ArrayList<>();
        List<MethodeInfo> methods = new ArrayList<>();

        // Inspection des membres directs (sans récursivité automatique)
        for (Object decl : node.bodyDeclarations()) {
            if (decl instanceof FieldDeclaration fieldDecl) {
                String visibility = getVisibility(fieldDecl);
                String typeName = fieldDecl.getType().toString();
                for (Object frag : fieldDecl.fragments()) {
                    if (frag instanceof VariableDeclarationFragment fragment) {
                        fieldCount++;
                        String fieldName = fragment.getName().getIdentifier();
                        // System.out.println("  Attribut : " + fieldName + " : " + typeName + " (" + visibility + ")");

                        // Ajout au modèle de faits
                        fields.add(new AttributInfo(fieldName, typeName, visibility));
                    }
                }
            } else if (decl instanceof MethodDeclaration methodDecl) {
                methodCount++;
                if (methodDecl.isConstructor()) {
                    constructorCount++;
                }
                String methodName = methodDecl.getName().getIdentifier();
                int paramCount = methodDecl.parameters().size();
                boolean isConstructor = methodDecl.isConstructor();
                // System.out.println("  Méthode : " + methodName + " (Params: " + paramCount + ", Constructeur: " + isConstructor + ")");

                // Extraction des appels internes à cette méthode pour le modèle
                CallExtractorVisitor callVisitor = new CallExtractorVisitor(currentUnit);
                methodDecl.accept(callVisitor);

                // Calcul des lignes de code sur le CORPS de la méthode (getBody())
                boolean hasBody = methodDecl.getBody() != null;
                int methodLoc = 0;
                if (hasBody && currentUnit != null) {
                    Block body = methodDecl.getBody();
                    int startLine = currentUnit.getLineNumber(body.getStartPosition());
                    int endLine = currentUnit.getLineNumber(body.getStartPosition() + body.getLength());
                    methodLoc = (endLine - startLine) + 1;
                }

                methods.add(new MethodeInfo(methodName, paramCount, isConstructor, hasBody, methodLoc, callVisitor.extractedCalls));
            }
        }

        // Instanciation et stockage de l'objet TypeInfo dans le modèle
        TypeInfo typeInfo = new TypeInfo(qualifiedName, packageName, genre, superClasses, interfaces, fields, methods);
        extractedTypes.add(typeInfo);

        return super.visit(node);
    }

    // Traitement d'une énumération
    @Override
    public boolean visit(EnumDeclaration node) {
        ITypeBinding binding = node.resolveBinding();
        if (binding != null) {
            typeCount++;
            enumCount++;

            String qualifiedName = binding.getQualifiedName();
            String packageName = (binding.getPackage() != null) ? binding.getPackage().getName() : "(défaut)";
            // System.out.println("Type trouvé : " + qualifiedName + " [enum]");

            List<AttributInfo> fields = new ArrayList<>();
            List<MethodeInfo> methods = new ArrayList<>();

            for (Object decl : node.bodyDeclarations()) {
                if (decl instanceof FieldDeclaration fieldDecl) {
                    String visibility = getVisibility(fieldDecl);
                    String typeName = fieldDecl.getType().toString();
                    for (Object frag : fieldDecl.fragments()) {
                        if (frag instanceof VariableDeclarationFragment fragment) {
                            fieldCount++;
                            String fieldName = fragment.getName().getIdentifier();
                            // System.out.println("  Attribut : " + fieldName + " : " + typeName + " (" + visibility + ")");

                            // Ajout au modèle
                            fields.add(new AttributInfo(fieldName, typeName, visibility));
                        }
                    }
                } else if (decl instanceof MethodDeclaration methodDecl) {
                    methodCount++;
                    if (methodDecl.isConstructor()) {
                        constructorCount++;
                    }
                    String methodName = methodDecl.getName().getIdentifier();
                    int paramCount = methodDecl.parameters().size();
                    boolean isConstructor = methodDecl.isConstructor();
                    // System.out.println("  Méthode : " + methodName + " (Params: " + paramCount + ", Constructeur: " + isConstructor + ")");

                    // Extraction des appels dans les méthodes de l'enum
                    CallExtractorVisitor callVisitor = new CallExtractorVisitor(currentUnit);
                    methodDecl.accept(callVisitor);

                    // Calcul des lignes de code sur le CORPS de la méthode (getBody())
                    boolean hasBody = methodDecl.getBody() != null;
                    int methodLoc = 0;
                    if (hasBody && currentUnit != null) {
                        Block body = methodDecl.getBody();
                        int startLine = currentUnit.getLineNumber(body.getStartPosition());
                        int endLine = currentUnit.getLineNumber(body.getStartPosition() + body.getLength());
                        methodLoc = (endLine - startLine) + 1;
                    }

                    methods.add(new MethodeInfo(methodName, paramCount, isConstructor, hasBody, methodLoc, callVisitor.extractedCalls));
                }
            }

            // Instanciation et ajout de l'enum au modèle de faits
            TypeInfo typeInfo = new TypeInfo(qualifiedName, packageName, "enum", Collections.emptyList(), Collections.emptyList(), fields, methods);
            extractedTypes.add(typeInfo);
        }
        return super.visit(node);
    }
}