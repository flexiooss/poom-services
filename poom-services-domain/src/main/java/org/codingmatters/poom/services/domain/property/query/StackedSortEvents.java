package org.codingmatters.poom.services.domain.property.query;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

public class StackedSortEvents<T> implements SortEvents<Void> {
    private final Stack<T> stack = new Stack<>();
    private final T defaultValue;

    public StackedSortEvents(T defaultValue) {
        this.defaultValue = defaultValue;
    }

    public void reset() {
        this.stack.clear();
    }

    public T result() {
        return this.stack.isEmpty() ? this.defaultValue : this.stack.peek();
    }

    protected void push(T o) {
        this.stack.push(o);
    }

    protected T pop() {
        return this.stack.pop();
    }

    protected List<T> popAll() {
        List<T> result = new LinkedList<>();
        while(! this.stack.empty()) {
            result.add(this.stack.pop());
        }
        return result;
    }

    protected List<T> reversedPopAll() {
        List<T> result = this.popAll();
        Collections.reverse(result);
        return result;
    }
}
