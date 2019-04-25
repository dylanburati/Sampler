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

import java.text.DecimalFormat;
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

        DecimalFormat fmt = new DecimalFormat("0.###");
        for(int id : inputs) {
            EditText ed = ((EditText) holder.expandedView.findViewById(id));
            switch(id) {
                case R.id.pitch_min:
                    if(sample.shouldDisplay(Sample.FIELD_MIN_PITCH)) {
                        ed.setText(String.format("%d", sample.minPitch));
                    } else {
                        ed.setText("");
                    }
                    break;
                case R.id.pitch_max:
                    if(sample.shouldDisplay(Sample.FIELD_MAX_PITCH)) {
                        ed.setText(String.format("%d", sample.maxPitch));
                    } else {
                        ed.setText("");
                    }
                    break;
                case R.id.pitch_base:
                    if(sample.shouldDisplay(Sample.FIELD_BASE_PITCH)) {
                        ed.setText(String.format("%d", sample.basePitch));
                    } else {
                        ed.setText("");
                    }
                    break;
                case R.id.velocity_min:
                    if(sample.shouldDisplay(Sample.FIELD_MIN_VELOCITY)) {
                        ed.setText(String.format("%d", sample.minVelocity));
                    } else {
                        ed.setText("");
                    }
                    break;
                case R.id.velocity_max:
                    if(sample.shouldDisplay(Sample.FIELD_MAX_VELOCITY)) {
                        ed.setText(String.format("%d", sample.maxVelocity));
                    } else {
                        ed.setText("");
                    }
                    break;
                case R.id.position_start:
                    if(!sample.shouldUseDefaultLoopStart) {
                        ed.setText(fmt.format(sample.startTime));
                    } else {
                        ed.setText("");
                    }
                    break;
                case R.id.position_end:
                    if(!sample.shouldUseDefaultLoopResume) {
                        ed.setText(fmt.format(sample.endTime));
                    } else {
                        ed.setText("");
                    }
                    break;
                case R.id.position_loop:
                    if(!sample.shouldUseDefaultLoopEnd) {
                        ed.setText(fmt.format(sample.resumeTime));
                    } else {
                        ed.setText("");
                    }
                    break;
                case R.id.envelope_attack:
                    if(sample.shouldDisplay(Sample.FIELD_ATTACK)) {
                        ed.setText(fmt.format(sample.attack));
                    } else {
                        ed.setText("");
                    }
                    break;
                case R.id.envelope_decay:
                    if(sample.shouldDisplay(Sample.FIELD_DECAY)) {
                        ed.setText(fmt.format(sample.decay));
                    } else {
                        ed.setText("");
                    }
                    break;
                case R.id.envelope_sustain:
                    if(sample.shouldDisplay(Sample.FIELD_SUSTAIN)) {
                        float m = 20 * (float) Math.log10((double) sample.sustain);
                        if(m > 0) {
                            m = 0;
                        } else if(m < -120) {
                            m = -120;
                        }
                        ed.setText(fmt.format(m));
                    } else {
                        ed.setText("");
                    }
                    break;
                case R.id.envelope_release:
                    if(sample.shouldDisplay(Sample.FIELD_RELEASE)) {
                        ed.setText(fmt.format(sample.release));
                    } else {
                        ed.setText("");
                    }
                    break;
                default:
                    break;
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
                                edSample.setMinPitch(Integer.parseInt(s.toString()));
                            } catch(NumberFormatException ignored) {
                            }
                            break;
                        case R.id.pitch_max:
                            try {
                                edSample.setMaxPitch(Integer.parseInt(s.toString()));
                            } catch(NumberFormatException ignored) {
                            }
                            break;
                        case R.id.pitch_base:
                            try {
                                edSample.setBasePitch(Integer.parseInt(s.toString()));
                            } catch(NumberFormatException ignored) {
                            }
                            break;
                        case R.id.velocity_min:
                            try {
                                edSample.setMinVelocity(Integer.parseInt(s.toString()));
                            } catch(NumberFormatException ignored) {
                            }
                            break;
                        case R.id.velocity_max:
                            try {
                                edSample.setMaxVelocity(Integer.parseInt(s.toString()));
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
                                edSample.setAttack(Float.parseFloat(s.toString()));
                            } catch(NumberFormatException ignored) {
                            }
                            break;
                        case R.id.envelope_decay:
                            try {
                                edSample.setDecay(Float.parseFloat(s.toString()));
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
                                edSample.setSustain(m);
                            } catch(NumberFormatException ignored) {
                            }
                            break;
                        case R.id.envelope_release:
                            try {
                                edSample.setRelease(Float.parseFloat(s.toString()));
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
