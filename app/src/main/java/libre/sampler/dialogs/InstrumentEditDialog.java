package libre.sampler.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;
import libre.sampler.R;
import libre.sampler.adapters.SampleListAdapter;
import libre.sampler.models.Instrument;
import libre.sampler.models.Sample;
import libre.sampler.utils.AdapterLoader;
import libre.sampler.utils.AppConstants;

public class InstrumentEditDialog extends DialogFragment {
    private EditText nameInputView;

    private EditText sampleInputView;
    private Button sampleAddButton;
    private RecyclerView sampleData;
    private SampleListAdapter sampleDataAdapter;
    private InstrumentEditDialogListener listener;
    private Button instrumentDeleteButton;

    public Instrument previousInstrument;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        listener = (InstrumentEditDialogListener) context;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        if(savedInstanceState != null) {
            previousInstrument = savedInstanceState.getParcelable(AppConstants.TAG_SAVED_STATE_INSTRUMENT);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = LayoutInflater.from(requireActivity());
        LinearLayout rootView = (LinearLayout) inflater.inflate(R.layout.dialog_instrument_edit, null);
        nameInputView = (EditText) rootView.findViewById(R.id.input_name);
        instrumentDeleteButton = (Button) rootView.findViewById(R.id.delete_instrument);
        sampleInputView = (EditText) rootView.findViewById(R.id.input_sample_paths);
        sampleAddButton = (Button) rootView.findViewById(R.id.submit_sample_paths);
        sampleData = (RecyclerView) rootView.findViewById(R.id.sample_data);
        sampleDataAdapter = new SampleListAdapter(previousInstrument.getSamples());
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

        nameInputView.setText(previousInstrument.name);

        final SharedPreferences sharedPreferences = rootView.getContext().getSharedPreferences(
                AppConstants.TAG_SHARED_PREFS, Context.MODE_PRIVATE);
        String defaultSamplePath = sharedPreferences.getString(AppConstants.PREF_DEFAULT_SAMPLE_PATH, null);
        if(defaultSamplePath != null) {
            sampleInputView.setText(defaultSamplePath);
        }
        
        instrumentDeleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDialog().cancel();
                listener.onInstrumentDelete(previousInstrument);
            }
        });

        builder.setView(rootView)
                .setPositiveButton(R.string.dialog_project_edit_submit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = nameInputView.getText().toString();
                        previousInstrument.name = name;

                        previousInstrument.setSamples(sampleDataAdapter.items);
                        listener.onInstrumentEdit(previousInstrument);
                        dialog.dismiss();
                    }
                });

        return builder.create();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putParcelable(AppConstants.TAG_SAVED_STATE_INSTRUMENT, (Parcelable) previousInstrument);
    }

    public interface InstrumentEditDialogListener {
        public void onInstrumentEdit(Instrument instrument);
        public void onInstrumentDelete(Instrument instrument);
    }
}
