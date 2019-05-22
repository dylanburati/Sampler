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
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;
import libre.sampler.R;
import libre.sampler.adapters.SampleListAdapter;
import libre.sampler.models.Instrument;
import libre.sampler.models.InstrumentEvent;
import libre.sampler.models.ProjectViewModel;
import libre.sampler.models.Sample;
import libre.sampler.utils.AppConstants;

public class InstrumentEditDialog extends DialogFragment {
    private EditText nameInputView;
    private EditText sampleInputView;
    private Button sampleAddButton;
    private RecyclerView sampleData;
    private SampleListAdapter sampleDataAdapter;
    private Button instrumentDeleteButton;

    private ProjectViewModel viewModel;
    public String defaultSamplePath;
    private List<Sample> sampleList;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = LayoutInflater.from(requireActivity());
        LinearLayout rootView = (LinearLayout) inflater.inflate(R.layout.dialog_instrument_edit, null);
        nameInputView = (EditText) rootView.findViewById(R.id.input_name);
        instrumentDeleteButton = (Button) rootView.findViewById(R.id.delete_instrument);
        sampleInputView = (EditText) rootView.findViewById(R.id.input_sample_paths);
        sampleAddButton = (Button) rootView.findViewById(R.id.submit_sample_paths);
        sampleData = (RecyclerView) rootView.findViewById(R.id.sample_data);

        viewModel = ViewModelProviders.of(getActivity()).get(ProjectViewModel.class);

        if(savedInstanceState != null) {
            nameInputView.setText(savedInstanceState.getString(AppConstants.TAG_SAVED_STATE_INSTRUMENT_CREATE_NAME));
            sampleInputView.setText(savedInstanceState.getString(AppConstants.TAG_SAVED_STATE_INSTRUMENT_CREATE_PATH));
        } else {
            nameInputView.setText(viewModel.getEditDialogInstrument().name);
            String prevFilename = viewModel.getEditDialogInstrument().firstFilename();
            if(prevFilename != null) {
                sampleInputView.setText(prevFilename);
            } else if(defaultSamplePath != null) {
                sampleInputView.setText(defaultSamplePath);
            }
        }

        sampleList = viewModel.getEditDialogInstrument().getSamples();
        sampleDataAdapter = new SampleListAdapter(sampleList);
        sampleData.setAdapter(sampleDataAdapter);
        sampleAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File sampleFile = new File(sampleInputView.getText().toString());
                // todo support wildcard search: File.listFiles
                if(sampleFile.isFile() && sampleFile.canRead()) {
                    int insertIdx = sampleList.size();
                    viewModel.getEditDialogInstrument().addSample(sampleFile.getAbsolutePath());
                    sampleDataAdapter.notifyItemInserted(insertIdx);
                }
            }
        });

        instrumentDeleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDialog().cancel();
                viewModel.instrumentEventSource.dispatch(new InstrumentEvent(InstrumentEvent.INSTRUMENT_DELETE, viewModel.getEditDialogInstrument()));
                viewModel.setEditDialogInstrument(null);
            }
        });

        builder.setView(rootView)
                .setPositiveButton(R.string.dialog_project_edit_submit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = nameInputView.getText().toString();
                        viewModel.getEditDialogInstrument().name = name;

                        viewModel.getEditDialogInstrument().setSamples(sampleList);
                        Instrument toEdit = viewModel.getEditDialogInstrument();
                        viewModel.instrumentEventSource.dispatch(new InstrumentEvent(InstrumentEvent.INSTRUMENT_EDIT, toEdit));
                        viewModel.setEditDialogInstrument(null);
                        dialog.dismiss();
                    }
                });

        return builder.create();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(AppConstants.TAG_SAVED_STATE_INSTRUMENT_CREATE_NAME, nameInputView.getText().toString());
        outState.putString(AppConstants.TAG_SAVED_STATE_INSTRUMENT_CREATE_PATH, sampleInputView.getText().toString());
    }
}