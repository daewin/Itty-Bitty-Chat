package com.daewin.ibachat.settings;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.daewin.ibachat.MainActivity;
import com.daewin.ibachat.R;
import com.daewin.ibachat.databinding.SettingsActivityBinding;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

/**
 * Settings page
 */

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SettingsActivityBinding binding
                = DataBindingUtil.setContentView(this, R.layout.settings_activity);

        // Toolbar settings
        setSupportActionBar(binding.settingsToolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }

        binding.logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AuthUI.getInstance()
                        .signOut(SettingsActivity.this)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {

                            public void onComplete(@NonNull Task<Void> task) {
                                // The user is now signed out
                                startActivity(new Intent(SettingsActivity.this,
                                                                            MainActivity.class));
                                finishAffinity();
                            }
                        });
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
