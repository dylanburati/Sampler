package libre.sampler.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.io.File;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;
import libre.sampler.R;
import libre.sampler.adapters.SampleListAdapter;
import libre.sampler.models.Instrument;
import libre.sampler.models.Sample;
import libre.sampler.utils.AdapterLoader;

public class InstrumentCreateDialog extends DialogFragment {
    private EditText nameInputView;

    private EditText sampleInputView;
    private Button sampleAddButton;
    private RecyclerView sampleData;
    private SampleListAdapter sampleDataAdapter;
    private Instrument toCreate;
    private InstrumentCreateDialogListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        listener = (InstrumentCreateDialogListener) context;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        toCreate = new Instrument(null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = LayoutInflater.from(requireActivity());
        LinearLayout rootView = (LinearLayout) inflater.inflate(R.layout.dialog_instrument_create, null);
        nameInputView = (EditText) rootView.findViewById(R.id.input_name);
        sampleInputView = (EditText) rootView.findViewById(R.id.input_sample_paths);
        sampleAddButton = (Button) rootView.findViewById(R.id.submit_sample_paths);
        sampleData = (RecyclerView) rootView.findViewById(R.id.sample_data);
        sampleDataAdapter = new SampleListAdapter(new ArrayList<Sample>());
        sampleData.setAdapter(sampleDataAdapter);
        sampleAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File sampleFile = new File(sampleInputView.getText().toString());
                // todo support wildcard search: File.listFiles
                if(sampleFile.isFile() && sampleFile.canRead()) {
                    AdapterLoader.insertItem(sampleDataAdapter, new Sample(sampleFile.getAbsolutePath(), -1, -1));
                }
            }
        });

        builder.setView(rootView)
                .setPositiveButton(R.string.dialog_project_create_submit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = nameInputView.getText().toString();
                        toCreate.name = name;

                        for(Sample s : sampleDataAdapter.items) {
                            if(s == null) {
                                continue;
                            }
                            toCreate.addSample(s);
                        }
                        if(listener != null) listener.onInstrumentCreate(toCreate);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.dialog_project_create_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        return builder.create();
    }

    public interface InstrumentCreateDialogListener {
        public void onInstrumentCreate(Instrument instrument);
    }
}
