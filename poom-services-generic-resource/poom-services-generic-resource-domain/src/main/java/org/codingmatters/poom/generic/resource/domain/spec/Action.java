package org.codingmatters.poom.generic.resource.domain.spec;

import java.util.*;

public enum Action {
    CREATE, UPDATE, REPLACE
    ;

    static public final Set<Action> all = new HashSet<>(Arrays.asList(Action.values()));
    static public final Set<Action> createUpdate = new HashSet<>(Arrays.asList(CREATE, UPDATE));
    static public final Set<Action> createReplace = new HashSet<>(Arrays.asList(CREATE, REPLACE));
    static public final Set<Action> updateReplace = new HashSet<>(Arrays.asList(UPDATE, REPLACE));
    static public final Set<Action> create = new HashSet<>(Arrays.asList(CREATE));
    static public final Set<Action> update = new HashSet<>(Arrays.asList(UPDATE));
    static public final Set<Action> replace = new HashSet<>(Arrays.asList(REPLACE));

    static public Optional<Action> from(String name) {
        if(name == null) return Optional.empty();
        try {
            return Optional.of(Action.valueOf(name.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
