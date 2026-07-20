package com.example.fonoss.ui.home;

import com.example.fonoss.R;
import dagger.hilt.android.AndroidEntryPoint;

import com.example.fonoss.ui.auth.UserViewModel;

import android.os.Bundle;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@AndroidEntryPoint
public class WelcomeFragment extends Fragment {

    private int currentStep = 0;
    private final int TOTAL_STEPS = 12;

    private View onboardingHeader;
    private ProgressBar progressBar;
    private ViewGroup contentContainer;
    private View screenWelcome;
    private LinearLayout screenSurvey;
    private TextView textQuestion;
    private TextView textInstruction;
    private LinearLayout layoutOptions;
    private View layoutSlider;
    private TextView textSliderValue;
    private Slider onboardingSlider;
    private View imageOnboarding;
    private MaterialButton buttonContinue;
    private View buttonSkipLogin;
    private View authLayout;
    private View welcomeOverlay;
    private View scrollOptions;
    private View scrollChips;
    private com.google.android.material.chip.ChipGroup chipGroupGenres;

    // Survey State
    private String selectedBooksRead = "";
    private int selectedTargetBooks = 10;
    private Set<String> selectedObstacles = new HashSet<>();
    private String selectedDailyTime = "";
    private Set<String> selectedGenres = new HashSet<>();
    private List<String> allAvailableGenres = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_welcome, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        UserViewModel userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);

        if (mAuth.getCurrentUser() != null) {
            userViewModel.fetchUserData();
            androidx.navigation.NavOptions navOptions = new androidx.navigation.NavOptions.Builder()
                    .setPopUpTo(R.id.welcomeFragment, true)
                    .build();
            Navigation.findNavController(view).navigate(R.id.booksFragment, null, navOptions);
            return;
        }

        // Reset step if we come back from Login/Register
        currentStep = 0;

        // Initialize genres statically
        allAvailableGenres.clear();
        allAvailableGenres.addAll(java.util.Arrays.asList(
                "Allegory", "Children", "Christmas stories", "Classic", "Essays",
                "Fantasy", "Feminist literature", "Fiction", "Gothic fiction",
                "Historical fiction", "Horror", "Mystery", "Novellas", "Philosophy",
                "Political satire", "Psychological fiction", "Romance", "Satire",
                "Science fiction", "Self-Help", "Short stories", "Supernatural fiction",
                "Vampires", "Weird fiction"
        ));
        Collections.sort(allAvailableGenres);

        // Bind views
        onboardingHeader = view.findViewById(R.id.onboarding_header);
        progressBar = view.findViewById(R.id.onboarding_progress);
        contentContainer = view.findViewById(R.id.onboarding_content_container);
        screenWelcome = view.findViewById(R.id.screen_welcome);
        screenSurvey = view.findViewById(R.id.screen_survey);
        textQuestion = view.findViewById(R.id.text_question);
        textInstruction = view.findViewById(R.id.text_instruction);
        layoutOptions = view.findViewById(R.id.layout_options);
        layoutSlider = view.findViewById(R.id.layout_slider);
        textSliderValue = view.findViewById(R.id.text_slider_value);
        onboardingSlider = view.findViewById(R.id.onboarding_slider);
        imageOnboarding = view.findViewById(R.id.image_onboarding);
        buttonContinue = view.findViewById(R.id.button_continue);
        buttonSkipLogin = view.findViewById(R.id.button_skip_login);
        authLayout = view.findViewById(R.id.auth_layout);
        welcomeOverlay = view.findViewById(R.id.welcome_overlay);
        scrollOptions = view.findViewById(R.id.scroll_options);
        scrollChips = view.findViewById(R.id.scroll_chips);
        chipGroupGenres = view.findViewById(R.id.chip_group_genres);

        buttonContinue.setOnClickListener(v -> nextStep());
        buttonSkipLogin.setOnClickListener(v -> {
            clearSurveyData();
            showFinalAuth();
        });
        view.findViewById(R.id.button_back_onboarding).setOnClickListener(v -> prevStep());

        view.findViewById(R.id.button_login).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_welcomeFragment_to_loginFragment)
        );

        view.findViewById(R.id.button_register).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_welcomeFragment_to_registerFragment)
        );

        onboardingSlider.addOnChangeListener((slider, value, fromUser) -> {
            int intValue = (int) value;
            selectedTargetBooks = intValue;
            if (intValue >= 51) {
                textSliderValue.setText("50+ books / year");
            } else {
                textSliderValue.setText(intValue + " books / year");
            }
        });

        updateUI();
    }

    private void nextStep() {
        if (currentStep == 11) {
            saveSurveyData();
        }
        if (currentStep < TOTAL_STEPS - 1) {
            currentStep++;
            updateUI();
        } else {
            Navigation.findNavController(getView()).navigate(R.id.action_welcomeFragment_to_registerFragment);
        }
    }

    private void prevStep() {
        if (currentStep > 0) {
            currentStep--;
            updateUI();
        }
    }

    private void updateUI() {
        Fade fade = new Fade();
        fade.setDuration(300);
        TransitionManager.beginDelayedTransition((ViewGroup) getView(), fade);

        // Progress bar (Step 0 is initial welcome, so start progress from step 1)
        if (currentStep == 0) {
            onboardingHeader.setVisibility(View.GONE);
            screenWelcome.setVisibility(View.VISIBLE);
            screenSurvey.setVisibility(View.GONE);
            getView().findViewById(R.id.grid_background).setVisibility(View.VISIBLE);
            welcomeOverlay.setVisibility(View.VISIBLE);
            getView().setBackgroundResource(R.drawable.bg_welcome_gradient);
            buttonContinue.setText("Start");
            buttonSkipLogin.setVisibility(View.VISIBLE);
            buttonContinue.setVisibility(View.VISIBLE);
            buttonContinue.setBackgroundResource(R.drawable.button_gradient_primary);
            buttonContinue.setTextColor(getResources().getColor(android.R.color.white));
        } else {
            onboardingHeader.setVisibility(View.VISIBLE);
            progressBar.setProgress((currentStep * 100) / (TOTAL_STEPS - 1));
            screenWelcome.setVisibility(View.GONE);
            screenSurvey.setVisibility(View.VISIBLE);
            getView().findViewById(R.id.grid_background).setVisibility(View.GONE);
            welcomeOverlay.setVisibility(View.GONE);
            getView().setBackgroundColor(getResources().getColor(android.R.color.white));
            buttonSkipLogin.setVisibility(View.GONE);
            
            // Standard Next button, will be hidden if step has auto-options
            buttonContinue.setVisibility(View.VISIBLE);
            buttonContinue.setText("Next");
            buttonContinue.setBackgroundResource(R.drawable.bg_tab_layout);
            buttonContinue.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.primary_600)));
            buttonContinue.setTextColor(getResources().getColor(android.R.color.white));
            
            renderStep();
        }
    }

    private void renderStep() {
        layoutOptions.removeAllViews();
        scrollOptions.setVisibility(View.GONE);
        scrollChips.setVisibility(View.GONE);
        layoutSlider.setVisibility(View.GONE);
        imageOnboarding.setVisibility(View.GONE);
        textInstruction.setVisibility(View.VISIBLE);
        
        // Ensure standard padding/gravity
        screenSurvey.setGravity(android.view.Gravity.CENTER);
        layoutOptions.setPadding(0, 0, 0, 0);

        switch (currentStep) {
            case 1:
                textQuestion.setText("Welcome to ListenTale!");
                textInstruction.setText("Over 2 million people have chosen ListenTale to explore and develop themselves. Welcome to the family!");
                imageOnboarding.setVisibility(View.VISIBLE);
                break;
            case 2:
                textQuestion.setText("How many books did you read last year?");
                textInstruction.setText("This helps us understand your reading habits.");
                scrollOptions.setVisibility(View.VISIBLE);
                buttonContinue.setVisibility(View.GONE);
                addOption("0-2 books", "I rarely read/listen to books.", true, false, 2);
                addOption("3-5 books", "I read occasionally.", true, false, 2);
                addOption("More than 6 books", "Books are my passion.", true, false, 2);
                break;
            case 3:
                textQuestion.setText("What is your target number?");
                textInstruction.setText("Set a goal for yourself this year.");
                layoutSlider.setVisibility(View.VISIBLE);
                onboardingSlider.setValue(selectedTargetBooks);
                break;
            case 4:
                textQuestion.setText("10 is the average number");
                textInstruction.setText("ListenTale members listen to 10 books each year on average, compared to 1.2 for most people.");
                imageOnboarding.setVisibility(View.VISIBLE);
                break;
            case 5:
                textQuestion.setText("What's stopping you from reaching your goal?");
                textInstruction.setText("You can choose multiple options.");
                scrollOptions.setVisibility(View.VISIBLE);
                buttonContinue.setText("Next");
                buttonContinue.setVisibility(View.VISIBLE);
                addOption("Lack of time / Too busy", "", false, true, 5);
                addOption("Procrastination", "", false, true, 5);
                addOption("Too many distractions", "", false, true, 5);
                addOption("Financial constraints", "", false, true, 5);
                updateButtonState();
                break;
            case 6:
                textQuestion.setText("How much time do you spend daily?");
                textInstruction.setText("Even 15 minutes a day makes a difference.");
                scrollOptions.setVisibility(View.VISIBLE);
                buttonContinue.setVisibility(View.GONE);
                addOption("Less than 30 minutes", "", true, false, 6);
                addOption("30-60 minutes", "", true, false, 6);
                addOption("Over 60 minutes", "", true, false, 6);
                break;
            case 7:
                textQuestion.setText("You are not alone!");
                textInstruction.setText("2/3 of our users choose ListenTale because it fits their busy lifestyle.");
                imageOnboarding.setVisibility(View.VISIBLE);
                break;
            case 8:
                textQuestion.setText("Persistence is the key!");
                textInstruction.setText("Allow ListenTale to send notifications to help you stay on track.");
                imageOnboarding.setVisibility(View.VISIBLE);
                break;
            case 9:
                textQuestion.setText("Which topics interest you?");
                textInstruction.setText("You can choose multiple options (Max 10).");
                scrollChips.setVisibility(View.VISIBLE);
                buttonContinue.setText("Next");
                buttonContinue.setVisibility(View.VISIBLE);
                chipGroupGenres.removeAllViews();
                
                List<String> displayGenres = allAvailableGenres;

                for (String genre : displayGenres) {
                    com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(getContext());
                    chip.setText(genre);
                    chip.setCheckable(true);
                    chip.setChecked(selectedGenres.contains(genre));
                    chip.setChipBackgroundColorResource(R.color.white);
                    chip.setCheckedIconVisible(false);
                    chip.setTextColor(getResources().getColor(R.color.slate_900));
                    
                    if (chip.isChecked()) {
                        chip.setChipBackgroundColorResource(R.color.primary_600);
                        chip.setTextColor(getResources().getColor(android.R.color.white));
                    }
                    
                    chip.setOnClickListener(v -> {
                        if (chip.isChecked()) {
                            if (selectedGenres.size() >= 10) {
                                chip.setChecked(false);
                                android.widget.Toast.makeText(getContext(), "You can select up to 10 topics", android.widget.Toast.LENGTH_SHORT).show();
                            } else {
                                selectedGenres.add(genre);
                                chip.setChipBackgroundColorResource(R.color.primary_600);
                                chip.setTextColor(getResources().getColor(android.R.color.white));
                            }
                        } else {
                            selectedGenres.remove(genre);
                            chip.setChipBackgroundColorResource(R.color.white);
                            chip.setTextColor(getResources().getColor(R.color.slate_900));
                        }
                        updateGenresChipsState();
                        setButtonEnabled(!selectedGenres.isEmpty());
                    });
                    chipGroupGenres.addView(chip);
                }
                updateGenresChipsState();
                setButtonEnabled(!selectedGenres.isEmpty());
                break;
            case 10:
                textQuestion.setText("93% Success Rate");
                textInstruction.setText("93% of members agree that ListenTale helped them develop their personal skills.");
                imageOnboarding.setVisibility(View.VISIBLE);
                break;
            case 11:
                textQuestion.setText("Everything is ready!");
                textInstruction.setText("ListenTale will suggest a plan to help you achieve your goals.");
                imageOnboarding.setVisibility(View.VISIBLE);
                buttonContinue.setText("Finish");
                buttonContinue.setVisibility(View.VISIBLE);
                setButtonEnabled(true);
                break;
        }
    }

    private void setButtonEnabled(boolean enabled) {
        buttonContinue.setEnabled(enabled);
        if (enabled) {
            buttonContinue.setAlpha(1.0f);
        } else {
            buttonContinue.setAlpha(0.5f);
        }
    }

    private void updateGenresChipsState() {
        boolean atLimit = selectedGenres.size() >= 10;
        for (int i = 0; i < chipGroupGenres.getChildCount(); i++) {
            com.google.android.material.chip.Chip chip = (com.google.android.material.chip.Chip) chipGroupGenres.getChildAt(i);
            if (!chip.isChecked()) {
                if (atLimit) {
                    chip.setAlpha(0.5f);
                    chip.setClickable(false);
                } else {
                    chip.setAlpha(1.0f);
                    chip.setClickable(true);
                }
            } else {
                chip.setAlpha(1.0f);
                chip.setClickable(true);
            }
        }
    }

    private void updateButtonState() {
        boolean anySelected = false;
        for (int i = 0; i < layoutOptions.getChildCount(); i++) {
            View child = layoutOptions.getChildAt(i);
            if (child.findViewById(R.id.image_check).getVisibility() == View.VISIBLE) {
                anySelected = true;
                break;
            }
        }
        setButtonEnabled(anySelected);
    }

    private void addOption(String title, String subtitle, boolean autoNext, boolean showTick, int step) {
        View optionView = getLayoutInflater().inflate(R.layout.item_onboarding_option, layoutOptions, false);
        com.google.android.material.card.MaterialCardView cardView = (com.google.android.material.card.MaterialCardView) optionView;
        TextView textTitle = optionView.findViewById(R.id.text_option_title);
        TextView textSub = optionView.findViewById(R.id.text_option_subtitle);
        ImageView checkIcon = optionView.findViewById(R.id.image_check);
        
        textTitle.setText(title);
        if (subtitle == null || subtitle.isEmpty()) {
            textSub.setVisibility(View.GONE);
        } else {
            textSub.setText(subtitle);
        }

        // Restore selected state when rendering
        boolean isSelected = false;
        if (step == 2 && title.equals(selectedBooksRead)) isSelected = true;
        if (step == 5 && selectedObstacles.contains(title)) isSelected = true;
        if (step == 6 && title.equals(selectedDailyTime)) isSelected = true;
        
        if (isSelected) {
             if (autoNext) {
                 cardView.setStrokeWidth(4);
                 cardView.setStrokeColor(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.primary_600)));
                 cardView.setCardBackgroundColor(getResources().getColor(R.color.primary_light));
             } else {
                 checkIcon.setVisibility(View.VISIBLE);
                 cardView.setStrokeWidth(0);
                 cardView.setCardBackgroundColor(getResources().getColor(R.color.primary_600));
                 textTitle.setTextColor(getResources().getColor(android.R.color.white));
             }
        }

        optionView.setOnClickListener(v -> {
            if (autoNext) {
                // Save state
                if (step == 2) selectedBooksRead = title;
                if (step == 6) selectedDailyTime = title;
                
                // Single choice mode
                cardView.setStrokeWidth(4);
                cardView.setStrokeColor(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.primary_600)));
                cardView.setCardBackgroundColor(getResources().getColor(R.color.primary_light));
                v.postDelayed(() -> nextStep(), 200);
            } else {
                // Multi choice mode: Toggle
                boolean willBeSelected = (checkIcon.getVisibility() == View.GONE);
                
                if (willBeSelected) {
                    checkIcon.setVisibility(View.VISIBLE);
                    cardView.setStrokeWidth(0);
                    cardView.setCardBackgroundColor(getResources().getColor(R.color.primary_600));
                    textTitle.setTextColor(getResources().getColor(android.R.color.white));
                    if (step == 5) selectedObstacles.add(title);
                } else {
                    checkIcon.setVisibility(View.GONE);
                    cardView.setStrokeWidth(0);
                    cardView.setCardBackgroundColor(getResources().getColor(android.R.color.white));
                    textTitle.setTextColor(getResources().getColor(R.color.slate_900));
                    if (step == 5) selectedObstacles.remove(title);
                }
                updateButtonState();
            }
        });

        layoutOptions.addView(optionView);
    }

    private void showFinalAuth() {
        TransitionManager.beginDelayedTransition((ViewGroup) getView(), new Fade());
        onboardingHeader.setVisibility(View.GONE);
        contentContainer.setVisibility(View.GONE);
        getView().findViewById(R.id.button_container).setVisibility(View.GONE);
        
        // Restore Welcome State visuals
        getView().findViewById(R.id.grid_background).setVisibility(View.VISIBLE);
        welcomeOverlay.setVisibility(View.VISIBLE);
        getView().setBackgroundResource(R.drawable.bg_welcome_gradient);

        authLayout.setVisibility(View.VISIBLE);
    }

    private void saveSurveyData() {
        if (getActivity() == null) return;
        SharedPreferences prefs = getActivity().getSharedPreferences("SurveyPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("selectedBooksRead", selectedBooksRead);
        editor.putInt("selectedTargetBooks", selectedTargetBooks);
        editor.putStringSet("selectedObstacles", selectedObstacles);
        editor.putString("selectedDailyTime", selectedDailyTime);
        editor.putStringSet("selectedGenres", selectedGenres);
        editor.putBoolean("hasSurveyData", true);
        editor.apply();
    }

    private void clearSurveyData() {
        if (getActivity() == null) return;
        SharedPreferences prefs = getActivity().getSharedPreferences("SurveyPrefs", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }
}
