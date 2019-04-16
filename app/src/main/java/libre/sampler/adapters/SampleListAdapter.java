package libre.sampler.adapters;

import android.animation.LayoutTransition;
import android.content.res.Resources;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.AutoTransition;
import android.transition.ChangeBounds;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.transition.TransitionManager;
import android.transition.TransitionPropagation;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import libre.sampler.R;
import libre.sampler.listeners.StatefulClickListener;
import libre.sampler.listeners.StatefulTextWatcher;
import libre.sampler.models.Instrument;
import libre.sampler.models.Sample;
import libre.sampler.utils.AdapterLoader;

public class SampleListAdapter extends RecyclerView.Adapter<SampleListAdapter.ViewHolder> implements AdapterLoader.Loadable<Sample> {
    public List<Sample> items;
    private final Set<ViewHolder> viewHolderSet;

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
            ed.setText("");
            ed.addTextChangedListener(new StatefulTextWatcher<SampleInputField>(new SampleInputField(sample, id)) {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if(this.data.sample == null || items.indexOf(this.data.sample) == -1) {
                        this.data.sample = null;
                        return;
                    }
                    switch(this.data.viewId) {
                        case R.id.pitch_min:
                            try {
                                this.data.sample.minPitch = Integer.parseInt(s.toString());
                            } catch(NumberFormatException ignored) {
                            }
                            break;
                        case R.id.pitch_max:
                            try {
                                this.data.sample.maxPitch = Integer.parseInt(s.toString());
                            } catch(NumberFormatException ignored) {
                            }
                            break;
                        case R.id.pitch_base:
                            try {
                                this.data.sample.basePitch = Integer.parseInt(s.toString());
                            } catch(NumberFormatException ignored) {
                            }
                            break;
                        case R.id.velocity_min:
                            try {
                                this.data.sample.minVelocity = Integer.parseInt(s.toString());
                            } catch(NumberFormatException ignored) {
                            }
                            break;
                        case R.id.velocity_max:
                            try {
                                this.data.sample.maxVelocity = Integer.parseInt(s.toString());
                            } catch(NumberFormatException ignored) {
                            }
                            break;
                        case R.id.position_start:
                            try {
                                this.data.sample.setLoopStart(Float.parseFloat(s.toString()));
                            } catch(NumberFormatException ignored) {
                            }
                            break;
                        case R.id.position_end:
                            try {
                                this.data.sample.setLoopEnd(Float.parseFloat(s.toString()));
                            } catch(NumberFormatException ignored) {
                            }
                            break;
                        case R.id.position_loop:
                            try {
                                this.data.sample.setLoopResume(Float.parseFloat(s.toString()));
                            } catch(NumberFormatException ignored) {
                            }
                            break;
                        case R.id.envelope_attack:
                            try {
                                this.data.sample.attack = Float.parseFloat(s.toString());
                            } catch(NumberFormatException ignored) {
                            }
                            break;
                        case R.id.envelope_decay:
                            try {
                                this.data.sample.decay = Float.parseFloat(s.toString());
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
                                this.data.sample.sustain = m;
                            } catch(NumberFormatException ignored) {
                            }
                            break;
                        case R.id.envelope_release:
                            try {
                                this.data.sample.release = Float.parseFloat(s.toString());
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
        public Sample sample;
        public int viewId;

        public SampleInputField(Sample sample, int viewId) {
            this.sample = sample;
            this.viewId = viewId;
        }
    }
}
