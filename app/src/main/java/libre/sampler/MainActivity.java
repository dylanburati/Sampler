package libre.sampler;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProviders;
import libre.sampler.fragments.MainDetailFragment;
import libre.sampler.fragments.MainProjectsFragment;
import libre.sampler.models.MainViewModel;
import libre.sampler.models.Project;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity";
    private MainViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = ViewModelProviders.of(this).get(MainViewModel.class);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_fragment_root, new MainProjectsFragment())
                .commit();
    }

    public void openDetailFragment(int dialogActionType, Project dialogProject) {
        viewModel.setDialogActionType(dialogActionType);
        viewModel.setDialogProject(dialogProject);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_fragment_root, new MainDetailFragment())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack(null).commit();
    }
}
