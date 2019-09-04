package com.newland.airconditionercontroller.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.google.gson.Gson;
import com.newland.airconditionercontroller.Constant;
import com.newland.airconditionercontroller.R;
import com.newland.airconditionercontroller.base.BaseActivity;
import com.newland.airconditionercontroller.utils.DataCache;
import com.newland.airconditionercontroller.utils.LogUtil;
import com.newland.airconditionercontroller.utils.SPHelper;
import com.newland.airconditionercontroller.utils.Utils;

import cn.com.newland.nle_sdk.requestEntity.SignIn;
import cn.com.newland.nle_sdk.responseEntity.User;
import cn.com.newland.nle_sdk.responseEntity.base.BaseResponseEntity;
import cn.com.newland.nle_sdk.util.NetWorkBusiness;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends BaseActivity implements View.OnClickListener {
    private static String TAG = "LoginActivity";
    private EditText etUserName;
    private EditText etPwd;
    private ImageView mShowpswImageView;
    private Boolean mShowPassword = false;

    private SPHelper spHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        spHelper = SPHelper.getInstant(getApplicationContext());
        initView();
        initViewData();
        registerListener();
    }

    private void initView() {
        initHeadView();
        setHeadVisable(true);
        initLeftTitleView("返回");
        setLeftTitleView(true);
        initTitleView("登录");
        setRithtTitleViewVisable(true);

        etUserName = findViewById(R.id.userName);
        etPwd = findViewById(R.id.pwd);
        mShowpswImageView = (ImageView) findViewById(R.id.showpsw_imageView);
    }

    protected void initViewData() {
        etUserName.setText(DataCache.getUserName(getApplicationContext()));
        etPwd.setText(DataCache.getPwd(getApplicationContext()));
    }

    protected void registerListener() {
        findViewById(R.id.signIn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Utils.isFastDoubleClick()) {
                    return;
                }
                signIn();
            }
        });
        mShowpswImageView.setOnClickListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == 2) {
        }
    }

    private void signIn() {
        String platformAddress = spHelper.getStringFromSP(getApplicationContext(), Constant.SETTING_PLATFORM_ADDRESS);
        String port = spHelper.getStringFromSP(getApplicationContext(), Constant.SETTING_PORT);

        final String userName = etUserName.getText().toString();
        final String pwd = etPwd.getText().toString();
        if (TextUtils.isEmpty(platformAddress) || TextUtils.isEmpty(port)) {
            Toast.makeText(getApplicationContext(), "请设置云平台信息", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(userName) || TextUtils.isEmpty(pwd)) {
            Toast.makeText(getApplicationContext(), "用户名或密码不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        //构建SDK对象：负责调用云平台接口进行通信
        NetWorkBusiness netWorkBusiness = null;
        try {
            netWorkBusiness = new NetWorkBusiness( "",DataCache.getBaseUrl(getApplicationContext()));
        } catch (IllegalArgumentException e) {
            LogUtil.d(TAG, "illegal Url: " + e.getMessage());
            Toast.makeText(LoginActivity.this, "Url格式错误,请确认", Toast.LENGTH_SHORT).show();
            return;
        }

        LogUtil.d(TAG, "BaseUrl: " + DataCache.getBaseUrl(getApplicationContext()));
        //进行登录请求操作
        netWorkBusiness.signIn(new SignIn(userName, pwd), new Callback<BaseResponseEntity<User>>() {
            //登录成功返回处理
            @Override
            public void onResponse(@NonNull Call<BaseResponseEntity<User>> call, @NonNull Response<BaseResponseEntity<User>> response) {
                final Gson gson = new Gson();
                //通过返回对象-response，获取返回对象BaseResponseEntity（包含请求的返回信息内容）
                BaseResponseEntity<User> baseResponseEntity = response.body();
                LogUtil.d(TAG, "signIn, baseResponseEntity: " + gson.toJson(baseResponseEntity));
                if (baseResponseEntity != null) {
                    if (baseResponseEntity.getStatus() == 0) {
                        DataCache.updateUserName(getApplicationContext(), userName);
                        DataCache.updatePwd(getApplicationContext(), pwd);
                        //从返回对象中获取Token值，并保存在本地
                        String accessToken = baseResponseEntity.getResultObj().getAccessToken();
                        DataCache.updateAccessToken(getApplicationContext(), accessToken);
                        LogUtil.d(TAG, "signIn, accessToken: " + accessToken);
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        Bundle bundle = new Bundle();
                        bundle.putSerializable("userBaseResponseEntity", baseResponseEntity);
                        intent.putExtras(bundle);
                        startActivity(intent);
                    } else {
                        Toast.makeText(LoginActivity.this, baseResponseEntity.getMsg(), Toast.LENGTH_SHORT).show();

                    }
                } else {
                    Toast.makeText(LoginActivity.this, "请求地址出错", Toast.LENGTH_SHORT).show();
                }
            }

            //登录失败返回处理
            @Override
            public void onFailure(@NonNull Call<BaseResponseEntity<User>> call, @NonNull Throwable t) {
                Toast.makeText(getApplicationContext(), t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.showpsw_imageView:
                if (mShowPassword) {
                    mShowpswImageView.setBackgroundResource(R.mipmap.ico_eye_off);
                    etPwd.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    etPwd.setSelection(etPwd.getText().toString().length());
                    mShowPassword = !mShowPassword;
                } else {
                    mShowpswImageView.setBackgroundResource(R.mipmap.ico_eye_on);
                    etPwd.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    etPwd.setSelection(etPwd.getText().toString().length());
                    mShowPassword = !mShowPassword;
                }
        }
    }

    @Override
    protected void setRightBtn(View tv_mRight) {
        super.setRightBtn(tv_mRight);
        showPopupMenu(tv_mRight);
    }

    private void showPopupMenu(View view) {
        // 这里的view代表popupMenu需要依附的view
        PopupMenu popupMenu = new PopupMenu(LoginActivity.this, view);
        // 获取布局文件
        popupMenu.getMenuInflater().inflate(R.menu.login_menu, popupMenu.getMenu());
        popupMenu.show();
        // 通过上面这几行代码，就可以把控件显示出来了
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // 控件每一个item的点击事件
                int menuId = item.getItemId();
                if (menuId == R.id.settingsMenu) {
                    startActivityForResult(new Intent(getApplicationContext(), SettingActivity.class), 1);
                } else if (menuId == R.id.aboutMenu) {
                    Intent intent = new Intent(LoginActivity.this, AboutActivity.class);
                    startActivity(intent);
                }
                return true;
            }
        });
        popupMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
            @Override
            public void onDismiss(PopupMenu menu) {
                // 控件消失时的事件
            }
        });
    }
}
