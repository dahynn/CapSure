package com.capsule.insurance.common.exception;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Safe to persist or log. Never include exception messages, SQL, or provider payloads. */
public final class SafeFailure {
    private SafeFailure() {}

    public static String describe(Throwable failure) {
        if (failure == null) return "UNEXPECTED_ERROR";
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable root = failure;
        for (int depth = 0; depth < 16 && root.getCause() != null; depth++) {
            if (!visited.add(root) || visited.contains(root.getCause())) break;
            root = root.getCause();
        }
        String code = failure instanceof BusinessException business
                ? business.getErrorCode().name() : "UNEXPECTED_ERROR";
        String type = root.getClass().getSimpleName().replaceAll("[^A-Za-z0-9_$]", "");
        return code + ":" + type.substring(0, Math.min(type.length(), 100));
    }
}
