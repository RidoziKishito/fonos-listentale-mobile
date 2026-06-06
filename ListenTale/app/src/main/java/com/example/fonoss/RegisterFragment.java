package com.example.fonoss;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RegisterFragment extends Fragment {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private UserViewModel userViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);

        TextInputLayout nameLayout = view.findViewById(R.id.input_name_layout);
        TextInputLayout emailLayout = view.findViewById(R.id.input_email_layout);
        TextInputLayout passwordLayout = view.findViewById(R.id.input_password_layout);
        
        TextInputEditText inputName = view.findViewById(R.id.input_name);
        TextInputEditText inputEmail = view.findViewById(R.id.input_email);
        TextInputEditText inputPassword = view.findViewById(R.id.input_password);
        
        TextWatcher registerWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                nameLayout.setError(null);
                emailLayout.setError(null);
                passwordLayout.setError(null);
            }
            @Override public void afterTextChanged(Editable s) {}
        };

        inputName.addTextChangedListener(registerWatcher);
        inputEmail.addTextChangedListener(registerWatcher);
        inputPassword.addTextChangedListener(registerWatcher);

        inputPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                performRegister(inputName, inputEmail, inputPassword, nameLayout, emailLayout, passwordLayout);
                return true;
            }
            return false;
        });

        view.findViewById(R.id.button_back).setOnClickListener(v -> 
            Navigation.findNavController(v).popBackStack(R.id.welcomeFragment, false)
        );

        view.findViewById(R.id.button_sign_up).setOnClickListener(v -> 
            performRegister(inputName, inputEmail, inputPassword, nameLayout, emailLayout, passwordLayout)
        );

        view.findViewById(R.id.text_login).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_registerFragment_to_loginFragment)
        );
    }

    private void performRegister(TextInputEditText name, TextInputEditText email, TextInputEditText password,
                                 TextInputLayout nameLayout, TextInputLayout emailLayout, TextInputLayout passwordLayout) {
        
        hideKeyboard(name);
        String nameStr = name.getText().toString().trim();
        String emailStr = email.getText().toString().trim();
        String passStr = password.getText().toString().trim();

        boolean hasError = false;
        if (nameStr.isEmpty()) { nameLayout.setError("Full name required"); hasError = true; }
        if (emailStr.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(emailStr).matches()) { 
            emailLayout.setError("Invalid email"); hasError = true; 
        }
        if (passStr.length() < 6) { passwordLayout.setError("Min 6 characters"); hasError = true; }

        if (hasError) return;

        mAuth.createUserWithEmailAndPassword(emailStr, passStr)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();
                        Map<String, Object> user = new HashMap<>();
                        user.put("name", nameStr);
                        user.put("email", emailStr);
                        
                        // Khởi tạo các mảng rỗng để tránh lỗi mất dữ liệu sau này
                        user.put("saved", new ArrayList<>());
                        user.put("downloaded", new ArrayList<>());
                        user.put("inProgress", new ArrayList<>());
                        user.put("completed", new ArrayList<>());
                        user.put("progressPositions", new HashMap<String, Long>());
                        user.put("progressChapters", new HashMap<String, Long>());

                        db.collection("users").document(uid).set(user)
                                .addOnSuccessListener(aVoid -> {
                                    userViewModel.fetchUserData();
                                    Toast.makeText(getContext(), "Registration successful!", Toast.LENGTH_SHORT).show();
                                    Navigation.findNavController(name).navigate(R.id.action_registerFragment_to_booksFragment);
                                })
                                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    } else {
                        Toast.makeText(getContext(), "Auth Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void hideKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }
}
