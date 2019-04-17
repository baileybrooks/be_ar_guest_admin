package com.example.android.bearguestmobile;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;

import static com.firebase.ui.auth.util.FirebaseAuthError.ERROR_WRONG_PASSWORD;

public class LoginFragment extends Fragment {
    private View loginFragmentView;
    private EditText password;
    private EditText email;
    private Button button_create;
    private Button button_login;
    private TextView text_error_message;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseUIActivity activity;

    public LoginFragment() {
        // Required empty fragment constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        loginFragmentView = inflater.inflate(R.layout.fragment_login_user, container, false);
        mAuth = FirebaseAuth.getInstance();

        email = (EditText) loginFragmentView.findViewById(R.id.input_username);
        password =(EditText) loginFragmentView.findViewById(R.id.input_password);
        button_create = (Button) loginFragmentView.findViewById(R.id.button_create);
        button_login = (Button) loginFragmentView.findViewById(R.id.button_login);
        text_error_message = (TextView) loginFragmentView.findViewById(R.id.text_error_msg);

        activity = ((FirebaseUIActivity)getActivity());
        if(activity==null) {
            Log.v("Firebase err", "activity is null");
            text_error_message.setText(getString(R.string.login_err_activity_null));
            return loginFragmentView;
        }

        addButtonListeners();
        hideKeyboardOnUnfocus();

        return loginFragmentView;
    }

    public void LoginUser() {
        String sEmail = email.getText().toString().trim();
        String sPassword = password.getText().toString().trim();

        // Verify user has provided an email and password
        if(sEmail.equals("")){
            email.setHintTextColor(ContextCompat.getColor(getContext(), R.color.colorErrorRed));
        }

        if(sPassword.equals("")) {
            password.setHintTextColor(ContextCompat.getColor(getContext(), R.color.colorErrorRed));
        }

        if(sPassword.equals("") || sEmail.equals("")) {
            text_error_message.setText(getString(R.string.login_err_missing_fields));
            return;
        }

        // sEmail and sPassword are non-empty, try to login and handle auth errors
        mAuth.signInWithEmailAndPassword(sEmail, sPassword)
                .addOnCompleteListener(activity, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        // String thisUser = mAuth.getCurrentUser().getProviderId();
                        //Profile user = UserRepository.getInstance().getUser(thisUser).getValue();

                        if (task.isSuccessful()) {
                            // check if user is admin or not
                            // if (user != null) {
                                // ^ && user.getRole == "admin"
                                // Toast.makeText((FirebaseUIActivity)getActivity(), "User role: " + user.getRole(), Toast.LENGTH_SHORT).show();
                                currentUser = mAuth.getCurrentUser();
                                activity.finish();
                                startActivity(new Intent(activity.getApplicationContext(),
                                        MainActivity.class));
                            // }
                        } else {
                            Log.v("mAuth signin", "LoginUserWithEmail:Fail", task.getException());

                            try {
                                throw task.getException();
                            } catch(FirebaseAuthWeakPasswordException e) {
                                text_error_message.setText(getString(R.string.login_err_invalid_password));
                            } catch(FirebaseAuthException e) {
                                switch (e.getErrorCode()) {
                                    case "ERROR_WRONG_PASSWORD":
                                        text_error_message.setText(getString(R.string.login_err_invalid_password));
                                        return;
                                    case "ERROR_INVALID_EMAIL":
                                        text_error_message.setText(getString(R.string.login_err_invalid_email));
                                        return;
                                    case "ERROR_USER_NOT_FOUND":
                                        text_error_message.setText(getString(R.string.login_err_user_not_found));
                                        return;
                                    default:
                                        text_error_message.setText("Login Error: " + task.getException().getMessage());
                                }
                            }
                            catch(Exception e) {
                                Log.v("mAuth", e.getMessage());
                                text_error_message.setText("Error: " + e.getMessage());
                            }
                        }
                    }
                });
    }

    private void addButtonListeners() {
        button_create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v == button_create){
                    FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
                    CreateUserFragment createUserFragment = new CreateUserFragment();
                    transaction
                            .replace(R.id.login_container, createUserFragment)
                            .addToBackStack(null)
                            .commit();
                }
            }
        });
        button_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v == button_login){
                    LoginUser();
                }
            }
        });
    }

    // TODO: Figure out how to keep keyboard open when going from email input to password input
    private void hideKeyboardOnUnfocus() {

        // Close keyboard when the user clicks on anything other than the EditText
        ((EditText) loginFragmentView
            .findViewById(R.id.input_username))
            .setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (!hasFocus) {
                        hideKeyboardFrom((FirebaseUIActivity)getActivity(), loginFragmentView);
                    }
                }
            });

        ((EditText) loginFragmentView
            .findViewById(R.id.input_password))
            .setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (!hasFocus) {
                        hideKeyboardFrom((FirebaseUIActivity)getActivity(), loginFragmentView);
                    }
                }
            });
    }

    public static void hideKeyboardFrom(Context context, View view) {
        if(context==null)
            return;
        // Must use Input Method Manager to close the keyboard
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
