package com.teligati.app;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.teligati.app.fragment.*;

public class MainActivity extends AppCompatActivity {
    private String userRole = "STUDENT";
    private String userId = "2307003";

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

        FloatingActionButton fabAi = findViewById(R.id.fabAiWizard);
        if (fabAi != null) {
            fabAi.setOnClickListener(v -> {
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
