package com.android.frontier.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.android.frontier.R;
import com.android.frontier.trip.TripListActivity;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.shashank.sony.fancytoastlib.FancyToast;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SignInActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 111;
    private static final String TAG = SignInActivity.class.getSimpleName();

    @BindView(R.id.sign_in_button)
    private
    Button signInButton;

    private FirebaseAuth mFirebaseAuth;

    public static Intent createIntent(Context context) {
        return new Intent(context, SignInActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        ButterKnife.bind(this);

        mFirebaseAuth = FirebaseAuth.getInstance();

        if (mFirebaseAuth.getCurrentUser() != null) {
            // Start sign in activity
            startActivity(TripListActivity.tripListIntent(this));
            finish();
        }

        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(
                        AuthUI.getInstance().createSignInIntentBuilder()
                                .build(),
                        RC_SIGN_IN);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            handleSignInResponse(resultCode, data);
        }
    }

    @MainThread
    private void handleSignInResponse(int resultCode, Intent data) {
        IdpResponse response = IdpResponse.fromResultIntent(data);
        Log.d(TAG, "handleSignInResponse: " + response);
        // Successfully signed in
        if (resultCode == RESULT_OK) {
            startActivity(TripListActivity.tripListIntent(this));
            finish();
        } else {
            // Sign In Failed
            if (response == null) {
                // User pressed back button
                FancyToast.makeText(this, getString(R.string.sign_in_was_cancelled), FancyToast.LENGTH_LONG, FancyToast.ERROR, false).show();
                return;
            }

            if (response.getErrorCode() == ErrorCodes.NO_NETWORK) {
                FancyToast.makeText(this, getString(R.string.no_internet_connection), FancyToast.LENGTH_LONG, FancyToast.ERROR, false).show();
                return;
            }

            if (response.getErrorCode() == ErrorCodes.UNKNOWN_ERROR) {
                FancyToast.makeText(this, getString(R.string.unknown_error), FancyToast.LENGTH_LONG, FancyToast.ERROR, false).show();
                return;
            }

            FancyToast.makeText(this, getString(R.string.unknown_error), FancyToast.LENGTH_LONG, FancyToast.ERROR, false).show();
        }
    }
}
