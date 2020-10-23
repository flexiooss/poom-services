package org.codingmatters.poom.caches.in.memory.lru;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class LRUManager<K> {
    private final int capacity;
    private final LinkedList<K> delegate = new LinkedList<>();

    public LRUManager(int capacity) {
        this.capacity = capacity;
    }

    public synchronized void accessed(K key) {
        this.delegate.remove(key);
        this.delegate.push(key);
    }

    public synchronized List<K> overload() {
        if(this.delegate.size() > this.capacity) {
            return this.delegate.subList(this.capacity, this.delegate.size());
        } else {
            return Collections.emptyList();
        }
    }

    public synchronized void removed(K key) {
        this.delegate.remove(key);
    }
}
