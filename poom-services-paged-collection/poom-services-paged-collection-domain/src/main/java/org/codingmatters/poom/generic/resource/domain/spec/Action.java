package org.codingmatters.poom.generic.resource.domain.spec;

import java.util.*;

public enum Action {
    RETRIEVE, CREATE, UPDATE, REPLACE, DELETE;

    static public final Set<Action> none = actions();
    static public final Set<Action> all = new HashSet<>(Arrays.asList(Action.values()));

    static public Set<Action> actions(Action ... actions) {
        if(actions == null) return new HashSet<Action>();
        return new HashSet<>(Arrays.asList(actions));
    }

    static public Set<Action> actions(Set<Action> set, Action ... actions) {
        if(actions == null) return set;
        Set<Action> result = new HashSet<>(set);
        for (Action action : actions) {
            result.add(action);
        }
        return result;
    }

    static public Optional<Action> from(String name) {
        if(name == null) return Optional.empty();
        try {
            return Optional.of(Action.valueOf(name.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
