package libre.sampler.listeners;

import android.content.Context;
import android.graphics.drawable.TransitionDrawable;
import android.util.Log;
import android.view.View;

import libre.sampler.models.Project;

public abstract class ProjectClickListener implements View.OnClickListener {
    public Project project = null;
    public ProjectClickListener(Project p) {
        this.project = p;
    }
}
