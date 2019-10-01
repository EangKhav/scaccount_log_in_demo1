package com.ekv.dev.scaccountlogindemo1;

import android.accounts.Account;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.ekv.dev.scaccountlogindemo1.presenter.MainActivityPresenter;
import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import java.util.Collections;

public class MainActivity extends MainActivityPresenter {

    public CallbackManager callbackManager;
    private AccessTokenTracker accessTokenTracker;
    private ProfileTracker profileTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AppEventsLogger.activateApp(getApplication());
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

        initGoogleLogIn();
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

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private int RC_SIGN_IN = 101;
    private GoogleSignInClient mGoogleSignInClient;
    private void initGoogleLogIn(){
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Set the dimensions of the sign-in button.
        Button signInButton = findViewById(R.id.google_log_in);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
            }
        });

        findViewById(R.id.facebook_log_in_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                findViewById(R.id.facebook_log_in).performClick();
            }
        });
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            // Signed in successfully, show authenticated UI.
            displayMessage1(account);
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w("EkvAuth", "signInResult:failed code=" + e.getStatusCode());
            displayMessage1((GoogleSignInAccount) null);
        }
    }

    private void displayMessage1(GoogleSignInAccount account){
        String name = (account != null) ? account.getDisplayName() : "User not logged in";
        ((TextView)findViewById(R.id.tv_result1)).setText(name);
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
