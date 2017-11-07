package com.daewin.ibachat.chat;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.daewin.ibachat.databinding.StartNewChatItemBinding;
import com.daewin.ibachat.model.ThreadModel;
import com.daewin.ibachat.model.UserModel;
import com.daewin.ibachat.user.User;
import com.github.wrdlbrnft.sortedlistadapter.SortedListAdapter;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Comparator;

/**
 */

public class StartNewChatListAdapter extends SortedListAdapter<UserModel> {

    private DatabaseReference mDatabase;

    StartNewChatListAdapter(@NonNull Context context,
                            @NonNull Class<UserModel> aClass,
                            @NonNull Comparator<UserModel> comparator) {
        super(context, aClass, comparator);
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    @NonNull
    @Override
    protected ViewHolder<? extends UserModel> onCreateViewHolder
            (@NonNull LayoutInflater layoutInflater, @NonNull ViewGroup viewGroup, int i) {

        StartNewChatItemBinding binding =
                StartNewChatItemBinding.inflate(layoutInflater, viewGroup, false);

        return new FriendViewHolder(binding);
    }

    private class FriendViewHolder extends SortedListAdapter.ViewHolder<UserModel> {

        private StartNewChatItemBinding binding;
        private String currentUsersEncodedEmail;
        private String currentUsersName;

        FriendViewHolder(@NonNull StartNewChatItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            // Current user reference initialization
            UserModel currentUser = User.getCurrentUserModel();

            if (currentUser != null) {
                currentUsersEncodedEmail = currentUser.getEncodedEmail();
                currentUsersName = currentUser.getName();
            }
        }

        @Override
        protected void performBind(@NonNull final UserModel userModel) {
            binding.setUser(userModel);
            binding.executePendingBindings();

            binding.itemConstraintLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    final Intent startChat = new Intent(view.getContext(), ChatActivity.class);
                    startChat.putExtra(ChatActivity.ARG_NAME_OF_FRIEND, userModel.getName());
                    startChat.putExtra(ChatActivity.ARG_EMAIL_OF_FRIEND, userModel.getEmail());

                    getThreadID(userModel).addOnSuccessListener(new OnSuccessListener<String>() {
                        @Override
                        public void onSuccess(String threadID) {
                            startChat.putExtra(ChatActivity.ARG_CURRENT_THREAD_ID, threadID);
                            view.getContext().startActivity(startChat);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // TODO
                        }
                    });
                }
            });
        }

        private Task<String> getThreadID(UserModel userModel) {

            final TaskCompletionSource<String> taskCompletionSource = new TaskCompletionSource<>();

            final String encodedEmailOfFriend
                    = User.getEncodedEmail(userModel.getEmail());

            final String nameOfFriend = userModel.getName();

            DatabaseReference ourThreadWithFriend = mDatabase
                    .child("users")
                    .child(currentUsersEncodedEmail)
                    .child("user_threads_with")
                    .child(encodedEmailOfFriend);

            ourThreadWithFriend.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {

                        String threadID = dataSnapshot.child("id").getValue(String.class);
                        taskCompletionSource.setResult(threadID);
                    } else {

                        String newThreadID = createNewThread(nameOfFriend, encodedEmailOfFriend);
                        taskCompletionSource.setResult(newThreadID);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    taskCompletionSource.setException(databaseError.toException());
                }
            });

            return taskCompletionSource.getTask();
        }


        private String createNewThread(String nameOfFriend, String encodedEmailOfFriend) {

            DatabaseReference threadsReference = mDatabase.child("threads").push();

            threadsReference.child("members")
                    .child(encodedEmailOfFriend)
                    .child("is_typing")
                    .setValue(false);

            threadsReference.child("members")
                    .child(currentUsersEncodedEmail)
                    .child("is_typing")
                    .setValue(false);

            createIndividualUserThreadIndex(threadsReference.getKey(),
                    nameOfFriend, encodedEmailOfFriend);

            return threadsReference.getKey();
        }

        private void createIndividualUserThreadIndex(String threadID,
                                                     String nameOfFriend,
                                                     String encodedEmailOfFriend) {

            DatabaseReference ourThreadWithFriend = mDatabase
                    .child("users")
                    .child(currentUsersEncodedEmail)
                    .child("user_threads_with")
                    .child(encodedEmailOfFriend);

            ourThreadWithFriend.child("id").setValue(threadID);
            ourThreadWithFriend.child("name").setValue(nameOfFriend);

            DatabaseReference friendsThreadWithUs = mDatabase
                    .child("users")
                    .child(encodedEmailOfFriend)
                    .child("user_threads_with")
                    .child(currentUsersEncodedEmail);

            friendsThreadWithUs.child("id").setValue(threadID);
            friendsThreadWithUs.child("name").setValue(currentUsersName);
        }
    }
}
