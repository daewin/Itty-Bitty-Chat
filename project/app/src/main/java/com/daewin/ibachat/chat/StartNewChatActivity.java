package com.daewin.ibachat.chat;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.SearchView;

import com.daewin.ibachat.MyLifecycleObserver;
import com.daewin.ibachat.R;
import com.daewin.ibachat.databinding.StartNewChatActivityBinding;
import com.daewin.ibachat.user.User;
import com.daewin.ibachat.model.UserModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 */

public class StartNewChatActivity extends AppCompatActivity {

    private StartNewChatActivityBinding binding;
    private StartNewChatListAdapter mStartNewChatListAdapter;
    private RecyclerView mRecyclerView;
    private DatabaseReference friendsList;
    private ArrayList<UserModel> userModels;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getLifecycle().addObserver(new MyLifecycleObserver());

        binding = DataBindingUtil.setContentView(this, R.layout.start_new_chat_activity);

        // Toolbar settings
        setSupportActionBar(binding.chatToolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }

        initializeRecyclerView();
        initializeFriendsList();
        initializeSearchView();
    }

    private void initializeRecyclerView() {
        mRecyclerView = binding.chatRecyclerView;

        mRecyclerView.setHasFixedSize(true);

        // Use a linear layout manager
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Specify our adapter
        mStartNewChatListAdapter = new StartNewChatListAdapter
                (this, UserModel.class, UserModel.alphabeticalComparator);

        mRecyclerView.setAdapter(mStartNewChatListAdapter);
    }

    private void initializeFriendsList() {
        DatabaseReference userDatabase
                = FirebaseDatabase.getInstance().getReference().child("users");

        UserModel currentUser = User.getCurrentUserModel();

        if (currentUser != null) {
            String currentUsersEncodedEmail = currentUser.getEncodedEmail();
            friendsList = userDatabase.child(currentUsersEncodedEmail).child("friends");
        }

        userModels = new ArrayList<>();

        friendsList.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                    String name = snapshot.getValue(String.class);
                    String email = snapshot.getKey();
                    String decodedEmail = User.getDecodedEmail(email);

                    UserModel userModel = new UserModel(name, decodedEmail);

                    if (userModel.exists()) {
                        userModels.add(userModel);
                    }
                }

                // Display all initially
                updateAdapterList(userModels);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w("Error", databaseError.toException().getMessage());
            }
        });
    }

    private void initializeSearchView() {
        binding.chatSearchView.setQueryHint("Search friends");
        binding.chatSearchView.setIconified(false);

        binding.chatSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                // Do nothing
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {

                if (s.isEmpty()) {
                    updateAdapterList(userModels);
                    return false;
                }

                ArrayList<UserModel> filteredUserModels = new ArrayList<>();

                for (UserModel userModel : userModels) {

                    String name = userModel.getName().toLowerCase();
                    String email = userModel.getEmail().toLowerCase();
                    String query = s.toLowerCase();

                    if (email.contains(query) || name.contains(query)) {
                        filteredUserModels.add(userModel);
                    }
                }

                if (filteredUserModels.size() > 0) {
                    updateAdapterList(filteredUserModels);
                } else {
                    clearAdapterList();
                }

                return false;
            }
        });
    }

    private void updateAdapterList(List<UserModel> userModels) {

        mStartNewChatListAdapter.edit()
                .replaceAll(userModels)
                .commit();

        mRecyclerView.scrollToPosition(0);
    }

    private void clearAdapterList() {

        mStartNewChatListAdapter.edit()
                .removeAll()
                .commit();

        mRecyclerView.scrollToPosition(0);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
