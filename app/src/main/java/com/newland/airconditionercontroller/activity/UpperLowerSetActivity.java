package com.newland.airconditionercontroller.activity;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;
import com.newland.airconditionercontroller.Constant;
import com.newland.airconditionercontroller.R;
import com.newland.airconditionercontroller.base.BaseActivity;
import com.newland.airconditionercontroller.bean.DeviceInfo;
import com.newland.airconditionercontroller.utils.DataCache;
import com.newland.airconditionercontroller.utils.LogUtil;
import com.newland.airconditionercontroller.utils.SPHelper;

import org.json.JSONException;
import org.json.JSONObject;

import cn.com.newland.nle_sdk.responseEntity.base.BaseResponseEntity;
import cn.com.newland.nle_sdk.util.NetWorkBusiness;

public class UpperLowerSetActivity extends BaseActivity implements View.OnClickListener{
    private final static String TAG = "UpperLowerSetActivity";

    private EditText mUpperText, mLowerText;
    private Button mSaveBtn, mCancleBtn;

    private NetWorkBusiness mNetWorkBusiness;
    private SPHelper spHelper;
    private String mDeviceId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upperlowerset);
        mNetWorkBusiness = new NetWorkBusiness(DataCache.getAccessToken(getApplicationContext()), DataCache.getBaseUrl(getApplicationContext()));
        spHelper = SPHelper.getInstant(getApplicationContext());
        mDeviceId = spHelper.getStringFromSP(getApplicationContext(), Constant.DEVICE_ID);
        initView();
        initEvent();
    }

    private void initView() {
        initHeadView();
        setHeadVisable(true);
        initLeftTitleView("返回");
        setLeftTitleView(true);
        initTitleView("上限/下限值设置");
        setRithtTitleViewVisable(false);
        setRithtSettingVisable(false);

        mUpperText = findViewById(R.id.upper_text);
        mLowerText = findViewById(R.id.lower_text);
        mSaveBtn = findViewById(R.id.save);
        mCancleBtn = findViewById(R.id.cancle);

        mUpperText.setText(spHelper.getStringFromSPDef(getApplicationContext(), Constant.UPPER_VALUE, Constant.UPPER_LOWER_DEFAULT_VALUE));
        mLowerText.setText(spHelper.getStringFromSPDef(getApplicationContext(), Constant.LOWER_VALUE, Constant.UPPER_LOWER_DEFAULT_VALUE));
    }

    private void initEvent() {
        mSaveBtn.setOnClickListener(this);
        mCancleBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.save:
                saveSetting();
                break;
            case R.id.cancle:
                finish();
                break;
                default:
                    break;
        }
    }

    private void saveSetting() {
        String upperValue = mUpperText.getText().toString().trim();
        String lowerValue = mLowerText.getText().toString().trim();

        if ("".equals(upperValue)) {
            Toast.makeText(getApplicationContext(), "请填写上限值", Toast.LENGTH_LONG).show();
            return;
        }
        if ("".equals(lowerValue)) {
            Toast.makeText(getApplicationContext(), "请填写下限值", Toast.LENGTH_LONG).show();
            return;
        }
        if (Integer.valueOf(upperValue) < Integer.valueOf(lowerValue)) {
            Toast.makeText(getApplicationContext(), "上限值必须大于下限值,请重新设置", Toast.LENGTH_LONG).show();
            return;
        }
        controlUperLimit(upperValue);
        controlLowerLimit(lowerValue);

        Toast.makeText(getApplicationContext(), "设置完成", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void controlUperLimit(final String uperValue) {
        final Gson gson = new Gson();
        mNetWorkBusiness.control(mDeviceId, DeviceInfo.apiTagUpperLimitCtrl, uperValue, new retrofit2.Callback<BaseResponseEntity>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<BaseResponseEntity> call, @NonNull retrofit2.Response<BaseResponseEntity> response) {
                BaseResponseEntity baseResponseEntity = response.body();
                LogUtil.d(TAG, "Control Upper, gson.toJson(baseResponseEntity):" + gson.toJson(baseResponseEntity));

                if (baseResponseEntity != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(gson.toJson(baseResponseEntity));
                        int status = (int) jsonObject.get("Status");
                        LogUtil.d(TAG, "Control Upper, Status:" + status);
                        if (0 == status) {
                            LogUtil.d(TAG, "Control Upper success");
                        } else {
                            LogUtil.d(TAG, "return status value is error, Control Upper fail");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    LogUtil.d(TAG, "请求出错 : 请求参数不合法或者服务出错");
                }
            }

            @Override
            public void onFailure(@NonNull retrofit2.Call<BaseResponseEntity> call, @NonNull Throwable t) {
                LogUtil.d(TAG, "请求出错 : \n" + t.getMessage());
            }
        });
    }

    private void controlLowerLimit(final String lowerValue) {
        final Gson gson = new Gson();
        mNetWorkBusiness.control(mDeviceId, DeviceInfo.apiTagLowerLimitCtrl, lowerValue, new retrofit2.Callback<BaseResponseEntity>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<BaseResponseEntity> call, @NonNull retrofit2.Response<BaseResponseEntity> response) {
                BaseResponseEntity baseResponseEntity = response.body();
                LogUtil.d(TAG, "Control Lower, gson.toJson(baseResponseEntity):" + gson.toJson(baseResponseEntity));

                if (baseResponseEntity != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(gson.toJson(baseResponseEntity));
                        int status = (int) jsonObject.get("Status");
                        LogUtil.d(TAG, "Control Lower, Status:" + status);
                        if (0 == status) {
                            LogUtil.d(TAG, "Control Lower success");
                        } else {
                            LogUtil.d(TAG, "return status value is error, Control Lower fail");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    LogUtil.d(TAG, "请求出错 : 请求参数不合法或者服务出错");
                }
            }

            @Override
            public void onFailure(@NonNull retrofit2.Call<BaseResponseEntity> call, @NonNull Throwable t) {
                LogUtil.d(TAG, "请求出错 : \n" + t.getMessage());
            }
        });
    }
}
