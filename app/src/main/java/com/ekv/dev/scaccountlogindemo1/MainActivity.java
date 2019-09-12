package com.ekv.dev.scaccountlogindemo1;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.ekv.dev.scaccountlogindemo1.presenter.MainActivityPresenter;
import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import java.util.Arrays;
import java.util.Collections;

public class MainActivity extends MainActivityPresenter {

    public CallbackManager callbackManager;
    private AccessTokenTracker accessTokenTracker;
    private ProfileTracker profileTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FacebookSdk.sdkInitialize(this.getApplicationContext());
        callbackManager = CallbackManager.Factory.create();

        accessTokenTracker= new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(AccessToken oldToken, AccessToken newToken) {
                Toast.makeText(getApplicationContext(), "AccessToken changed", Toast.LENGTH_SHORT).show();
            }
        };

        profileTracker = new ProfileTracker() {
            @Override
            protected void onCurrentProfileChanged(Profile oldProfile, Profile newProfile) {
                displayMessage(newProfile);
            }
        };

        accessTokenTracker.startTracking();
        profileTracker.startTracking();

        LoginManager.getInstance().registerCallback(callbackManager, callback);
        LoginManager.getInstance().logInWithReadPermissions(this, Collections.singletonList("public_profile"));
//        LoginManager.getInstance().logInWithReadPermissions(this, Collections.singletonList("email"));
//        LoginManager.getInstance().logInWithReadPermissions(this, Collections.singletonList("publish_actions"));
//        LoginManager.getInstance().logInWithReadPermissions(this, Collections.singletonList("publish_actions"));
//        for (String permission : AccessToken.getCurrentAccessToken().getPermissions()) {
//            Log.d("Permission", "onCreate: Permission -> "+permission);
//        }
    }

    @Override
    public void onStop() {
        super.onStop();
        accessTokenTracker.stopTracking();
        profileTracker.stopTracking();
    }

    @Override
    public void onResume() {
        super.onResume();
        Profile profile = Profile.getCurrentProfile();
        displayMessage(profile);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        callbackManager.onActivityResult(requestCode, resultCode, data); 
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void displayMessage(Profile profile){
        String name = (profile != null) ? profile.getName() : "User not logged in";
        ((TextView)findViewById(R.id.tv_result)).setText(name);
    }

    public FacebookCallback<LoginResult> callback = new FacebookCallback<LoginResult>() {
        @Override
        public void onSuccess(LoginResult loginResult) {
            AccessToken accessToken = AccessToken.getCurrentAccessToken();
            boolean isLoggedIn = accessToken != null && !accessToken.isExpired();
            if (accessToken != null && isLoggedIn) {
                Toast.makeText(getApplicationContext(), "AccessToken = "+accessToken.getToken() , Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onCancel() {
//            Toast.makeText(getApplicationContext(), "User Cancelled login", Toast.LENGTH_SHORT).show(); 
        }

        @Override
        public void onError(FacebookException error) {
//            Toast.makeText(getApplicationContext(), "Error occurred while login", Toast.LENGTH_SHORT).show();
        }
    };
}
