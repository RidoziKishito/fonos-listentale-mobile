package com.example.fonoss.ui.auth;

import com.example.fonoss.R;
import dagger.hilt.android.AndroidEntryPoint;

import com.example.fonoss.ui.library.LibraryViewModel;
import com.example.fonoss.ui.home.WelcomeFragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import com.google.firebase.auth.FirebaseAuth;

@AndroidEntryPoint
public class ProfileFragment extends Fragment {

    private UserViewModel userViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
        LibraryViewModel libraryViewModel = new ViewModelProvider(requireActivity()).get(LibraryViewModel.class);
        TextView textName = view.findViewById(R.id.text_profile_name);

        userViewModel.getUserName().observe(getViewLifecycleOwner(), name -> {
            if (textName != null) textName.setText(name);
        });

        view.findViewById(R.id.button_account_info).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_profileFragment_to_accountInfoFragment)
        );

        view.findViewById(R.id.button_settings).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_profileFragment_to_settingsFragment)
        );

        view.findViewById(R.id.button_logout).setOnClickListener(v -> {
            libraryViewModel.clearLocalDownloads();
            FirebaseAuth.getInstance().signOut();
            userViewModel.clearData();
            // Navigate to WelcomeFragment and clear backstack
            NavOptions navOptions = new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_graph, true)
                    .build();
            Navigation.findNavController(v).navigate(R.id.welcomeFragment, null, navOptions);
        });
    }
}



