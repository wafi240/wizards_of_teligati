package com.teligati.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.teligati.app.auth.AuthGatewayActivity;
import com.teligati.app.fragment.*;

public class MainActivity extends AppCompatActivity {
    private String userRole = "STUDENT";
    private String userId = "2307003";

    private DrawerLayout drawerLayout;
    private MaterialToolbar toolbar;
    private BottomNavigationView bottomNav;
    private NavigationView navView;
    private FloatingActionButton fabAi;
    private int currentMenuId = R.id.nav_dashboard;
    private boolean isSyncingNavigation = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        com.teligati.app.data.SessionManager session = com.teligati.app.data.SessionManager.getInstance(this);
        String intentRole = getIntent() != null ? getIntent().getStringExtra("USER_ROLE") : null;
        String intentId = getIntent() != null ? getIntent().getStringExtra("USER_ID") : null;

        if (intentRole != null && !intentRole.trim().isEmpty()) {
            userRole = com.teligati.app.data.SessionManager.normalizeRole(intentRole);
            if (intentId != null && !intentId.trim().isEmpty()) {
                userId = intentId.trim();
            }
            if (!userRole.equalsIgnoreCase(session.getUserRole()) || !userId.equalsIgnoreCase(session.getUserId())) {
                session.setSession(userRole, userId);
            }
        } else if (session.isLoggedIn()) {
            userRole = session.getUserRole();
            userId = session.getUserId();
        } else {
            session.setSession(userRole, userId);
        }

        drawerLayout = findViewById(R.id.drawer_layout);
        toolbar = findViewById(R.id.toolbar);
        bottomNav = findViewById(R.id.bottom_navigation);
        navView = findViewById(R.id.nav_view);
        fabAi = findViewById(R.id.fabAiWizard);

        setupToolbar();
        setupNavigationDrawer();
        setupBottomNavigation();
        setupFabAi();
        setupBackNavigation();

        // Ensure Room database is initialized and seeded once
        com.teligati.app.data.repository.AppRepository.getInstance(this).ensureSeeded(v -> {});

        // Refresh verification status from backend if authenticated
        if (session.isLoggedIn() && session.getAccessToken() != null) {
            com.teligati.app.data.repository.AuthRepository.getInstance(this)
                    .getVerificationStatus(new com.teligati.app.data.repository.AuthRepository.Callback<com.teligati.app.network.model.VerificationStatusResponse>() {
                        @Override
                        public void onSuccess(com.teligati.app.network.model.VerificationStatusResponse result) {
                            setupNavigationDrawer();
                        }

                        @Override
                        public void onError(String errorMessage) {}
                    });
        }

