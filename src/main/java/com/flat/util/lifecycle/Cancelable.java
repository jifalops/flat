package com.flat.util.lifecycle;

/**
 * Something that can be canceled, i.e. a Thread.
 */
public interface Cancelable {
    void cancel();
    boolean isCanceled();

    static interface CancelListener {
        void onCanceled();
    }
}
