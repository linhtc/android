package com.linhtek.linhomes;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
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

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;

public class LoginActivity extends AppCompatActivity {
    FirebaseDatabase database;
    DatabaseReference myRef;
    ValueEventListener mListener;
    DBHelper db;
    private static final String TAG = "LoginActivity";
    private static final int REQUEST_SIGNUP = 0;
    boolean logged = false;

    @Bind(R.id.input_user) EditText _emailText;
    @Bind(R.id.input_password) EditText _passwordText;
    @Bind(R.id.btn_login) Button _loginButton;
    @Bind(R.id.link_signup) TextView _signupLink;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);
        
        _loginButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                login();
            }
        });

        _signupLink.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Start the Signup activity
                Intent intent = new Intent(getApplicationContext(), SignupActivity.class);
                startActivityForResult(intent, REQUEST_SIGNUP);
                finish();
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
            }
        });

        db = new DBHelper(getBaseContext());
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("users");
        mListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try{
                    if(dataSnapshot.hasChildren()){
                        Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                        Log.e("FCM", "response hasChildren ==========> "+map.toString());
                        for (Map.Entry<String, Object> entry : map.entrySet()) {
//                            String key = entry.getKey();
                            Object itemUser = entry.getValue();
                            if(!itemUser.toString().isEmpty()){
                                JSONObject user = new JSONObject(itemUser.toString());
                                Log.e("FCM", "response user ==========> "+user.toString());
                                if(user.has("name") && user.has("phone") && user.has("pw")){
                                    Log.e("FCM", "response user has ==========> "+user.getString("phone"));
                                    if(!db.checkExistUser(user.getString("phone"))){
                                        Log.e("FCM", "response insertUser ==========> "+user.getString("phone"));
                                        db.insertUser(user.getString("name"), user.getString("phone"), user.getString("pw"), 0);
                                    }
                                }
                            }
                        }
//                        Map<String, Object> childUpdates = new HashMap<>();
//                        childUpdates.put("name", name);
//                        childUpdates.put("pw", pw);
//                        childUpdates.put("phone", dataSnapshot.getKey());
//                        myRef.updateChildren(childUpdates);
//                        myRef.removeEventListener(mListener);
//                        DBHelper db = new DBHelper(getBaseContext());
//                        db.insertUser(name, ph, pw);
//                        Log.e("FCM", "update database OK");
                    }
                    Log.e("FCM", "response dataSnapshot ==========> "+dataSnapshot.getValue().toString());
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

    }

    public void login() {
        Log.e(TAG, "Login");

        if (!validate()) {
            onLoginFailed();
            return;
        }

        logged = false;
        _loginButton.setEnabled(false);

        final ProgressDialog progressDialog = new ProgressDialog(LoginActivity.this, R.style.AppTheme_Dark_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Authenticating...");
        progressDialog.show();

        String email = _emailText.getText().toString();
        String password = _passwordText.getText().toString();

        DBHelper db = new DBHelper(getBaseContext());
        Cursor user = db.getUser(email, password);
        if(user.moveToFirst()){
            Log.e("LoginActivity", "============> moveToFirst true");
            logged = true;
            db.updateUser(email, 1);
        }
        user.close();

        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        // On complete call either onLoginSuccess or onLoginFailed
                        if(logged){
                            onLoginSuccess();
                        } else{
                             onLoginFailed();
                        }
                        progressDialog.dismiss();
                    }
                }, 3000);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SIGNUP) {
            if (resultCode == RESULT_OK) {
                this.finish();
            }
        }
    }

    @Override
    public void onBackPressed() {
        // Disable going back to the MainActivity
        moveTaskToBack(true);
    }

    public void onLoginSuccess() {
        _loginButton.setEnabled(true);
        finish();
    }

    public void onLoginFailed() {
        Toast.makeText(getBaseContext(), "Login failed", Toast.LENGTH_LONG).show();

        _loginButton.setEnabled(true);
    }

    public boolean validate() {
        boolean valid = true;

        String email = _emailText.getText().toString();
        String password = _passwordText.getText().toString();

        if (password.isEmpty()) {
            _emailText.setError(getResources().getString(R.string.required));
            valid = false;
        } else {
            _emailText.setError(null);
        }

        if (password.isEmpty()) {
            _passwordText.setError(getResources().getString(R.string.required));
            valid = false;
        } else {
            _passwordText.setError(null);
        }

        return valid;
    }
}
