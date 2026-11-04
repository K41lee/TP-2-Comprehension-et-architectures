package hai913i.tp2.model;

import java.util.Objects;

public record CallArcFact(MethodFact caller, MethodFact target) {
    public CallArcFact {
        Objects.requireNonNull(caller, "La méthode appelante ne peut pas être nulle");
        Objects.requireNonNull(target, "La méthode cible ne peut pas être nulle");
    }
}