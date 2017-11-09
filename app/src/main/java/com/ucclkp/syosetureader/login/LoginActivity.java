package com.ucclkp.syosetureader.login;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.ucclkp.syosetureader.R;
import com.ucclkp.syosetureader.UApplication;

public class LoginActivity extends AppCompatActivity
{
    private Button mSubmitBT;
    private EditText mUserNameTV;
    private EditText mPasswordTV;
    private ProgressBar mLoginProgressBar;

    private LoginWorker mLoginWorker;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Toolbar toolbar = findViewById(R.id.tb_login_activity);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        mSubmitBT = findViewById(R.id.bt_login_submit);
        mSubmitBT.setOnClickListener(mSubmitClickListener);

        mUserNameTV = findViewById(R.id.et_login_username);
        mPasswordTV = findViewById(R.id.et_login_password);
        mLoginProgressBar = findViewById(R.id.pb_login_progress);

        mLoginWorker = new LoginWorker();
        mLoginWorker.setOnLoginEventListener(mLoginEventListener);

        if (UApplication.cookieManager.isLogined())
        {
            Toast.makeText(LoginActivity.this,
                    "您已登陆", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        mLoginWorker.cancel();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
            {
                finish();
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private void recoverView()
    {
        mSubmitBT.setEnabled(true);
        mUserNameTV.setEnabled(true);
        mPasswordTV.setEnabled(true);
        mLoginProgressBar.setVisibility(View.INVISIBLE);
    }


    private View.OnClickListener mSubmitClickListener
            = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            mSubmitBT.setEnabled(false);
            mUserNameTV.setEnabled(false);
            mPasswordTV.setEnabled(false);
            mLoginProgressBar.setVisibility(View.VISIBLE);

            mLoginWorker.login(
                    mUserNameTV.getText().toString(),
                    mPasswordTV.getText().toString());
        }
    };

    private LoginWorker.OnLoginEventListener mLoginEventListener
            = new LoginWorker.OnLoginEventListener()
    {
        @Override
        public void onLoginSucceed()
        {
            recoverView();

            Toast.makeText(LoginActivity.this,
                    "登陆成功", Toast.LENGTH_SHORT).show();
            UApplication.cookieManager.saveCookieToLocal(LoginActivity.this);
            finish();
        }

        @Override
        public void onLoginFailed(String errMsg)
        {
            recoverView();

            Toast.makeText(LoginActivity.this,
                    errMsg, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onLoginCancelled()
        {
            if (UApplication.cookieManager.isLogined())
                UApplication.cookieManager.saveCookieToLocal(LoginActivity.this);
        }
    };
}

