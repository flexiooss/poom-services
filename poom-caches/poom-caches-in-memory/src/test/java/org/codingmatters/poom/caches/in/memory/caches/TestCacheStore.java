package org.codingmatters.poom.caches.in.memory.caches;

import org.codingmatters.poom.caches.in.memory.stores.CacheStore;
import org.codingmatters.poom.caches.in.memory.stores.MapCacheStore;

import java.util.*;

public class TestCacheStore<K, V> implements CacheStore<K, V> {

    public enum ActionType {
        GET,
        STORE,
        HAS,
        REMOVE,
        CLEAR,
        KEYS;

        public ActionBuilder action(Object ... args) {
            return new ActionBuilder(this, args);
        }

        static public class ActionBuilder {
            private final ActionType actionType;
            private final Object[] args;

            public ActionBuilder(ActionType actionType, Object[] args) {
                this.actionType = actionType;
                this.args = args;
            }

            public Action returning(Object result) {
                return new Action(this.actionType, this.args, result);
            }
            public Action notReturning() {
                return new Action(this.actionType, this.args, null);
            }
        }
    }

    static public class Action {
        private final ActionType actionType;
        private final Object[] args;
        private final Object result;

        private Action(ActionType actionType, Object[] args, Object result) {
            this.actionType = actionType;
            this.args = args;
            this.result = result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Action action = (Action) o;
            return actionType == action.actionType &&
                    Arrays.equals(args, action.args) &&
                    Objects.equals(result, action.result);
        }

        @Override
        public int hashCode() {
            int result1 = Objects.hash(actionType, result);
            result1 = 31 * result1 + Arrays.hashCode(args);
            return result1;
        }

        @Override
        public String toString() {
            return "Action{" +
                    "actionType=" + actionType +
                    ", args=" + Arrays.toString(args) +
                    ", result=" + result +
                    '}';
        }
    }

    private final MapCacheStore<K, V> delegate = new MapCacheStore<>();
    private final List<Action> actions = new LinkedList<>();

    public List<Action> actions() {
        return actions;
    }

    @Override
    public Optional<V> get(K key) {
        Optional<V> result = this.delegate.get(key);
        this.actions.add(ActionType.GET.action(key).returning(result));
        return result;
    }

    @Override
    public void store(K key, V value) {
        this.actions.add(ActionType.STORE.action(key, value).notReturning());
        this.delegate.store(key, value);
    }

    @Override
    public boolean has(K key) {
        boolean has = this.delegate.has(key);
        this.actions.add(ActionType.HAS.action(key).returning(has));
        return has;
    }

    @Override
    public void remove(K key) {
        this.actions.add(ActionType.REMOVE.action(key).notReturning());
        this.delegate.remove(key);
    }

    @Override
    public void clear() {
        this.actions.add(ActionType.CLEAR.action().notReturning());
        this.delegate.clear();
    }

    @Override
    public Set<K> keys() {
        Set<K> keys = this.delegate.keys();
        this.actions.add(ActionType.KEYS.action().returning(keys));
        return keys;
    }
}
