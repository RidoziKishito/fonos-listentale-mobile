package com.example.fonoss.ui.auth;

import com.example.fonoss.R;
import dagger.hilt.android.AndroidEntryPoint;

import com.example.fonoss.utils.UiNotifier;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.google.android.material.button.MaterialButton;

@AndroidEntryPoint
public class AccountInfoFragment extends Fragment {

    private boolean isEditing = false;
    private TextView textNameDisplay, textEmailDisplay;
    private EditText editName;
    private MaterialButton buttonEditSave;
    private UserViewModel userViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_account_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);

        textNameDisplay = view.findViewById(R.id.text_name_display);
        textEmailDisplay = view.findViewById(R.id.text_email_display);
        editName = view.findViewById(R.id.edit_name);
        buttonEditSave = view.findViewById(R.id.button_edit_save);

        userViewModel.getUserName().observe(getViewLifecycleOwner(), name -> textNameDisplay.setText(name));
        userViewModel.getUserEmail().observe(getViewLifecycleOwner(), email -> textEmailDisplay.setText(email));

        view.findViewById(R.id.button_back).setOnClickListener(v -> 
            Navigation.findNavController(v).navigateUp()
        );

        buttonEditSave.setOnClickListener(v -> {
            if (!isEditing) {
                enterEditMode();
            } else {
                saveChanges();
            }
        });
    }

    private void enterEditMode() {
        isEditing = true;
        buttonEditSave.setText("Save Changes");
        textNameDisplay.setVisibility(View.GONE);
        editName.setVisibility(View.VISIBLE);
        editName.setText(textNameDisplay.getText());
        editName.requestFocus();
    }

    private void saveChanges() {
        String newName = editName.getText().toString().trim();

        if (newName.isEmpty()) {
            UiNotifier.warning(getContext(), "Name cannot be empty");
            return;
        }

        if (!com.example.fonoss.utils.TextModerationHelper.isTextAppropriate(requireContext(), newName)) {
            UiNotifier.warning(getContext(), "Inappropriate name (contains restricted words)");
            return;
        }

        userViewModel.updateUserName(newName);
        
        isEditing = false;
        buttonEditSave.setText("Edit Profile");
        textNameDisplay.setVisibility(View.VISIBLE);
        editName.setVisibility(View.GONE);

        UiNotifier.success(getContext(), "Profile updated");
    }
}
