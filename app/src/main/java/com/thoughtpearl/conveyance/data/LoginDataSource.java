package com.thoughtpearl.conveyance.data;

import android.os.Build;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.thoughtpearl.conveyance.LocationApp;
import com.thoughtpearl.conveyance.api.ApiHandler;
import com.thoughtpearl.conveyance.api.response.LoginRequest;
import com.thoughtpearl.conveyance.api.response.LoginResponse;
import com.thoughtpearl.conveyance.data.model.LoggedInUser;
import com.thoughtpearl.conveyance.ui.login.LoginActivity;
import com.thoughtpearl.conveyance.utility.TrackerUtility;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/*import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;*/

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
public class LoginDataSource {

    @RequiresApi(api = Build.VERSION_CODES.O)
    public Result<LoggedInUser> login(String username, String password) {
        try {
            Call<LoginResponse> loginResponseCall = ApiHandler.getClient().login(username, password, LocationApp.DEVICE_ID);
            loginResponseCall.enqueue(new Callback<LoginResponse>() {
                @Override
                public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                    LoggedInUser fakeUser =
                            new LoggedInUser(
                                    java.util.UUID.randomUUID().toString(),
                                    username,
                                    response.body().isRideDisabled()
                                    );
                }

                @Override
                public void onFailure(Call<LoginResponse> call, Throwable t) {

                }
            });
            // TODO: handle loggedInUser authentication
            LoggedInUser fakeUser =
                    new LoggedInUser(
                            java.util.UUID.randomUUID().toString(),
                            username,
                            true);
            return new Result.Success<>(fakeUser);
        } catch (Exception e) {
            return new Result.Error(new IOException("Error logging in", e));
        }
    }

    public void logout() {
        // TODO: revoke authentication
    }
}