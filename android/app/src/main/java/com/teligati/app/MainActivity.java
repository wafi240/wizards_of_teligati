package com.teligati.app;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.teligati.app.fragment.*;

public class MainActivity extends AppCompatActivity {
    private String userRole = "STUDENT";
    private String userId = "2307003";
    private FloatingActionButton fabAi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getIntent() != null) {
            if (getIntent().getStringExtra("USER_ROLE") != null) {
                userRole = getIntent().getStringExtra("USER_ROLE");
            }
            if (getIntent().getStringExtra("USER_ID") != null) {
                userId = getIntent().getStringExtra("USER_ID");
            }
        }

        fabAi = findViewById(R.id.fabAiWizard);

        // Auto-hide the AI FAB when already inside AiFragment so it never overlaps the Send button!
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
                if (fabAi != null) {
                    if (f instanceof AiFragment) {
                        fabAi.hide();
                    } else {
                        fabAi.show();
                    }
                }
            }
        }, false);

        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        nav.setOnItemSelectedListener(item -> {
            Fragment selected = null;
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                selected = new HomeFragment();
            } else if (id == R.id.nav_explore) {
                selected = new ExploreFragment();
            } else if (id == R.id.nav_opportunities) {
                selected = new OpportunitiesFragment();
            } else if (id == R.id.nav_projects) {
                selected = new ProjectsFragment();
            } else if (id == R.id.nav_profile) {
                Bundle args = new Bundle();
                args.putString("USER_ROLE", userRole);
                args.putString("USER_ID", userId);
                ProfileFragment profileFrag = new ProfileFragment();
                profileFrag.setArguments(args);
                selected = profileFrag;
            }
            if (selected != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selected)
                        .commit();
            }
            return true;
        });

        if (fabAi != null) {
            fabAi.setOnClickListener(v -> {
                fabAi.hide();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new AiFragment())
                        .commit();
            });
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }
    }
}
