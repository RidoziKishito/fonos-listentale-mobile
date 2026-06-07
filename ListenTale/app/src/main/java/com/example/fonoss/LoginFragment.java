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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.GoogleAuthProvider;

import androidx.credentials.CredentialManager;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.Credential;
import androidx.credentials.CustomCredential;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import android.widget.Toast;

public class LoginFragment extends Fragment {

    private FirebaseAuth mAuth;
    private UserViewModel userViewModel;
    private CredentialManager credentialManager;
    private final Executor executor = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
        credentialManager = CredentialManager.create(requireContext());

        TextInputLayout emailLayout = view.findViewById(R.id.input_email_layout);
        TextInputLayout passwordLayout = view.findViewById(R.id.input_password_layout);
        TextInputEditText inputEmail = view.findViewById(R.id.input_email);
        TextInputEditText inputPassword = view.findViewById(R.id.input_password);
        MaterialButton buttonSignIn = view.findViewById(R.id.button_sign_in);
        View buttonGoogle = view.findViewById(R.id.button_google_sign_in);

        TextWatcher authWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                emailLayout.setError(null);
                passwordLayout.setError(null);
            }
            @Override public void afterTextChanged(Editable s) {}
        };

        inputEmail.addTextChangedListener(authWatcher);
        inputPassword.addTextChangedListener(authWatcher);

        inputPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                performLogin(inputEmail, inputPassword, emailLayout, passwordLayout);
                return true;
            }
            return false;
        });

        view.findViewById(R.id.button_back).setOnClickListener(v -> 
            Navigation.findNavController(v).popBackStack(R.id.welcomeFragment, false)
        );

        buttonSignIn.setOnClickListener(v -> 
            performLogin(inputEmail, inputPassword, emailLayout, passwordLayout)
        );

        view.findViewById(R.id.text_signup).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_loginFragment_to_registerFragment)
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
                    Toast.makeText(getContext(), "Google Sign In failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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
                                userViewModel.fetchUserData();
                                Navigation.findNavController(requireView()).navigate(R.id.action_loginFragment_to_booksFragment);
                            } else {
                                Toast.makeText(getContext(), "Auth failed", Toast.LENGTH_SHORT).show();
                            }
                        });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void performLogin(TextInputEditText email, TextInputEditText password, 
                              TextInputLayout emailLayout, TextInputLayout passwordLayout) {
        
        hideKeyboard(email);
        String emailStr = email.getText().toString().trim();
        String passStr = password.getText().toString().trim();

        // Basic validation before network call
        if (emailStr.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(emailStr).matches()) {
            emailLayout.setError("Invalid email format");
            return;
        }
        if (passStr.isEmpty()) {
            passwordLayout.setError("Password required");
            return;
        }

        mAuth.signInWithEmailAndPassword(emailStr, passStr)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        userViewModel.fetchUserData();
                        Navigation.findNavController(email).navigate(R.id.action_loginFragment_to_booksFragment);
                    } else {
                        Exception e = task.getException();
                        // Security standard: Don't reveal which one is wrong
                        if (e instanceof FirebaseAuthInvalidUserException) {
                            emailLayout.setError("User does not exist");
                        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            // This catch both wrong password AND badly formatted auth data
                            emailLayout.setError("Incorrect email or password");
                            passwordLayout.setError("Incorrect email or password");
                        } else {
                            emailLayout.setError("Login failed. Try again later.");
                        }
                    }
                });
    }

    private void hideKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }
}
