package com.essentiallocalization.localization.scheme;

import android.os.Bundle;

import com.essentiallocalization.localization.Node;


public interface Scheme {
    Node.State calcNewState(Node node, Bundle args);
}
