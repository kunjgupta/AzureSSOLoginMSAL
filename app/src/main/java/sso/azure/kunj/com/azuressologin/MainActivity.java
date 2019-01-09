package sso.azure.kunj.com.azuressologin;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.AuthenticationResult;
import com.microsoft.identity.client.MsalClientException;
import com.microsoft.identity.client.MsalException;
import com.microsoft.identity.client.MsalServiceException;
import com.microsoft.identity.client.MsalUiRequiredException;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.User;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import sso.azure.kunj.com.azuressologin.api.MicrosoftGraphApiClient;
import sso.azure.kunj.com.azuressologin.api.MicrosoftGraphApiInterface;
import sso.azure.kunj.com.azuressologin.pojo.UserDetails;

public class MainActivity extends AppCompatActivity {

    /* Azure AD Variables */
    private PublicClientApplication sampleApp;
    private Button mGetGraphApibutton;
    private Button mOpenLinkWebview;
    private TextView mResultTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initialSetUp();

        mGetGraphApibutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onCallGraphClicked();
            }
        });

        mOpenLinkWebview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = "https://portal.azure.com/";
                CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder()
                        .setToolbarColor(ContextCompat.getColor(MainActivity.this, R.color.colorPrimary))
                        .setCloseButtonIcon(BitmapFactory.decodeResource(MainActivity.this.getResources(), R.drawable.baseline_arrow_back_black_18dp))
                        .setShowTitle(true);

                CustomTabsIntent customTabsIntent = builder.build();
                customTabsIntent.launchUrl(MainActivity.this, Uri.parse(url));
            }
        });

    }

    /* Use MSAL to acquireToken for the end-user
    * Callback will call Graph api w/ access token & update UI
    */
    private void onCallGraphClicked() {
        sampleApp.acquireToken(this, Constants.SCOPES, getAuthInteractiveCallback());
    }

    /* Handles the redirect from the System Browser */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        sampleApp.handleInteractiveRequestRedirect(requestCode, resultCode, data);
    }

    /* Callback method for acquireTokenSilent calls
    * Looks if tokens are in the cache (refreshes if necessary and if we don't forceRefresh)
    * else errors that we need to do an interactive request.
    */
    private AuthenticationCallback getAuthSilentCallback() {
        return new AuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                /* Successfully got a token, call Graph now */

                /* Store the authResult */
                String idToken = authenticationResult.getIdToken();
                String accessToken = authenticationResult.getAccessToken();

                // Print tokens.
                Log.e(Constants.TAG, "ID Token: " + idToken);
                Log.e(Constants.TAG, "Access Token: " + accessToken);
                PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().putString(Constants.PREFS_ACCESS_TOKEN, accessToken).apply();
            }

            @Override
            public void onError(MsalException exception) {
            /* Failed to acquireToken */
                Log.d(Constants.TAG, "Authentication failed: " + exception.toString());

                if (exception instanceof MsalClientException) {
                /* Exception inside MSAL, more info inside MsalError.java */
                } else if (exception instanceof MsalServiceException) {
                /* Exception when communicating with the STS, likely config issue */
                } else if (exception instanceof MsalUiRequiredException) {
                /* Tokens expired or no session, retry with interactive */
                }
            }

            @Override
            public void onCancel() {
            /* User cancelled the authentication */
                Log.d(Constants.TAG, "User cancelled login.");
            }
        };
    }

    /* Callback used for interactive request.  If succeeds we use the access
        * token to call the Microsoft Graph. Does not check cache
        */
    private AuthenticationCallback getAuthInteractiveCallback() {
        return new AuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                /* Successfully got a token, call graph now */
                Log.e(Constants.TAG, "Successfully authenticated");
                Log.e(Constants.TAG, "ID Token: " + authenticationResult.getIdToken());

                PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().putString(Constants.PREFS_ACCESS_TOKEN, authenticationResult.getAccessToken()).apply();

                mOpenLinkWebview.setVisibility(View.VISIBLE);

                callGraphAPI();
            }

            @Override
            public void onError(MsalException exception) {
            /* Failed to acquireToken */
                Log.d(Constants.TAG, "Authentication failed: " + exception.toString());

                if (exception instanceof MsalClientException) {
                /* Exception inside MSAL, more info inside MsalError.java */
                } else if (exception instanceof MsalServiceException) {
                /* Exception when communicating with the STS, likely config issue */
                }
            }

            @Override
            public void onCancel() {
            /* User cancelled the authentication */
                Log.d(Constants.TAG, "User cancelled login.");
            }
        };
    }

    private void initialSetUp() {
        mGetGraphApibutton = findViewById(R.id.get_graph_api_button);
        mOpenLinkWebview = findViewById(R.id.webview_button);
        mResultTextView = findViewById(R.id.results_textview);

        mOpenLinkWebview.setVisibility(View.GONE);

        /* Configure your sample app and save state for this activity */
        sampleApp = null;
        if (sampleApp == null) {
            sampleApp = new PublicClientApplication(
                    MainActivity.this,
                    Constants.CLIENT_ID);
        }

        /* Attempt to get a user and acquireTokenSilent
        * If this fails we do an interactive request
        */
        List<User> users = null;

        try {
            users = sampleApp.getUsers();

            if (users != null && users.size() == 1) {
                /* We have 1 user */
                sampleApp.acquireTokenSilentAsync(Constants.SCOPES, users.get(0), getAuthSilentCallback());
            } else {
                /* We have no user */
                /* Let's do an interactive request */
                sampleApp.acquireToken(MainActivity.this, Constants.SCOPES, getAuthInteractiveCallback());
            }
        } catch (MsalClientException e) {
            Log.d(Constants.TAG, "MSAL Exception Generated while getting users: " + e.toString());

        } catch (IndexOutOfBoundsException e) {
            Log.d(Constants.TAG, "User at this position does not exist: " + e.toString());
        }
    }

    private void callGraphAPI() {
        MicrosoftGraphApiInterface graphApiInterface = MicrosoftGraphApiClient.getClient().create(MicrosoftGraphApiInterface.class);

        Call<UserDetails> call = graphApiInterface.me(PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getString(Constants.PREFS_ACCESS_TOKEN, ""));
        call.enqueue(new Callback<UserDetails>() {
            @Override
            public void onResponse(Call<UserDetails> call, Response<UserDetails> response) {
                Log.d(Constants.TAG,"Response code: "+response.code());
                Log.d(Constants.TAG,"Response body: "+response.body());

                UserDetails details = response.body();

                mResultTextView.setText(details.toString());
            }

            @Override
            public void onFailure(Call<UserDetails> call, Throwable t) {
                call.cancel();
            }
        });

    }
}