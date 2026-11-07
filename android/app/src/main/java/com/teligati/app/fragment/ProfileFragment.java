package com.teligati.app.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.teligati.app.R;
import com.teligati.app.adapter.MediaStoreImageAdapter;
import com.teligati.app.adapter.TransactionAdapter;
import com.teligati.app.auth.AuthGatewayActivity;
import com.teligati.app.data.DataMapper;
import com.teligati.app.data.SessionManager;
import com.teligati.app.data.entity.AlumniProfileEntity;
import com.teligati.app.data.entity.StudentProfileEntity;
import com.teligati.app.data.entity.TeacherProfileEntity;
import com.teligati.app.data.repository.AppRepository;
import com.teligati.app.data.repository.AuthRepository;
import com.teligati.app.data.repository.ProfileRepository;
import com.teligati.app.mediastore.MediaImageItem;
import com.teligati.app.mediastore.MediaStoreHelper;
import com.teligati.app.network.model.ProfileResponse;
import com.teligati.app.network.model.ProfileUpdateRequest;

public class ProfileFragment extends Fragment {
    private double availableBalance = 18500.0;
    private double escrowBalance = 3500.0;
    private AppRepository repository;
    private ProfileRepository profileRepository;

    private TextView tvName;
    private TextView tvMeta;
    private TextView tvAvailable;
    private TextView tvEscrow;
    private ImageView ivProfileAvatar;

    private ActivityResultLauncher<String> mediaPermissionLauncher;

