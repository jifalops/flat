package com.flat.util.lifecycle;

/**
 * Something that will end at some point, i.e. on another thread.
 * Does not imply the user can stop it or that it failed.
 */
public interface Finishable {
    boolean isFinished();

    static interface FinishListener {
        void onFinished();
    }
}
