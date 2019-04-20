package libre.sampler.adapters;

import android.text.Editable;
import android.transition.ChangeBounds;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import libre.sampler.R;
import libre.sampler.listeners.StatefulClickListener;
import libre.sampler.listeners.StatefulTextWatcher;
import libre.sampler.models.Sample;
import libre.sampler.utils.AdapterLoader;

public class SampleListAdapter extends RecyclerView.Adapter<SampleListAdapter.ViewHolder> implements AdapterLoader.Loadable<Sample> {
    public List<Sample> items;
    private final Set<ViewHolder> viewHolderSet;
    private int prevSize;

    @Override
    public List<Sample> items() {
        return items;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public boolean isCollapsed = true;
        private LinearLayout rootView;
        private View collapsedView;
        private View expandIcon;
        private View expandedView;

        public ViewHolder(LinearLayout v) {
            super(v);
            rootView = v;
            collapsedView = v.findViewById(R.id.collapsed_sample_data);
            expandIcon = v.findViewById(R.id.icon_expand);
            expandedView = v.findViewById(R.id.expanded_sample_data);
        }

        public void collapse() {
            if(!isCollapsed) {
                Transition tr = new ChangeBounds();
                tr.setDuration(300);
                TransitionManager.beginDelayedTransition(rootView, tr);
                expandedView.setVisibility(View.GONE);
                expandIcon.setBackground(rootView.getContext().getDrawable(R.drawable.ic_keyboard_arrow_down));
                isCollapsed = true;
            }
        }

        public void expand() {
            if(isCollapsed) {
                Transition tr = new ChangeBounds();
                tr.setDuration(300);
                TransitionManager.beginDelayedTransition(rootView, tr);
                expandedView.setVisibility(View.VISIBLE);
                expandIcon.setBackground(rootView.getContext().getDrawable(R.drawable.ic_keyboard_arrow_up));
                isCollapsed = false;
            }
        }
    }

