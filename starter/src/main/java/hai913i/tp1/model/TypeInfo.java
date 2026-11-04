package hai913i.tp1.model;

import java.util.List;

public record TypeInfo(
        String qualifiedName,
        String packageName,
        String genre, // "classe", "interface", "enum"
        List<String> superClasses,
        List<String> interfaces,
        List<AttributInfo> fields,
        List<MethodeInfo> methods
) {}