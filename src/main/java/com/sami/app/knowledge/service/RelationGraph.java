package com.sami.app.knowledge.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Cycle detection over directed article relationships.
 *
 * <p>Prerequisite and supersedes chains must stay acyclic: a cycle makes
 * "what must I read first?" unanswerable and sends any reader — or any future
 * recommendation engine — round in circles. Symmetric relations such as
 * "related" are exempt and are never passed here.
 *
 * <p>Pure logic and unit-testable.
 */
@Component
public class RelationGraph {

    /**
     * Refuses a new edge that would close a cycle.
     *
     * @param edges existing directed edges: article id → the articles it points at
     */
    public void assertNoCycle(Map<Long, List<Long>> edges, Long from, Long to) {
        if (from.equals(to)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "An article cannot reference itself");
        }
        // The new edge closes a cycle exactly when `to` can already reach `from`.
        if (reaches(edges, to, from)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "This relationship would create a circular dependency between articles");
        }
    }

    /** Iterative depth-first reachability; iterative so a deep chain cannot blow the stack. */
    public boolean reaches(Map<Long, List<Long>> edges, Long start, Long target) {
        Set<Long> visited = new HashSet<>();
        Deque<Long> stack = new ArrayDeque<>();
        stack.push(start);

        while (!stack.isEmpty()) {
            Long current = stack.pop();
            if (current.equals(target)) {
                return true;
            }
            if (!visited.add(current)) {
                continue;
            }
            for (Long next : edges.getOrDefault(current, List.of())) {
                if (!visited.contains(next)) {
                    stack.push(next);
                }
            }
        }
        return false;
    }
}
