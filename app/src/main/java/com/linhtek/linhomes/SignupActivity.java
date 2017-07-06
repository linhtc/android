package com.linhtek.linhomes;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;

public class SignupActivity extends AppCompatActivity {
    private static final String TAG = "SignupActivity";
    FirebaseDatabase database;
    DatabaseReference myRef;
    ValueEventListener mListener;
    boolean created = false;

    @Bind(R.id.input_name) EditText _nameText;
    @Bind(R.id.input_mobile) EditText _mobileText;
    @Bind(R.id.input_password) EditText _passwordText;
    @Bind(R.id.input_reEnterPassword) EditText _reEnterPasswordText;
    @Bind(R.id.btn_signup) Button _signupButton;
    @Bind(R.id.link_login) TextView _loginLink;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        ButterKnife.bind(this);

        _signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signup();
            }
        });

        _loginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Finish the registration screen and return to the Login activity
                Intent intent = new Intent(getApplicationContext(),LoginActivity.class);
                startActivity(intent);
                finish();
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
            }
        });
    }

    public void signup() {
        Log.d(TAG, "Signup");

        if (!validate()) {
            onSignupFailed();
            return;
        }

        created = false;
        _signupButton.setEnabled(false);

        final ProgressDialog progressDialog = new ProgressDialog(SignupActivity.this, R.style.AppTheme_Dark_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Creating Account...");
        progressDialog.show();

        String mobile = _mobileText.getText().toString();
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("users/"+mobile);
        mListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try{
                    if(!dataSnapshot.hasChildren()){
                        created = true;
                        String name = _nameText.getText().toString();
                        String ph = _mobileText.getText().toString();
                        String pw = _passwordText.getText().toString();
                        // create the acc
                        Map<String, Object> childUpdates = new HashMap<>();
                        childUpdates.put("name", name);
                        childUpdates.put("phone", dataSnapshot.getKey());
                        myRef.updateChildren(childUpdates);
                        myRef.removeEventListener(mListener);
                        DBHelper db = new DBHelper(getBaseContext());
                        db.insertUser(name, ph, pw);
                        Log.e("FCM", "update database OK");
                    }
                } catch (Exception e){
                    Log.e("FCM err", e.getMessage());
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("FCM err", "=============> Failed to read value.", error.toException());
            }
        };
        myRef.addValueEventListener(mListener);

        // TODO: Implement your own signup logic here.

        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        // On complete call either onSignupSuccess or onSignupFailed
                        // depending on success
                        progressDialog.dismiss();
                        if(created){
                            onSignupSuccess();
                        } else{
                             onSignupFailed();
                        }
                    }
                }, 3000);
    }


    public void onSignupSuccess() {
        _signupButton.setEnabled(true);
        setResult(RESULT_OK, null);
        finish();
    }

    public void onSignupFailed() {
        Toast.makeText(getBaseContext(), "Login failed", Toast.LENGTH_LONG).show();

        _signupButton.setEnabled(true);
    }

    public boolean validate() {
        boolean valid = true;

        String name = _nameText.getText().toString();
        String mobile = _mobileText.getText().toString();
        String password = _passwordText.getText().toString();
        String reEnterPassword = _reEnterPasswordText.getText().toString();

        if (name.isEmpty()) {
            _nameText.setError(getResources().getString(R.string.required));
            valid = false;
        } else {
            _nameText.setError(null);
        }

        if (mobile.isEmpty()) {
            _mobileText.setError(getResources().getString(R.string.required));
            valid = false;
        } else {
            _mobileText.setError(null);
        }

        if (password.isEmpty()) {
            _passwordText.setError(getResources().getString(R.string.required));
            valid = false;
        } else {
            _passwordText.setError(null);
        }

        if (reEnterPassword.isEmpty() || !(reEnterPassword.equals(password))) {
            _reEnterPasswordText.setError(getResources().getString(R.string.re_pw_wrong));
            valid = false;
        } else {
            _reEnterPasswordText.setError(null);
        }

        return valid;
    }
}