    private String userRole = "STUDENT";
    private String currentUserId = "2307003";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mediaPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openMediaStorePickerDialog();
                    } else {
                        Toast.makeText(getContext(), "Permission required to access MediaStore images", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        repository = AppRepository.getInstance(requireContext());
        profileRepository = ProfileRepository.getInstance(requireContext());

        SessionManager session = SessionManager.getInstance(requireContext());
        userRole = session.getUserRole();
        currentUserId = session.getUserId();

        if (getArguments() != null) {
            if (getArguments().getString("USER_ROLE") != null) userRole = getArguments().getString("USER_ROLE");
            if (getArguments().getString("USER_ID") != null) currentUserId = getArguments().getString("USER_ID");
        }

        setupProfile(view);
        return view;
    }

    private void setupProfile(View view) {
        tvName = view.findViewById(R.id.tvProfileUserName);
        tvMeta = view.findViewById(R.id.tvProfileUserMeta);
        tvAvailable = view.findViewById(R.id.tvAvailableBalance);
        tvEscrow = view.findViewById(R.id.tvEscrowBalance);
        ivProfileAvatar = view.findViewById(R.id.ivProfileAvatar);
        Button btnSelectPhoto = view.findViewById(R.id.btnSelectProfileImage);
        Button btnEdit = view.findViewById(R.id.btnEditProfileDetails);
        Button btnExportResume = view.findViewById(R.id.btnExportResume);
        Button btnWithdraw = view.findViewById(R.id.btnWithdraw);
        Button btnReleaseEscrow = view.findViewById(R.id.btnReleaseEscrow);
        Button btnPostGig = view.findViewById(R.id.btnPostGig);
        RecyclerView rvTx = view.findViewById(R.id.rvTransactions);
        Button btnLogout = view.findViewById(R.id.btnLogout);

        if (btnSelectPhoto != null) {
            btnSelectPhoto.setOnClickListener(v -> checkPermissionAndOpenMediaStore());
        }
        if (ivProfileAvatar != null) {
            ivProfileAvatar.setOnClickListener(v -> checkPermissionAndOpenMediaStore());
        }

        refreshProfileHeader();

        // Load balance from Room first
        repository.getUser(currentUserId, user -> {
            if (user != null) {
                availableBalance = user.getAvailableBalance();
                escrowBalance = user.getEscrowBalance();
                if (tvAvailable != null) tvAvailable.setText("Available: ৳" + (int) availableBalance);
                if (tvEscrow != null) tvEscrow.setText("In Escrow: ৳" + (int) escrowBalance);
            }
        });

        // Sync with backend demo wallet
        repository.syncWallet(wallet -> {
            if (!isAdded()) return;
            if (wallet != null) {
                availableBalance = wallet.getAvailableBalance();
                escrowBalance = wallet.getEscrowBalance();
                if (tvAvailable != null) tvAvailable.setText("Available: ৳" + (int) availableBalance);
                if (tvEscrow != null) tvEscrow.setText("In Escrow: ৳" + (int) escrowBalance);
            }
        });

        if (btnEdit != null) {
            btnEdit.setOnClickListener(v -> showEditProfileDialog());
        }

        if (btnExportResume != null) {
            btnExportResume.setOnClickListener(v -> {
                if (getContext() == null) return;
                String summary = "🎓 KUET Verified Profile Summary\n" +
                        "Role: " + userRole + " • ID: " + currentUserId + "\n" +
                        "Affiliation: Khulna University of Engineering & Technology (KUET)\n" +
                        "Status: Verified on Wizards of Teligati";

                new AlertDialog.Builder(getContext())
                        .setTitle("📄 Verified Profile Summary")
                        .setMessage(summary)
                        .setPositiveButton("Share Profile (Implicit Intent)", (d, w) -> {
                            Intent sendIntent = new Intent(Intent.ACTION_SEND);
                            sendIntent.putExtra(Intent.EXTRA_SUBJECT, "KUET Profile: " + currentUserId);
                            sendIntent.putExtra(Intent.EXTRA_TEXT, summary);
                            sendIntent.setType("text/plain");
                            startActivity(Intent.createChooser(sendIntent, "Share Profile via"));
                        })
                        .setNegativeButton("Close", null)
                        .show();
            });
        }

        if (btnWithdraw != null) {
            btnWithdraw.setOnClickListener(v -> {
                if (getContext() == null) return;
                new AlertDialog.Builder(getContext())
                        .setTitle("Withdraw Demo Earnings")
                        .setMessage("DEMO WALLET — NO REAL MONEY INVOLVED\n"
                                + "DEMO MODE — All balances and transactions are simulated. No real money is transferred.\n\n"
                                + "Available simulated balance: ৳" + (int) availableBalance + "\n\n"
                                + "Simulate withdrawal of ৳1,000 to demo student bank account?")
                        .setPositiveButton("Simulate Withdrawal", (d, w) -> {
                            repository.simulateWithdraw(1000.0, "SIMULATED_STUDENT_BANK", "Demo earnings withdrawal", res -> {
                                if (!isAdded()) return;
                                if (res != null) {
                                    availableBalance = res.getAvailableBalance();
                                    escrowBalance = res.getEscrowBalance();
                                    if (tvAvailable != null) tvAvailable.setText("Available: ৳" + (int) availableBalance);
                                    if (tvEscrow != null) tvEscrow.setText("In Escrow: ৳" + (int) escrowBalance);
                                    Toast.makeText(getContext(), "Simulated withdrawal successful! [Demo Only]", Toast.LENGTH_SHORT).show();
                                    // Refresh transaction list
                                    repository.syncTransactions(txs -> {
                                        if (!isAdded()) return;
                                        repository.getStudentHistory(currentUserId, list -> {
                                            if (rvTx != null) rvTx.setAdapter(new TransactionAdapter(DataMapper.toTransactionRecordList(list)));
                                        });
                                    });
                                } else {
                                    Toast.makeText(getContext(), "Withdrawal failed: insufficient demo balance", Toast.LENGTH_SHORT).show();
                                }
                            });
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        if (btnReleaseEscrow != null) {
            btnReleaseEscrow.setOnClickListener(v -> {
                if (getContext() == null) return;
                if (escrowBalance > 0) {
                    new AlertDialog.Builder(getContext())
                            .setTitle("Release Simulated Escrow")
                            .setMessage("DEMO WALLET — NO REAL MONEY INVOLVED\n"
                                    + "DEMO MODE — All balances and transactions are simulated.\n\n"
                                    + "Current locked escrow: ৳" + (int) escrowBalance + "\n\n"
                                    + "Confirm release of simulated escrow to available balance?")
                            .setPositiveButton("Confirm Release", (d, w) -> {
                                availableBalance += escrowBalance;
                                escrowBalance = 0;
                                if (tvAvailable != null) tvAvailable.setText("Available: ৳" + (int) availableBalance);
                                if (tvEscrow != null) tvEscrow.setText("In Escrow: ৳" + (int) escrowBalance);
                                repository.updateUserBalance(currentUserId, availableBalance, escrowBalance, success -> {
                                    Toast.makeText(getContext(), "Simulated escrow payment released! [Demo Only]", Toast.LENGTH_SHORT).show();
                                });
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                } else {
                    Toast.makeText(getContext(), "No pending escrow payments in demo wallet.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (rvTx != null) {
            rvTx.setLayoutManager(new LinearLayoutManager(getContext()));
            // First load from Room
            repository.getStudentHistory(currentUserId, list -> {
                if (rvTx != null) rvTx.setAdapter(new TransactionAdapter(DataMapper.toTransactionRecordList(list)));
            });
            // Then sync from backend
            repository.syncTransactions(txs -> {
                if (!isAdded()) return;
                repository.getStudentHistory(currentUserId, list -> {
                    if (rvTx != null) rvTx.setAdapter(new TransactionAdapter(DataMapper.toTransactionRecordList(list)));
                });
            });
        }

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                if (getActivity() == null) return;
                AuthRepository.getInstance(requireContext())
                        .logout(new AuthRepository.Callback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                if (getActivity() == null) return;
                                Intent intent = new Intent(getActivity(), AuthGatewayActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                getActivity().finish();
                            }

                            @Override
                            public void onError(String errorMessage) {
                                if (getActivity() == null) return;
                                Intent intent = new Intent(getActivity(), AuthGatewayActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                getActivity().finish();
                            }
                        });
            });
        }
    }

    private void refreshProfileHeader() {
        if (!isAdded()) return;
        SessionManager session = SessionManager.getInstance(requireContext());
        boolean isVerified = session.isVerified();
        String fallbackName = session.getUserName();
        String fallbackDept = session.getUserDept();

        // 1. Initial local Room load for zero-lag UI
        if ("ALUMNI".equalsIgnoreCase(userRole)) {
            repository.getAlumniProfile(currentUserId, profile -> {
                if (!isAdded()) return;
                if (profile != null) {
                    if (tvName != null) tvName.setText(profile.getName() + (profile.isVerified() || isVerified ? " ✓ Verified Alumni" : " • Alumni"));
                    String pos = profile.getCurrentPosition() != null ? profile.getCurrentPosition() : "Alumni";
                    String comp = (profile.getCurrentCompany() != null && !profile.getCurrentCompany().isBlank()) ? (" @ " + profile.getCurrentCompany()) : "";
                    String ctry = (profile.getCountry() != null && !profile.getCountry().isBlank()) ? (" • " + profile.getCountry()) : "";
                    if (tvMeta != null) tvMeta.setText(pos + comp + ctry);
                } else {
                    if (tvName != null) tvName.setText(fallbackName + (isVerified ? " ✓ Verified Alumni" : " • Alumni"));
                    if (tvMeta != null) tvMeta.setText(fallbackDept + " • " + currentUserId);
                }
            });
        } else if ("FACULTY".equalsIgnoreCase(userRole) || "TEACHER".equalsIgnoreCase(userRole)) {
            repository.getTeacherProfile(currentUserId, profile -> {
                if (!isAdded()) return;
                if (profile != null) {
                    if (tvName != null) tvName.setText(profile.getName() + " ✓ Faculty");
                    String desig = profile.getDesignation() != null ? profile.getDesignation() : "Faculty Member";
                    String lab = (profile.getLabName() != null && !profile.getLabName().isBlank()) ? (" • " + profile.getLabName()) : "";
                    String dept = (profile.getDepartment() != null && !profile.getDepartment().isBlank()) ? (" • " + profile.getDepartment()) : "";
                    if (tvMeta != null) tvMeta.setText(desig + lab + dept);
                } else {
                    if (tvName != null) tvName.setText(fallbackName + " ✓ Faculty");
                    if (tvMeta != null) tvMeta.setText(fallbackDept + " • " + currentUserId);
                }
            });
        } else {
            repository.getStudentProfile(currentUserId, profile -> {
                if (!isAdded()) return;
                if (profile != null) {
                    if (tvName != null) tvName.setText(profile.getName() + (isVerified ? " ✓ Verified Student" : " ⏳ Verification Pending"));
                    String roll = profile.getRoll() != null ? profile.getRoll() : currentUserId;
                    String dept = profile.getDepartment() != null ? profile.getDepartment() : fallbackDept;
                    if (tvMeta != null) tvMeta.setText("Roll " + roll + " • " + dept + " • CGPA " + profile.getCgpa());
                } else {
                    if (tvName != null) tvName.setText(fallbackName + (isVerified ? " ✓ Verified Student" : " ⏳ Verification Pending"));
                    if (tvMeta != null) tvMeta.setText("Roll " + currentUserId + " • " + fallbackDept);
                }
            });
        }

        // 2. Authoritative remote sync via ProfileRepository (Firestore source of truth)
        if (session.isLoggedIn()) {
            profileRepository.fetchMyProfile(new ProfileRepository.Callback<ProfileResponse>() {
                @Override
                public void onSuccess(ProfileResponse p) {
                    if (!isAdded() || p == null) return;
                    boolean v = p.isVerified();
                    if ("ALUMNI".equalsIgnoreCase(userRole) || "ROLE_ALUMNI".equalsIgnoreCase(userRole)) {
                        if (tvName != null) tvName.setText(p.getName() + (v ? " ✓ Verified Alumni" : " • Alumni"));
                        String pos = p.getCurrentPosition() != null ? p.getCurrentPosition() : "Alumni";
                        String comp = (p.getCurrentCompany() != null && !p.getCurrentCompany().isBlank()) ? (" @ " + p.getCurrentCompany()) : "";
                        String ctry = (p.getCountry() != null && !p.getCountry().isBlank()) ? (" • " + p.getCountry()) : "";
                        if (tvMeta != null) tvMeta.setText(pos + comp + ctry);
                    } else if ("FACULTY".equalsIgnoreCase(userRole) || "TEACHER".equalsIgnoreCase(userRole) || "ROLE_TEACHER".equalsIgnoreCase(userRole)) {
                        if (tvName != null) tvName.setText(p.getName() + (v ? " ✓ Verified Faculty" : " • Faculty"));
                        String desig = p.getDesignation() != null ? p.getDesignation() : "Faculty Member";
                        String lab = (p.getLabName() != null && !p.getLabName().isBlank()) ? (" • " + p.getLabName()) : "";
                        String dept = (p.getDepartment() != null && !p.getDepartment().isBlank()) ? (" • " + p.getDepartment()) : "";
                        if (tvMeta != null) tvMeta.setText(desig + lab + dept);
                    } else {
                        if (tvName != null) tvName.setText(p.getName() + (v ? " ✓ Verified Student" : " ⏳ Verification Pending"));
                        String r = p.getKuetId() != null ? p.getKuetId() : currentUserId;
                        String dept = p.getDepartment() != null ? p.getDepartment() : fallbackDept;
                        String cgpaStr = p.getCgpa() != null ? (" • CGPA " + p.getCgpa()) : "";
                        if (tvMeta != null) tvMeta.setText("Roll " + r + " • " + dept + cgpaStr);
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    // Fail silently to preserved Room cache
                }
            });
        }
    }

    private void showEditProfileDialog() {
        if (getContext() == null) return;

        if ("ALUMNI".equalsIgnoreCase(userRole) || "ROLE_ALUMNI".equalsIgnoreCase(userRole)) {
            repository.getAlumniProfile(currentUserId, profile -> {
                if (profile == null) return;
                ScrollView scrollView = new ScrollView(requireContext());
                LinearLayout layout = new LinearLayout(requireContext());
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(40, 24, 40, 16);
                scrollView.addView(layout);

                final EditText etName = createInput("Full Name", profile.getName());
                final EditText etCompany = createInput("Current Company", profile.getCurrentCompany());
                final EditText etPosition = createInput("Current Position", profile.getCurrentPosition());
                final EditText etCountry = createInput("Country", profile.getCountry());
                final EditText etCity = createInput("City", profile.getCity());
                final EditText etSkills = createInput("Skills", profile.getSkills());
                final EditText etBio = createInput("Bio & Mentorship Domains", profile.getBio());

                layout.addView(etName);
                layout.addView(etCompany);
                layout.addView(etPosition);
                layout.addView(etCountry);
                layout.addView(etCity);
                layout.addView(etSkills);
                layout.addView(etBio);

                new AlertDialog.Builder(requireContext())
                        .setTitle("Edit Alumni Profile")
                        .setView(scrollView)
                        .setPositiveButton("Save", (d, w) -> {
                            ProfileUpdateRequest updateReq = new ProfileUpdateRequest();
                            updateReq.setName(etName.getText().toString().trim());
                            updateReq.setCurrentCompany(etCompany.getText().toString().trim());
                            updateReq.setCurrentPosition(etPosition.getText().toString().trim());
                            updateReq.setCountry(etCountry.getText().toString().trim());
                            updateReq.setCity(etCity.getText().toString().trim());
                            updateReq.setSkills(etSkills.getText().toString().trim());
                            updateReq.setBio(etBio.getText().toString().trim());

                            // Save to backend and Room
                            profileRepository.updateMyProfile(updateReq, new ProfileRepository.Callback<ProfileResponse>() {
                                @Override
                                public void onSuccess(ProfileResponse result) {
                                    if (!isAdded()) return;
                                    refreshProfileHeader();
                                    Toast.makeText(requireContext(), "Alumni profile updated & synchronized with backend!", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onError(String errorMessage) {
                                    if (!isAdded()) return;
                                    // Fallback local update
                                    profile.setName(updateReq.getName());
                                    profile.setCurrentCompany(updateReq.getCurrentCompany());
                                    profile.setCurrentPosition(updateReq.getCurrentPosition());
                                    profile.setCountry(updateReq.getCountry());
                                    profile.setCity(updateReq.getCity());
                                    profile.setSkills(updateReq.getSkills());
                                    profile.setBio(updateReq.getBio());
                                    repository.updateAlumniProfile(profile, s -> {
                                        refreshProfileHeader();
                                        Toast.makeText(requireContext(), "Saved locally (Network: " + errorMessage + ")", Toast.LENGTH_LONG).show();
                                    });
                                }
                            });
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        } else if ("FACULTY".equalsIgnoreCase(userRole) || "TEACHER".equalsIgnoreCase(userRole) || "ROLE_TEACHER".equalsIgnoreCase(userRole)) {
            repository.getTeacherProfile(currentUserId, profile -> {
                if (profile == null) return;
                ScrollView scrollView = new ScrollView(requireContext());
                LinearLayout layout = new LinearLayout(requireContext());
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(40, 24, 40, 16);
                scrollView.addView(layout);

                final EditText etName = createInput("Faculty Name", profile.getName());
                final EditText etDesig = createInput("Designation", profile.getDesignation());
                final EditText etDept = createInput("Department", profile.getDepartment());
                final EditText etLab = createInput("Lab Name", profile.getLabName());
                final EditText etFocus = createInput("Research Focus Areas", profile.getResearchFocus());
                final EditText etBio = createInput("Academic Bio & Grants", profile.getBio());

                layout.addView(etName);
                layout.addView(etDesig);
                layout.addView(etDept);
                layout.addView(etLab);
                layout.addView(etFocus);
                layout.addView(etBio);

                new AlertDialog.Builder(requireContext())
                        .setTitle("Edit Faculty Profile")
                        .setView(scrollView)
                        .setPositiveButton("Save", (d, w) -> {
                            ProfileUpdateRequest updateReq = new ProfileUpdateRequest();
                            updateReq.setName(etName.getText().toString().trim());
                            updateReq.setDesignation(etDesig.getText().toString().trim());
                            updateReq.setDepartment(etDept.getText().toString().trim());
                            updateReq.setLabName(etLab.getText().toString().trim());
                            updateReq.setResearchFocus(etFocus.getText().toString().trim());
                            updateReq.setBio(etBio.getText().toString().trim());

                            profileRepository.updateMyProfile(updateReq, new ProfileRepository.Callback<ProfileResponse>() {
                                @Override
                                public void onSuccess(ProfileResponse result) {
                                    if (!isAdded()) return;
                                    refreshProfileHeader();
                                    Toast.makeText(requireContext(), "Faculty profile updated & synchronized with backend!", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onError(String errorMessage) {
                                    if (!isAdded()) return;
                                    profile.setName(updateReq.getName());
                                    profile.setDesignation(updateReq.getDesignation());
                                    profile.setDepartment(updateReq.getDepartment());
                                    profile.setLabName(updateReq.getLabName());
                                    profile.setResearchFocus(updateReq.getResearchFocus());
                                    profile.setBio(updateReq.getBio());
                                    repository.updateTeacherProfile(profile, s -> {
                                        refreshProfileHeader();
                                        Toast.makeText(requireContext(), "Saved locally (Network: " + errorMessage + ")", Toast.LENGTH_LONG).show();
                                    });
                                }
                            });
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        } else {
            // Student
            repository.getStudentProfile(currentUserId, profile -> {
                if (profile == null) return;
                final EditText etSkills = new EditText(requireContext());
                etSkills.setHint("Update Skills");
                etSkills.setText(profile.getSkills());
                etSkills.setPadding(32, 24, 32, 24);

                new AlertDialog.Builder(requireContext())
                        .setTitle("Update Student Skills")
                        .setView(etSkills)
                        .setPositiveButton("Save", (d, w) -> {
                            String newSkills = etSkills.getText().toString().trim();
                            ProfileUpdateRequest updateReq = new ProfileUpdateRequest();
                            updateReq.setSkills(newSkills);

                            profileRepository.updateMyProfile(updateReq, new ProfileRepository.Callback<ProfileResponse>() {
                                @Override
                                public void onSuccess(ProfileResponse result) {
                                    if (!isAdded()) return;
                                    refreshProfileHeader();
                                    Toast.makeText(requireContext(), "Profile updated & synchronized with backend!", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onError(String errorMessage) {
                                    if (!isAdded()) return;
                                    profile.setSkills(newSkills);
                                    repository.updateStudentProfile(profile, success -> {
                                        refreshProfileHeader();
                                        Toast.makeText(requireContext(), "Saved locally (Network: " + errorMessage + ")", Toast.LENGTH_LONG).show();
                                    });
                                }
                            });
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }
    }

    private void checkPermissionAndOpenMediaStore() {
        if (getContext() == null) return;
        if (MediaStoreHelper.hasMediaPermission(requireContext())) {
            openMediaStorePickerDialog();
        } else {
            mediaPermissionLauncher.launch(MediaStoreHelper.getRequiredMediaPermission());
        }
    }

    private void openMediaStorePickerDialog() {
        if (getContext() == null) return;

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_mediastore_picker, null);
        RecyclerView rvMedia = dialogView.findViewById(R.id.rvMediaStoreImages);
        View layoutEmpty = dialogView.findViewById(R.id.layoutEmptyMedia);
        Button btnGenerate = dialogView.findViewById(R.id.btnGenerateSampleImage);
        Button btnClose = dialogView.findViewById(R.id.btnCloseMediaPicker);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        rvMedia.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        MediaStoreImageAdapter adapter = new MediaStoreImageAdapter(requireContext(), item -> {
            // Selected image for profile picture preview (Local preview only, no automatic upload)
            new Thread(() -> {
                android.graphics.Bitmap fullThumb = MediaStoreHelper.loadThumbnail(requireContext(), item.getUri(), 300, 300);
                if (getActivity() != null && fullThumb != null) {
                    getActivity().runOnUiThread(() -> {
                        if (ivProfileAvatar != null) {
                            ivProfileAvatar.setImageBitmap(fullThumb);
                        }
                        Toast.makeText(requireContext(), "Profile picture preview set via MediaStore (" + item.getDisplayName() + ")", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
                }
            }).start();
        });
        rvMedia.setAdapter(adapter);

        Runnable refreshImages = () -> {
            new Thread(() -> {
                java.util.List<MediaImageItem> images = MediaStoreHelper.fetchDeviceImages(requireContext(), 30);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (images.isEmpty()) {
                            layoutEmpty.setVisibility(View.VISIBLE);
                            rvMedia.setVisibility(View.GONE);
                        } else {
                            layoutEmpty.setVisibility(View.GONE);
                            rvMedia.setVisibility(View.VISIBLE);
                            adapter.setItems(images);
                        }
                    });
                }
            }).start();
        };

        if (btnGenerate != null) {
            btnGenerate.setOnClickListener(v -> {
                MediaStoreHelper.insertSampleTestImage(requireContext(), "KUETian");
                Toast.makeText(requireContext(), "Sample image inserted to MediaStore!", Toast.LENGTH_SHORT).show();
                refreshImages.run();
            });
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        refreshImages.run();
        dialog.show();
    }

    private EditText createInput(String hint, String value) {
        EditText et = new EditText(requireContext());
        et.setHint(hint);
        if (value != null) et.setText(value);
        et.setPadding(24, 20, 24, 20);
        return et;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshProfileHeader();
    }
}