    public SampleListAdapter(List<Sample> items) {
        this.items = items;
        this.prevSize = items.size();
        // this.edited = new boolean[this.prevSize];
        // Arrays.fill(edited, false);
        this.viewHolderSet = new HashSet<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout v = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.component_sample_list_tile, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        viewHolderSet.add(holder);
        holder.collapse();
        holder.collapsedView.setOnClickListener(
                new StatefulClickListener<ViewHolder>(holder) {
                    @Override
                    public void onClick(View v) {
                        if(this.data.isCollapsed) {
                            this.data.expand();
                            for(ViewHolder other : viewHolderSet) {
                                if(other != null && !this.data.equals(other)) {
                                    other.collapse();
                                }
                            }
                        } else {
                            this.data.collapse();
                        }
                    }
                });
        holder.expandedView.findViewById(R.id.sample_remove).setOnClickListener(new StatefulClickListener<ViewHolder>(holder) {
            @Override
            public void onClick(View v) {
                viewHolderSet.remove(this.data);
                AdapterLoader.removeItem(SampleListAdapter.this, this.data.getAdapterPosition());
            }
        });

        Sample sample = this.items.get(position);
        String label = String.format("%03d %s", position + 1, sample.filename);
        ((TextView) holder.collapsedView.findViewById(R.id.sample_label)).setText(label);

        int[] inputs = new int[]{R.id.pitch_min, R.id.pitch_max, R.id.pitch_base,
                R.id.velocity_min, R.id.velocity_max,
                R.id.position_start, R.id.position_end, R.id.position_loop,
                R.id.envelope_attack, R.id.envelope_decay, R.id.envelope_sustain, R.id.envelope_release};

        for(int id : inputs) {
            EditText ed = ((EditText) holder.expandedView.findViewById(id));
            if(position < this.prevSize) {
                switch(id) {
                    case R.id.pitch_min:
                        ed.setText(String.format("%d", sample.minPitch));
                        break;
                    case R.id.pitch_max:
                        ed.setText(String.format("%d", sample.maxPitch));
                        break;
                    case R.id.pitch_base:
                        ed.setText(String.format("%d", sample.basePitch));
                        break;
                    case R.id.velocity_min:
                        ed.setText(String.format("%d", sample.minVelocity));
                        break;
                    case R.id.velocity_max:
                        ed.setText(String.format("%d", sample.maxVelocity));
                        break;
                    case R.id.position_start:
                        if(!sample.shouldUseDefaultLoopStart) {
                            ed.setText(String.format("%.3f", sample.startTime));
                        }
                        break;
                    case R.id.position_end:
                        if(!sample.shouldUseDefaultLoopResume) {
                            ed.setText(String.format("%.3f", sample.resumeTime));
                        }
                        break;
                    case R.id.position_loop:
                        if(!sample.shouldUseDefaultLoopEnd) {
                            ed.setText(String.format("%.3f", sample.endTime));
                        }
                        break;
                    case R.id.envelope_attack:
                        ed.setText(String.format("%.1f", sample.attack));
                        break;
                    case R.id.envelope_decay:
                        ed.setText(String.format("%.1f", sample.decay));
                        break;
                    case R.id.envelope_sustain:
                        float m = 20 * (float) Math.log10((double) sample.sustain);
                        if(m > 0) {
                            m = 0;
                        } else if(m < -120) {
                            m = -120;
                        }
                        ed.setText(String.format("%.1f", m));
                        break;
                    case R.id.envelope_release:
                        ed.setText(String.format("%.1f", sample.release));
                        break;
                    default:
                        break;
                }
            } else {
                ed.setText("");
            }

            ed.addTextChangedListener(new StatefulTextWatcher<SampleInputField>(new SampleInputField(holder, ed)) {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // tmp
                    if(!this.data.view.equals(this.data.viewHolder.expandedView.findViewById(this.data.view.getId()))) {
                        throw new RuntimeException();
                    }
                    // tmp>

                    int position = this.data.viewHolder.getAdapterPosition();
                    if(position > items.size()) {
                        return;
                    }
                    Sample edSample = items.get(position);
                    if(edSample == null) {
                        return;
                    }
                    switch(this.data.view.getId()) {
                        case R.id.pitch_min:
                            try {
                                edSample.minPitch = Integer.parseInt(s.toString());
                            } catch(NumberFormatException ignored) {
                            }
                            break;
                        case R.id.pitch_max:
                            try {
                                edSample.maxPitch = Integer.parseInt(s.toString());
                            } catch(NumberFormatException ignored) {
                            }
                            break;
                        case R.id.pitch_base:
                            try {
                                edSample.basePitch = Integer.parseInt(s.toString());
                            } catch(NumberFormatException ignored) {
                            }
                            break;
                        case R.id.velocity_min:
                            try {
                                edSample.minVelocity = Integer.parseInt(s.toString());
                            } catch(NumberFormatException ignored) {
                            }
                            break;
                        case R.id.velocity_max:
                            try {
                                edSample.maxVelocity = Integer.parseInt(s.toString());
                            } catch(NumberFormatException ignored) {
                            }
                            break;
                        case R.id.position_start:
                            try {
                                edSample.setLoopStart(Float.parseFloat(s.toString()));
                            } catch(NumberFormatException ignored) {
                            }
                            break;
                        case R.id.position_end:
                            try {
                                edSample.setLoopEnd(Float.parseFloat(s.toString()));
                            } catch(NumberFormatException ignored) {
                            }
                            break;
                        case R.id.position_loop:
                            try {
                                edSample.setLoopResume(Float.parseFloat(s.toString()));
                            } catch(NumberFormatException ignored) {
                            }
                            break;
                        case R.id.envelope_attack:
                            try {
                                edSample.attack = Float.parseFloat(s.toString());
                            } catch(NumberFormatException ignored) {
                            }
                            break;
                        case R.id.envelope_decay:
                            try {
                                edSample.decay = Float.parseFloat(s.toString());
                            } catch(NumberFormatException ignored) {
                            }
                            break;
                        case R.id.envelope_sustain:
                            try {
                                float m = Float.parseFloat(s.toString());
                                if(m >= 0) {
                                    m = 1;
                                } else {
                                    m = (float) Math.pow(10, m / 20.0);  // dB to amplitude
                                }
                                edSample.sustain = m;
                            } catch(NumberFormatException ignored) {
                            }
                            break;
                        case R.id.envelope_release:
                            try {
                                edSample.release = Float.parseFloat(s.toString());
                            } catch(NumberFormatException ignored) {
                            }
                            break;
                        default:
                            break;
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return this.items.size();
    }

    private static class SampleInputField {
        public ViewHolder viewHolder;
        public View view;

        public SampleInputField(ViewHolder viewHolder, View view) {
            this.viewHolder = viewHolder;
            this.view = view;
        }
    }
}
