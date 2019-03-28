package libre.sampler.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import libre.sampler.R;
import libre.sampler.adapters.PianoAdapter;
import libre.sampler.models.NoteEvent;

public class ProjectPatternsFragment extends Fragment {
    private RecyclerView pianoContainer;
    private PianoAdapter pianoAdapter;

    public ProjectPatternsFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_project_patterns, container, false);
        pianoContainer = rootView.findViewById(R.id.piano_container);
        pianoAdapter = new PianoAdapter(new Consumer<NoteEvent>() {
            @Override
            public void accept(NoteEvent noteEvent) {
                Log.d("StatefulTouchListener", String.format("noteEvent: keynum=%d action=%d", noteEvent.keyNum, noteEvent.action));
            }
        });
        pianoContainer.setAdapter(pianoAdapter);
        return rootView;
    }
}
