package libre.sampler.utils;

import android.view.ViewGroup;

import androidx.transition.Transition;
import androidx.transition.TransitionValues;
import androidx.transition.VisibilityPropagation;

public class NoTransitionPropagation extends VisibilityPropagation {
    @Override
    public long getStartDelay(ViewGroup sceneRoot, Transition transition, TransitionValues startValues, TransitionValues endValues) {
        return 0;
    }
}
