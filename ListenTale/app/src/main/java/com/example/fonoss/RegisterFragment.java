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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import androidx.credentials.CredentialManager;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.Credential;
import androidx.credentials.CustomCredential;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class RegisterFragment extends Fragment {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private UserViewModel userViewModel;
    private CredentialManager credentialManager;
    private final Executor executor = Executors.newSingleThreadExecutor();

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
        credentialManager = CredentialManager.create(requireContext());

        TextInputLayout nameLayout = view.findViewById(R.id.input_name_layout);
        TextInputLayout emailLayout = view.findViewById(R.id.input_email_layout);
        TextInputLayout passwordLayout = view.findViewById(R.id.input_password_layout);
        
        TextInputEditText inputName = view.findViewById(R.id.input_name);
        TextInputEditText inputEmail = view.findViewById(R.id.input_email);
        TextInputEditText inputPassword = view.findViewById(R.id.input_password);
        View buttonGoogle = view.findViewById(R.id.button_google_sign_up);
        
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

        buttonGoogle.setOnClickListener(v -> performGoogleSignIn());
    }

    private void performGoogleSignIn() {
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getString(R.string.default_web_client_id))
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        credentialManager.getCredentialAsync(requireActivity(), request, null, executor, new androidx.credentials.CredentialManagerCallback<GetCredentialResponse, androidx.credentials.exceptions.GetCredentialException>() {
            @Override
            public void onResult(GetCredentialResponse result) {
                handleGoogleSignInResult(result);
            }

            @Override
            public void onError(@NonNull androidx.credentials.exceptions.GetCredentialException e) {
                requireActivity().runOnUiThread(() -> 
                    UiNotifier.error(getContext(), "Google sign in failed"));
            }
        });
    }

    private void handleGoogleSignInResult(GetCredentialResponse result) {
        Credential credential = result.getCredential();
        if (credential instanceof CustomCredential && credential.getType().equals(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
            try {
                GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.getData());
                String idToken = googleIdTokenCredential.getIdToken();
                AuthCredential authCredential = GoogleAuthProvider.getCredential(idToken, null);
                
                mAuth.signInWithCredential(authCredential)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                boolean isNewUser = task.getResult().getAdditionalUserInfo().isNewUser();
                                if (isNewUser) {
                                    createFirebaseUserDoc(mAuth.getCurrentUser().getDisplayName(), mAuth.getCurrentUser().getEmail());
                                } else {
                                    userViewModel.fetchUserData();
                                    Navigation.findNavController(requireView()).navigate(R.id.action_registerFragment_to_booksFragment);
                                }
                            } else {
                                UiNotifier.error(getContext(), "Authentication failed");
                            }
                        });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void createFirebaseUserDoc(String name, String email) {
        String uid = mAuth.getCurrentUser().getUid();
        Map<String, Object> user = new HashMap<>();
        user.put("name", name != null ? name : "Google User");
        user.put("email", email);
        user.put("saved", new ArrayList<>());
        user.put("downloaded", new ArrayList<>());
        user.put("inProgress", new ArrayList<>());
        user.put("completed", new ArrayList<>());
        user.put("progressPositions", new HashMap<String, Long>());
        user.put("progressChapters", new HashMap<String, Long>());

        db.collection("users").document(uid).set(user)
                .addOnSuccessListener(aVoid -> {
                    userViewModel.fetchUserData();
                    Navigation.findNavController(requireView()).navigate(R.id.action_registerFragment_to_booksFragment);
                });
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
                                    UiNotifier.success(getContext(), "Registration successful");
                                    Navigation.findNavController(name).navigate(R.id.action_registerFragment_to_booksFragment);
                                })
                                .addOnFailureListener(e -> UiNotifier.error(getContext(), "Registration failed"));
                    } else {
                        UiNotifier.error(getContext(), "Authentication failed");
                    }
                });
    }

    private void hideKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }
}
