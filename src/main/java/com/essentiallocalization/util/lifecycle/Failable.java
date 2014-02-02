package com.essentiallocalization.util.lifecycle;

/**
 * For classes that fail internally but failure can be checked and acted upon.
 */
public interface Failable {
    boolean hasFailed();

    static interface Listener {
        void onFail();
    }
}
