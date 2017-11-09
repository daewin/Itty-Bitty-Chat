package com.daewin.ibachat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.daewin.ibachat.chat.ChatLandingActivity;
import com.daewin.ibachat.databinding.MainActivityBinding;
import com.daewin.ibachat.user.User;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Arrays;
import java.util.List;

/**
 * Main Activity to coordinate the login process
 */

public class MainActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 123;

    private MainActivityBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getLifecycle().addObserver(new MyLifecycleObserver());

        binding = DataBindingUtil.setContentView(this, R.layout.main_activity);

        // Go straight to the chat landing activity or show the login screen
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            startChatActivity(null);

        } else {
            binding.loginIndicatorTextView.setVisibility(View.VISIBLE);
            binding.logoImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startLoginActivity();
                }
            });
        }
    }

    private void startChatActivity(IdpResponse response) {
        startActivity(ChatLandingActivity.createIntent(this, response));
        finish();
    }

    private void startLoginActivity() {
        List<AuthUI.IdpConfig> providers
                = Arrays.asList(new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build(),
                new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build(),
                new AuthUI.IdpConfig.Builder(AuthUI.FACEBOOK_PROVIDER).build(),
                new AuthUI.IdpConfig.Builder(AuthUI.TWITTER_PROVIDER).build());

        Intent loginIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setTheme(R.style.NoActionBarTheme)
                .setAvailableProviders(providers)
                .build();

        startActivityForResult(loginIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            handleSignInResponse(resultCode, data);
            return;
        }

        showSnackbar(R.string.unknown_response);
    }

    @MainThread
    private void handleSignInResponse(int resultCode, Intent data) {
        final IdpResponse response = IdpResponse.fromResultIntent(data);

        // Successfully signed in
        if (resultCode == RESULT_OK) {

            binding.loginProgressBar.setVisibility(View.VISIBLE);
            binding.loginIndicatorTextView.setVisibility(View.GONE);

            final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

            if (currentUser != null) {
                // If the user has just registered, create a task to inform us when the
                // database information has been created successfully (or unsuccessfully)

                User.createUserDatabaseIfMissing()
                        .addOnSuccessListener(new OnSuccessListener<Boolean>() {

                            @Override
                            public void onSuccess(Boolean aBoolean) {
                                startChatActivity(response);
                            }

                        })
                        .addOnFailureListener(new OnFailureListener() {

                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w("Error", e.getMessage());
                                showSnackbar(R.string.firebase_error);
                                binding.loginProgressBar.setVisibility(View.INVISIBLE);
                                binding.loginIndicatorTextView.setVisibility(View.VISIBLE);
                                FirebaseAuth.getInstance().signOut();
                            }
                        });
            } else {
                binding.loginProgressBar.setVisibility(View.INVISIBLE);
                binding.loginIndicatorTextView.setVisibility(View.VISIBLE);
                showSnackbar(R.string.unknown_error);
            }

            return;

        } else {
            // Sign in failed
            if (response == null) {
                // User pressed back button
                showSnackbar(R.string.sign_in_cancelled);
                return;
            }

            if (response.getErrorCode() == ErrorCodes.NO_NETWORK) {
                showSnackbar(R.string.no_internet_connection);
                return;
            }

            if (response.getErrorCode() == ErrorCodes.UNKNOWN_ERROR) {
                showSnackbar(R.string.unknown_error);
                return;
            }
        }

        showSnackbar(R.string.unknown_sign_in_response);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @MainThread
    private void showSnackbar(@StringRes int errorMessageRes) {
        Snackbar.make(binding.getRoot(), errorMessageRes, Snackbar.LENGTH_LONG).show();
    }
}
