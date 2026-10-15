package org.codingmatters.poom.mcp.processor;

import org.codingmatters.value.objects.values.ObjectValue;
import org.codingmatters.value.objects.values.PropertyValue;

/**
 * Lecture défensive des paramètres envoyés par le client. {@code PropertyValue.single().stringValue()}
 * fait un cast non vérifié : un {@code "name":42} lèverait une ClassCastException (AssertionError sous
 * {@code -ea}), et {@code single()} d'un tableau vide rend null. Ici, une valeur absente, nulle, multiple
 * ou d'un autre type rend null : à l'appelant de la traiter comme absente ou invalide.
 */
final class Params {

    private Params() {}

    /** La propriété existe-t-elle (quelle que soit sa valeur) ? */
    static boolean present(ObjectValue o, String key) {
        return o != null && o.property(key) != null;
    }

    static String string(ObjectValue o, String key) {
        PropertyValue.Value value = single(o, key, PropertyValue.Type.STRING);
        return value != null ? value.stringValue() : null;
    }

    static ObjectValue object(ObjectValue o, String key) {
        PropertyValue.Value value = single(o, key, PropertyValue.Type.OBJECT);
        return value != null ? value.objectValue() : null;
    }

    private static PropertyValue.Value single(ObjectValue o, String key, PropertyValue.Type type) {
        if (o == null) return null;
        PropertyValue property = o.property(key);
        if (property == null || property.cardinality() != PropertyValue.Cardinality.SINGLE) return null;
        PropertyValue.Value value = property.single();
        if (value == null || value.isNull() || value.type() != type) return null;
        return value;
    }
}
