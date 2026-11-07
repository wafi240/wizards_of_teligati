package com.teligati.app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.teligati.app.data.SessionManager;
import com.teligati.app.data.entity.JobApplicationEntity;
import com.teligati.app.data.repository.AppRepository;
import com.teligati.app.databinding.ActivityOpportunityDetailBinding;
import com.teligati.app.model.Opportunity;

/**
 * OpportunityDetailActivity demonstrates core Android Architecture principles:
 * 1. Multi-Activity navigation (secondary screen launched from dashboard / listings)
 * 2. Explicit Intent navigation with typed extras
 * 3. Implicit Intent actions (ACTION_SEND with chooser, and ACTION_VIEW browser)
 * 4. View Binding (ActivityOpportunityDetailBinding)
 * 5. Button event handling and asynchronous Room callbacks
 */
public class OpportunityDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "EXTRA_ID";
    public static final String EXTRA_TITLE = "EXTRA_TITLE";
    public static final String EXTRA_ORG = "EXTRA_ORG";
    public static final String EXTRA_LOCATION = "EXTRA_LOCATION";
    public static final String EXTRA_COMPENSATION = "EXTRA_COMPENSATION";
    public static final String EXTRA_DURATION = "EXTRA_DURATION";
    public static final String EXTRA_DESC = "EXTRA_DESC";
    public static final String EXTRA_CATEGORY = "EXTRA_CATEGORY";
    public static final String EXTRA_SKILLS = "EXTRA_SKILLS";
    public static final String EXTRA_APPLIED = "EXTRA_APPLIED";

    private ActivityOpportunityDetailBinding binding;
    private long oppId;
    private String title;
    private String org;
    private String compensation;
    private String skills;
    private boolean isApplied;

    public static void start(Context context, Opportunity opp) {
        Intent intent = new Intent(context, OpportunityDetailActivity.class);
        intent.putExtra(EXTRA_ID, opp.getId());
        intent.putExtra(EXTRA_TITLE, opp.getTitle());
        intent.putExtra(EXTRA_ORG, opp.getOrganization());
        intent.putExtra(EXTRA_LOCATION, opp.getLocation());
        intent.putExtra(EXTRA_COMPENSATION, opp.getCompensation());
        intent.putExtra(EXTRA_DURATION, opp.getDuration());
        intent.putExtra(EXTRA_DESC, opp.getDescription());
        intent.putExtra(EXTRA_CATEGORY, opp.getCategory() != null ? opp.getCategory().name() : "ASSISTANT_WORK");
        intent.putExtra(EXTRA_SKILLS, opp.getSkillsRequired() != null ? String.join(", ", opp.getSkillsRequired()) : "General");
        intent.putExtra(EXTRA_APPLIED, opp.isApplied());
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // View Binding demonstration
        binding = ActivityOpportunityDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Extract explicit intent extras
        oppId = getIntent().getLongExtra(EXTRA_ID, 0);
        title = getIntent().getStringExtra(EXTRA_TITLE);
        org = getIntent().getStringExtra(EXTRA_ORG);
        String location = getIntent().getStringExtra(EXTRA_LOCATION);
        compensation = getIntent().getStringExtra(EXTRA_COMPENSATION);
        String duration = getIntent().getStringExtra(EXTRA_DURATION);
        String desc = getIntent().getStringExtra(EXTRA_DESC);
        String category = getIntent().getStringExtra(EXTRA_CATEGORY);
        skills = getIntent().getStringExtra(EXTRA_SKILLS);
        isApplied = getIntent().getBooleanExtra(EXTRA_APPLIED, false);

        // Bind data to views via View Binding
        if (title != null) binding.tvDetailTitle.setText(title);
        if (org != null) binding.tvDetailOrganization.setText(org);
        if (location != null) binding.tvDetailLocation.setText("📍 " + location);
        if (compensation != null) binding.tvDetailCompensation.setText(compensation);
        if (duration != null) binding.tvDetailDuration.setText(duration);
        if (desc != null) binding.tvDetailDescription.setText(desc);
        if (category != null) binding.tvDetailCategory.setText(category.replace("_", " "));
        if (skills != null) binding.tvDetailSkills.setText(skills);

        setupToolbar();
        setupEventHandlers();
        updateApplyButtonState();
    }

    private void setupToolbar() {
        binding.toolbarDetail.setNavigationOnClickListener(v -> finish());
    }

    private void setupEventHandlers() {
        // Event Handler 1: Apply Button
        binding.btnDetailApply.setOnClickListener(v -> handleApplyAction());

        // Event Handler 2: Implicit Intent (ACTION_SEND with chooser)
        binding.btnDetailShare.setOnClickListener(v -> {
            String shareBody = "🎓 Wizards of Teligati Opportunity\n\n" +
                    "Role: " + title + "\n" +
                    "Organization: " + org + "\n" +
                    "Compensation: " + compensation + "\n" +
                    "Skills: " + skills + "\n\n" +
                    "KUET Student & Alumni Professional Network";

            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, "KUET Opportunity: " + title);
            sendIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
            sendIntent.setType("text/plain");

            Intent chooserIntent = Intent.createChooser(sendIntent, "Share KUET Opportunity via");
            startActivity(chooserIntent);
        });

        // Event Handler 3: Implicit Intent (ACTION_VIEW - Open web browser)
        binding.btnDetailVisitWebsite.setOnClickListener(v -> {
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.kuet.ac.bd"));
            startActivity(webIntent);
        });
    }

    private void handleApplyAction() {
        if (isApplied) {
            Toast.makeText(this, "You have already applied for this role.", Toast.LENGTH_SHORT).show();
            return;
        }

        SessionManager session = SessionManager.getInstance(this);
        String applicantRoll = session.getUserId();
        String applicantName = session.getUserName();

        new AlertDialog.Builder(this)
                .setTitle("Submit Application")
                .setMessage("Submit your verified KUET profile for " + title + " (" + compensation + ")?")
                .setPositiveButton("Submit Application", (dialog, which) -> {
                    AppRepository repo = AppRepository.getInstance(this);
                    repo.applyForJob(oppId, applicantRoll, applicantName, session.getUserDept(),
                            3.84, skills != null ? skills : "General",
                            "Application submitted via detail portal.",
                            success -> {
                                runOnUiThread(() -> {
                                    isApplied = Boolean.TRUE.equals(success);
                                    updateApplyButtonState();
                                    Toast.makeText(this, "Application successfully submitted!", Toast.LENGTH_SHORT).show();
                                });
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateApplyButtonState() {
        if (isApplied) {
            binding.btnDetailApply.setText("Application Submitted ✓");
            binding.btnDetailApply.setEnabled(false);
            binding.btnDetailApply.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, R.color.indigo_dark));
        } else {
            binding.btnDetailApply.setText("Apply for this Role");
            binding.btnDetailApply.setEnabled(true);
        }
    }
}
