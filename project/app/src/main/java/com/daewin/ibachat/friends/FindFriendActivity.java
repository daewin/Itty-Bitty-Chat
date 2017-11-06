package com.daewin.ibachat.friends;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.SearchView;

import com.daewin.ibachat.MyLifecycleObserver;
import com.daewin.ibachat.R;
import com.daewin.ibachat.databinding.FindFriendActivityBinding;
import com.daewin.ibachat.user.User;
import com.daewin.ibachat.model.UserModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Find a friend via their email, and sorts the results lexicographically.
 */

public class FindFriendActivity extends AppCompatActivity {

    private DatabaseReference mUserDatabase;
    private FirebaseUser mCurrentUser;
    private FindFriendListAdapter mFindFriendListAdapter;
    private FindFriendActivityBinding binding;

    // Views
    private SearchView mSearchView;
    private RecyclerView mRecyclerView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getLifecycle().addObserver(new MyLifecycleObserver());

        binding = DataBindingUtil.setContentView(this, R.layout.find_friend_activity);

        // Toolbar settings
        setSupportActionBar(binding.searchToolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }

        // Database initialization. We use the index list for better performance(no shallow queries)
        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("users_index");

        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Initialize views
        mSearchView = binding.friendSearchView;
        mRecyclerView = binding.sendFriendRequestList;

        // Set up the RecyclerView
        initializeRecyclerView();

        // Set up the SearchView
        initializeSearchView();
    }

    @Override
    protected void onStop() {
        // Clear adapter list to force cleanup any remaining listeners
        clearAdapterList();
        super.onStop();
    }

    private void initializeRecyclerView() {
        // Use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // Use a linear layout manager
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Specify our adapter
        mFindFriendListAdapter = new FindFriendListAdapter
                (this, UserModel.class, UserModel.alphabeticalComparator);

        mRecyclerView.setAdapter(mFindFriendListAdapter);
    }

    private void initializeSearchView() {

        mSearchView.setQueryHint("Find friends via email");
        mSearchView.setIconified(false);

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                // Do nothing
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {

                if (newText.isEmpty()) {
                    clearAdapterList();
                    return false;
                }

                binding.searchProgressBar.setVisibility(View.VISIBLE);

                // Get the lexicographic range starting with newText
                Query usernameQuery = mUserDatabase
                        .orderByKey()
                        .startAt(newText)
                        .endAt(newText + Character.MAX_VALUE);

                usernameQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        List<UserModel> userModels = new ArrayList<>();

                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                            String name = snapshot.child("name").getValue(String.class);
                            String email = snapshot.getKey();
                            String decodedEmail = User.getDecodedEmail(email);

                            UserModel userModel = new UserModel(name, decodedEmail);

                            if (userModel.exists()) {
                                if (!userModel.getEmail().equals(mCurrentUser.getEmail())) {
                                    userModels.add(userModel);
                                }
                            }
                        }

                        updateAdapterList(userModels);
                        binding.searchProgressBar.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        clearAdapterList();
                        Log.w("Error", databaseError.toException().getMessage());
                        showSnackbar(R.string.firebase_database_error);
                    }
                });

                return false;
            }
        });
    }

    private void updateAdapterList(List<UserModel> userModels) {

        mFindFriendListAdapter.edit()
                .replaceAll(userModels)
                .commit();

        mRecyclerView.scrollToPosition(0);
    }

    private void clearAdapterList() {

        mFindFriendListAdapter.edit()
                .removeAll()
                .commit();

        mRecyclerView.scrollToPosition(0);
    }

    @MainThread
    private void showSnackbar(@StringRes int errorMessageRes) {
        Snackbar.make(binding.getRoot(), errorMessageRes, Snackbar.LENGTH_LONG).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        // Clear adapter list to force cleanup any remaining listeners
        clearAdapterList();
        super.onBackPressed();
    }
}