        if (savedInstanceState == null) {
            loadInitialDashboard();
        }
    }

    private void setupToolbar() {
        if (toolbar == null) return;

        // Set role-based subtitle
        if (isAlumni()) {
            toolbar.setSubtitle("Alumni Portal");
        } else if (isTeacher()) {
            toolbar.setSubtitle("Faculty Portal");
        } else {
            toolbar.setSubtitle("Student Portal");
        }

        // Hamburger drawer toggle
        toolbar.setNavigationOnClickListener(v -> {
            if (drawerLayout != null) {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            }
        });

        // Top action buttons (Notifications & Messages)
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_notifications) {
                navigateTo(R.id.nav_notifications, true);
                return true;
            } else if (id == R.id.action_messages) {
                navigateTo(R.id.nav_messages, true);
                return true;
            }
            return false;
        });
    }

    private void setupNavigationDrawer() {
        if (navView == null) return;

        // Inflate complete role-specific sidebar navigation
        navView.getMenu().clear();
        if (isAlumni()) {
            navView.inflateMenu(R.menu.menu_alumni);
        } else if (isTeacher()) {
            navView.inflateMenu(R.menu.menu_teacher);
        } else {
            navView.inflateMenu(R.menu.menu_student);
        }

        // Setup Header View
        if (navView.getHeaderCount() > 0) {
            android.view.View header = navView.getHeaderView(0);
            TextView tvName = header.findViewById(R.id.tvNavHeaderName);
            TextView tvRole = header.findViewById(R.id.tvNavHeaderRole);
            com.teligati.app.data.SessionManager sm = com.teligati.app.data.SessionManager.getInstance(this);
            String name = sm.getUserName();
            String dept = sm.getUserDept();
            boolean isVerified = sm.isVerified();

            if (tvName != null) tvName.setText(name);
            if (tvRole != null) {
                if (isAlumni()) {
                    tvRole.setText((isVerified ? "Verified Alumni" : "Alumni") + " • " + dept + " (" + userId + ")");
                } else if (isTeacher()) {
                    tvRole.setText((isVerified ? "Verified Faculty" : "Faculty") + " • " + dept + " (" + userId + ")");
                } else {
                    tvRole.setText((isVerified ? "Verified Student" : "Verification Pending") + " • Roll " + userId + " • " + dept);
                }
            }
        }

        navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (drawerLayout != null) {
                drawerLayout.closeDrawer(GravityCompat.START);
            }
            navigateTo(id, true);
            return true;
        });
    }

    private void setupBottomNavigation() {
        if (bottomNav == null) return;

        // Inflate 5-item role-specific bottom menu to prevent 5-item constraint crashes
        bottomNav.getMenu().clear();
        if (isAlumni()) {
            bottomNav.inflateMenu(R.menu.bottom_nav_alumni);
        } else if (isTeacher()) {
            bottomNav.inflateMenu(R.menu.bottom_nav_teacher);
        } else {
            bottomNav.inflateMenu(R.menu.bottom_nav_student);
        }

        bottomNav.setOnItemSelectedListener(item -> {
            if (isSyncingNavigation) return true;
            navigateTo(item.getItemId(), false);
            return true;
        });
    }

    private void setupFabAi() {
        if (fabAi == null) return;

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

        fabAi.setOnClickListener(v -> {
            fabAi.hide();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new AiFragment())
                    .commit();
        });
    }

    private void setupBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else if (currentMenuId != R.id.nav_dashboard) {
                    // Navigate back to the home role-specific dashboard instead of exiting
                    navigateTo(R.id.nav_dashboard, true);
                } else {
                    finish();
                }
            }
        });
    }

    private void loadInitialDashboard() {
        navigateTo(R.id.nav_dashboard, true);
    }

    public void navigateTo(int itemId, boolean syncBottomNav) {
        if (!isAuthorized(itemId)) {
            Toast.makeText(this, "Access Denied: Section restricted to authorized role", Toast.LENGTH_SHORT).show();
            return;
        }

        Fragment selected = createFragmentForMenuId(itemId);
        if (selected != null) {
            currentMenuId = itemId;
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, selected)
                    .commit();

            // Synchronize selection state in drawer
            if (navView != null) {
                MenuItem navItem = navView.getMenu().findItem(itemId);
                if (navItem != null) {
                    navItem.setChecked(true);
                }
            }

            // Synchronize selection state in bottom navigation if item exists there
            if (syncBottomNav && bottomNav != null) {
                MenuItem btmItem = bottomNav.getMenu().findItem(itemId);
                if (btmItem != null) {
                    isSyncingNavigation = true;
                    bottomNav.setSelectedItemId(itemId);
                    isSyncingNavigation = false;
                }
            }
        }
    }

    private boolean isAuthorized(int itemId) {
        // Enforce RBAC navigation permissions
        if (itemId == R.id.nav_professional_jobs || itemId == R.id.nav_student_applicants) {
            return isAlumni();
        }
        if (itemId == R.id.nav_research_opportunities || itemId == R.id.nav_proposals || itemId == R.id.nav_student_researchers) {
            return isTeacher();
        }
        if (itemId == R.id.nav_reference_opps || itemId == R.id.nav_alumni_dir || itemId == R.id.nav_portfolio || itemId == R.id.nav_history) {
            return isStudent();
        }
        return true;
    }

    private Fragment createFragmentForMenuId(int itemId) {
        Bundle roleArgs = new Bundle();
        roleArgs.putString("USER_ROLE", userRole);
        roleArgs.putString("USER_ID", userId);

        if (itemId == R.id.nav_dashboard) {
            if (isAlumni()) {
                AlumniDashboardFragment f = new AlumniDashboardFragment();
                f.setArguments(roleArgs);
                return f;
            } else if (isTeacher()) {
                TeacherDashboardFragment f = new TeacherDashboardFragment();
                f.setArguments(roleArgs);
                return f;
            } else {
                StudentDashboardFragment f = new StudentDashboardFragment();
                f.setArguments(roleArgs);
                return f;
            }
        } else if (itemId == R.id.nav_assistant_jobs) {
            if (isAlumni()) {
                return new AlumniAssistantJobsFragment();
            } else {
                return new StudentAssistantJobsFragment();
            }
        } else if (itemId == R.id.nav_research_papers) {
            if (isTeacher()) {
                return new TeacherResearchPapersFragment();
            } else {
                return new StudentResearchPapersFragment();
            }
        } else if (itemId == R.id.nav_reference_opps) {
            return new StudentReferenceOpportunitiesFragment();
        } else if (itemId == R.id.nav_alumni_dir) {
            return new StudentAlumniDirectoryFragment();
        } else if (itemId == R.id.nav_portfolio) {
            return new StudentPortfolioFragment();
        } else if (itemId == R.id.nav_history) {
            return new StudentHistoryFragment();
        } else if (itemId == R.id.nav_professional_jobs) {
            return new AlumniProfessionalJobsFragment();
        } else if (itemId == R.id.nav_reference_requests) {
            return new AlumniReferenceRequestsFragment();
        } else if (itemId == R.id.nav_student_applicants) {
            return new AlumniStudentApplicantsFragment();
        } else if (itemId == R.id.nav_research_opportunities) {
            return new TeacherResearchOpportunitiesFragment();
        } else if (itemId == R.id.nav_proposals) {
            return new TeacherProposalsFragment();
        } else if (itemId == R.id.nav_student_researchers) {
            return new TeacherStudentResearchersFragment();
        } else if (itemId == R.id.nav_profile) {
            ProfileFragment f = new ProfileFragment();
            f.setArguments(roleArgs);
            return f;
        } else if (itemId == R.id.nav_messages) {
            return new MessagesFragment();
        } else if (itemId == R.id.nav_notifications) {
            return new NotificationsFragment();
        } else if (itemId == R.id.nav_settings) {
            return new SettingsFragment();
        }
        return null;
    }

    public String getUserRole() {
        return userRole;
    }

    public String getUserId() {
        return userId;
    }

    private boolean isAlumni() {
        return "ALUMNI".equalsIgnoreCase(userRole);
    }

    private boolean isTeacher() {
        return "TEACHER".equalsIgnoreCase(userRole) || "FACULTY".equalsIgnoreCase(userRole);
    }

    private boolean isStudent() {
        return "STUDENT".equalsIgnoreCase(userRole);
    }
}
