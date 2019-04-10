package libre.sampler.adapters;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import libre.sampler.fragments.PlaceholderFragment;
import libre.sampler.fragments.ProjectInstrumentsFragment;
import libre.sampler.fragments.ProjectKeyboardFragment;
import libre.sampler.publishers.NoteEventSource;
import libre.sampler.utils.EventSource;

public class ProjectFragmentAdapter extends FragmentPagerAdapter {
    private final FragmentManager fm;

    public ProjectFragmentAdapter(@NonNull FragmentManager fm) {
        super(fm);
        this.fm = fm;
    }

    public static class PROJECT_FRAGMENTS {
        public static final int INSTRUMENTS = 0;
        public static final int KEYBOARD = 1;
        public static final int PATTERNS = 2;
        public static final int SETTINGS = 3;
        public static String[] titles = new String[]{
                "Instruments", "Keyboard", "Patterns", "Settings"};
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return PROJECT_FRAGMENTS.titles[position];
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        switch(position) {
            case PROJECT_FRAGMENTS.INSTRUMENTS:
                return new ProjectInstrumentsFragment();
            case PROJECT_FRAGMENTS.KEYBOARD:
                return new ProjectKeyboardFragment();
            case PROJECT_FRAGMENTS.PATTERNS:
            case PROJECT_FRAGMENTS.SETTINGS:
                return new PlaceholderFragment();
        }
        return null;
    }



    @Override
    public int getCount() {
        return 4;
    }
}
