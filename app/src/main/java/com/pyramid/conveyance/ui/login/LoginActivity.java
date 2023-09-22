package com.pyramid.conveyance.ui.login;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.pyramid.conveyance.LocationApp;
import com.pyramid.conveyance.ui.navigation.BottomNavigationActivity;
import com.pyramid.conveyance.utility.TrackerUtility;
import com.pyramid.conveyance.R;
import com.pyramid.conveyance.databinding.ActivityLoginBinding;

/*import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;*/

public class LoginActivity extends AppCompatActivity {

    private LoginViewModel loginViewModel;
    private ActivityLoginBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        loginViewModel = new ViewModelProvider(this, new LoginViewModelFactory())
                .get(LoginViewModel.class);

        final EditText usernameEditText = binding.username;
        final EditText passwordEditText = binding.password;
        final Button loginButton = binding.login;
        final ProgressBar loadingProgressBar = binding.progressCircular;

        loginViewModel.getLoginFormState().observe(this, loginFormState -> {
            if (loginFormState == null) {
                return;
            }
            loginButton.setEnabled(loginFormState.isDataValid());
            if (loginFormState.getUsernameError() != null) {
                usernameEditText.setError(getString(loginFormState.getUsernameError()));
            }
            if (loginFormState.getPasswordError() != null) {
                passwordEditText.setError(getString(loginFormState.getPasswordError()));
            }
        });

        loginViewModel.getLoginResult().observe(this, loginResult -> {
            if (loginResult == null) {
                return;
            }
            loadingProgressBar.setVisibility(View.GONE);
            if (loginResult.getError() != null) {
                showLoginFailed(loginResult.getError());
            }

            if (loginResult.getSuccess() != null) {
                SharedPreferences sharedPreferences = this.getSharedPreferences(LocationApp.APP_NAME, MODE_PRIVATE);
                sharedPreferences.edit().putString("username", usernameEditText.getText().toString()).commit();
                sharedPreferences.edit().putString("password", passwordEditText.getText().toString()).commit();
                sharedPreferences.edit().putBoolean("rideDisabled", loginResult.getSuccess().isRideDisabled()).commit();

                LocationApp.USER_NAME = usernameEditText.getText().toString();
                Intent intent = new Intent(LoginActivity.this, BottomNavigationActivity.class);
                startActivity(intent);

                setResult(Activity.RESULT_OK);
                //Complete and destroy login activity once successful
                finish();
            }

        });

        TextWatcher afterTextChangedListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // ignore
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // ignore
            }

            @Override
            public void afterTextChanged(Editable s) {
                loginViewModel.loginDataChanged(usernameEditText.getText().toString(),
                        passwordEditText.getText().toString());
            }
        };
        usernameEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                closeKeyboard();
                if (!TrackerUtility.checkConnection(getApplicationContext())) {
                    Toast.makeText(getApplicationContext(), "Please check your network connection", Toast.LENGTH_LONG).show();
                } else {
                    loginViewModel.login(usernameEditText.getText().toString(),
                            passwordEditText.getText().toString(),
                            TrackerUtility.getDeviceId(getApplicationContext()));
                }
            }
            return false;
        });

        loginButton.setOnClickListener(v -> {
            closeKeyboard();
            if (!TrackerUtility.checkConnection(getApplicationContext())) {
                Toast.makeText(getApplicationContext(), "Please check your network connection", Toast.LENGTH_LONG).show();
            } else {
                    loadingProgressBar.setVisibility(View.VISIBLE);
                loginViewModel.login(usernameEditText.getText().toString(),
                        passwordEditText.getText().toString(),
                        TrackerUtility.getDeviceId(getApplicationContext()));
            }
        });
    }

    private void updateUiWithUser(LoggedInUserView model) {
        String welcome = getString(R.string.welcome) + model.getDisplayName();
        // TODO : initiate successful logged in experience
        Toast.makeText(getApplicationContext(), welcome, Toast.LENGTH_LONG).show();
    }

    private void showLoginFailed(@StringRes Integer errorString) {
        Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_SHORT).show();
    }

    private void closeKeyboard()
    {
        // this will give us the view
        // which is currently focus
        // in this layout
        View view = this.getCurrentFocus();

        // if nothing is currently
        // focus then this will protect
        // the app from crash
        if (view != null) {

            // now assign the system
            // service to InputMethodManager
            InputMethodManager manager
                    = (InputMethodManager)
                    getSystemService(
                            Context.INPUT_METHOD_SERVICE);
            manager
                    .hideSoftInputFromWindow(
                            view.getWindowToken(), 0);
        }
    }
}