package com.essentiallocalization.localization.scheme;

import android.os.Bundle;

import com.essentiallocalization.localization.Node;
import com.essentiallocalization.localization.ranging.Ranging;


public interface Scheme {
    /**
     * @return a State that can either be accepted or rejected as the Node's new state.
     */
    Node.State predictState(Node subject, Node[] nodes, Ranging[] ranges);
}
