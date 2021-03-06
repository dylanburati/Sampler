package libre.sampler.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProviders;
import libre.sampler.R;
import libre.sampler.models.Instrument;
import libre.sampler.models.InstrumentEvent;
import libre.sampler.models.ProjectViewModel;
import libre.sampler.utils.AppConstants;

public class InstrumentEditDialog extends DialogFragment {
    private EditText nameInputView;
    private Button instrumentDeleteButton;

    private ProjectViewModel viewModel;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = LayoutInflater.from(requireActivity());
        ConstraintLayout rootView = (ConstraintLayout) inflater.inflate(R.layout.dialog_instrument_edit, null);
        nameInputView = rootView.findViewById(R.id.input_name);
        instrumentDeleteButton = rootView.findViewById(R.id.delete_instrument);

        viewModel = ViewModelProviders.of(getActivity()).get(ProjectViewModel.class);

        if(savedInstanceState != null) {
            nameInputView.setText(savedInstanceState.getString(AppConstants.TAG_SAVED_STATE_INSTRUMENT_EDIT_NAME));
        } else {
            nameInputView.setText(viewModel.getDialogInstrument().name);
        }

        instrumentDeleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Instrument toRemove = viewModel.getDialogInstrument();
                viewModel.removeInstrument(toRemove);
                viewModel.setDialogInstrument(null);
                getDialog().cancel();
            }
        });

        builder.setView(rootView)
                .setPositiveButton(R.string.dialog_project_edit_submit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        viewModel.getDialogInstrument().name = nameInputView.getText().toString();

                        Instrument toEdit = viewModel.getDialogInstrument();
                        viewModel.instrumentEventSource.dispatch(new InstrumentEvent(InstrumentEvent.INSTRUMENT_EDIT, toEdit));
                        viewModel.setDialogInstrument(null);
                        dialog.dismiss();
                    }
                });

        return builder.create();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(AppConstants.TAG_SAVED_STATE_INSTRUMENT_EDIT_NAME, nameInputView.getText().toString());
    }
}