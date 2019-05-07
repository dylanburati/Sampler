package libre.sampler.adapters;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import libre.sampler.fragments.ProjectInstrumentsFragment;
import libre.sampler.fragments.ProjectKeyboardFragment;
import libre.sampler.fragments.ProjectPatternsFragment;
import libre.sampler.fragments.ProjectSettingsFragment;

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
                return new ProjectPatternsFragment();
            case PROJECT_FRAGMENTS.SETTINGS:
                return new ProjectSettingsFragment();
        }
        return null;
    }



    @Override
    public int getCount() {
        return 4;
    }
}
