package libre.sampler.adapters;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import libre.sampler.fragments.PlaceholderFragment;
import libre.sampler.fragments.ProjectKeyboardFragment;

public class ProjectFragmentAdapter extends FragmentPagerAdapter {
    public ProjectFragmentAdapter(@NonNull FragmentManager fm) {
        super(fm);
    }

    private Fragment[] fragments = new Fragment[4];
    private static class PROJECT_FRAGMENTS {
        private static final int INSTRUMENTS = 0;
        private static final int KEYBOARD = 1;
        private static final int PATTERNS = 2;
        private static final int SETTINGS = 3;
        private static String[] titles = new String[]{
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
        if(fragments[position] == null) {
            switch(position) {
                case PROJECT_FRAGMENTS.KEYBOARD:
                    fragments[position] = new ProjectKeyboardFragment();
                    break;
                case PROJECT_FRAGMENTS.INSTRUMENTS:
                case PROJECT_FRAGMENTS.PATTERNS:
                case PROJECT_FRAGMENTS.SETTINGS:
                    fragments[position] = new PlaceholderFragment();
                    break;
            }
        }
        return fragments[position];
    }

    @Override
    public int getCount() {
        return 4;
    }
}
