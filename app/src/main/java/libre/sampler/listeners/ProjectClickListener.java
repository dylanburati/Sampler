package libre.sampler.listeners;

import android.graphics.drawable.TransitionDrawable;
import android.util.Log;
import android.view.View;

import libre.sampler.models.Project;

public class ProjectClickListener implements View.OnClickListener {
    private Project item;

    public ProjectClickListener(Project item) {
        this.item = item;
    }

    @Override
    public void onClick(View v) {
        Log.d("Project click listener", item.name);
    }
}
