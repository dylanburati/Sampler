package libre.sampler.utils;

import android.transition.Transition;
import android.transition.TransitionValues;
import android.transition.VisibilityPropagation;
import android.view.ViewGroup;

public class NoTransitionPropagation extends VisibilityPropagation {
    @Override
    public long getStartDelay(ViewGroup sceneRoot, Transition transition, TransitionValues startValues, TransitionValues endValues) {
        return 0;
    }
}
