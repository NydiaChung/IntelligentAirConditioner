package com.newland.airconditionercontroller.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.newland.airconditionercontroller.AirConditionerControllerApplication;
import com.newland.airconditionercontroller.Constant;
import com.newland.airconditionercontroller.R;
import com.newland.airconditionercontroller.base.BaseActivity;
import com.newland.airconditionercontroller.bean.DeviceInfo;
import com.newland.airconditionercontroller.utils.DataCache;
import com.newland.airconditionercontroller.utils.LogUtil;
import com.newland.airconditionercontroller.utils.SPHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import cn.com.newland.nle_sdk.responseEntity.Device;
import cn.com.newland.nle_sdk.responseEntity.DeviceState;
import cn.com.newland.nle_sdk.responseEntity.base.BaseResponseEntity;
import cn.com.newland.nle_sdk.util.NetWorkBusiness;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends BaseActivity {

    private static String TAG = "MainActivity";

    private TextView mUperLimitText, mUperLimitTitle, mLowerLimitText, mLowerLimitTitle, mCurrentTempText, mCurrentTempTextTitle, mOnlineText;
    private ImageView mAirStateImageView;
    private LinearLayout mOnlineLayout, mCurrentTempLayout;

    private int mAlarmMaxValue = 30;
    private int mUpperLimitValue = 0;
    private int mLowerLimitValue = 0;
    private int mTempValue = 0;
    private static final int GET_REMOTE_INFO = 101;
    private static final int GET_REMOTE_INFO_DELAY = 1000;
    private String mDeviceId;
    private SPHelper spHelper;
    private NetWorkBusiness mNetWorkBusiness;
    private boolean isDeviceExist = false;
    private boolean isDeviceOnLine = false;
    private boolean isPowerOn = false;
    private boolean isAlarmStatus = false;
    private int mBlueColor, mGrayColor, mAlarmColor;
    private Context mContext;

    @SuppressLint("HandlerLeak")
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == GET_REMOTE_INFO) {
                queryRemoteInfo();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        spHelper = SPHelper.getInstant(getApplicationContext());
        mDeviceId = spHelper.getStringFromSP(getApplicationContext(), Constant.DEVICE_ID);
        mNetWorkBusiness = new NetWorkBusiness(DataCache.getAccessToken(getApplicationContext()), DataCache.getBaseUrl(getApplicationContext()));
        mAlarmMaxValue = Integer.valueOf(spHelper.getStringFromSPDef(AirConditionerControllerApplication.getInstance(), Constant.ALARM_MAX_VALUE, Constant.ALARM_MAX_VALUE_DEFAULT_VALUE));
        initView();
        initEvent();
        getDeviceInfo();
//        queryRemoteInfo();
    }

    private void initView() {
        initHeadView();
        setHeadVisable(true);
        initTitleView(this.getString(R.string.app_title));
        setRithtTitleViewVisable(false);
        setRithtSettingVisable(true);

        mCurrentTempLayout = (LinearLayout) findViewById(R.id.currentTemp_layout);
        mCurrentTempTextTitle = (TextView) findViewById(R.id.currentTemp_title);
        mCurrentTempText = (TextView) findViewById(R.id.currentTemp_text);
        mUperLimitText = (TextView) findViewById(R.id.upper_text);
        mUperLimitTitle = (TextView) findViewById(R.id.upper_title);
        mLowerLimitText = (TextView) findViewById(R.id.lower_text);
        mLowerLimitTitle = (TextView) findViewById(R.id.lower_title);
        mAirStateImageView = (ImageView) findViewById(R.id.switch_image_view);
        mAirStateImageView.setTag(false);
        mOnlineLayout = (LinearLayout)findViewById(R.id.online_layout);
        mOnlineText = (TextView)findViewById(R.id.online_text);

        mBlueColor = getResources().getColor(R.color.textColor_blue);
        mGrayColor = getResources().getColor(R.color.text_gray);
        mAlarmColor = getResources().getColor(R.color.alarm_textColor);
    }

    @Override
    protected void setRightSetting() {
        super.setRightSetting();
        Intent intent = new Intent(MainActivity.this, UpperLowerSetActivity.class);
        startActivity(intent);
    }

    private void initEvent() {
        mAirStateImageView.setOnClickListener(new ControlPowerListener());
    }

    class ControlPowerListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (!isDeviceExist) {
                LogUtil.d(TAG, "Device is not exit");
                Toast.makeText(mContext, "设备不存在，请确认", Toast.LENGTH_LONG).show();
                return;
            }
            if (!isDeviceOnLine) {
                LogUtil.d(TAG, "Device is off line");
                Toast.makeText(mContext, "设备已离线，请确认", Toast.LENGTH_LONG).show();
                return;
            }
            final int controlValue = (boolean)mAirStateImageView.getTag() == false ? 1 : 0;
            LogUtil.d(TAG, "controlValue: " + controlValue);
            final Gson gson = new Gson();
            //调用命令控制接口
            /* *
             * param String deviceId：设备ID
             * param String apiTag：API标签
             * param Object data：命令值
             * param Callback<BaseResponseEntity> callback回调对象
             * */
            mNetWorkBusiness.control(mDeviceId, DeviceInfo.apiTagPowerCtrl, controlValue, new retrofit2.Callback<BaseResponseEntity>() {
                @Override
                public void onResponse(@NonNull retrofit2.Call<BaseResponseEntity> call, @NonNull retrofit2.Response<BaseResponseEntity> response) {
                    BaseResponseEntity baseResponseEntity = response.body();
                    LogUtil.d(TAG, "control PowerCtrl, gson.toJson(baseResponseEntity):" + gson.toJson(baseResponseEntity));

                    if (baseResponseEntity != null) {
                        try {
                            JSONObject jsonObject = new JSONObject(gson.toJson(baseResponseEntity));
                            int status = (int) jsonObject.get("Status");
                            LogUtil.d(TAG, "control PowerCtrl Status:" + status);
                            if (0 == status) {
                                displayPowerStatus(controlValue);
                            } else {
                                LogUtil.d(TAG, "return status value is error, open box fail");
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

    private void queryRemoteInfo() {
        LogUtil.d(TAG, "queryRemoteInfo");
        final Gson gson = new Gson();

        //查询设备在线状态
        /* *
         * param String deviceId：设备ID
         * param Callback<BaseResponseEntity<List<DeviceState>>> callback回调对象
         * */
        mNetWorkBusiness.getBatchOnLine(mDeviceId, new Callback<BaseResponseEntity<List<DeviceState>>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponseEntity<List<DeviceState>>> call, @NonNull Response<BaseResponseEntity<List<DeviceState>>> response) {
                BaseResponseEntity<List<DeviceState>> baseResponseEntity = response.body();
                if (baseResponseEntity != null) {
                    LogUtil.d(TAG, "get OnLine status success, gson.toJson(baseResponseEntity):" + gson.toJson(baseResponseEntity));
                    boolean value = false;
                    try {
                        JSONObject jsonObject = new JSONObject(gson.toJson(baseResponseEntity));
                        JSONArray resultObj = (JSONArray) jsonObject.get("ResultObj");
                        value = resultObj.getJSONObject(0).getBoolean("IsOnline");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    LogUtil.d(TAG, "get OnLine value:" + String.valueOf(value));
                    displayOnlineState(value);
                } else {
                    LogUtil.d(TAG, "get OnLine status fail");
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponseEntity<List<DeviceState>>> call, @NonNull Throwable t) {
                LogUtil.d(TAG, "get OnLine status error: \n" + t.getMessage());
            }
        });

        //查询单个传感器的最新状态接口
        /* *
         * param String deviceId：设备ID
         * param String apiTag：API标签
         * param Callback<BaseResponseEntity> callback回调对象
         * */
        mNetWorkBusiness.getSensor(mDeviceId, DeviceInfo.apiTagUpperLimit, new retrofit2.Callback<BaseResponseEntity>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<BaseResponseEntity> call, @NonNull retrofit2.Response<BaseResponseEntity> response) {
                BaseResponseEntity baseResponseEntity = response.body();
                LogUtil.d(TAG, "get UpperLimit, gson.toJson(baseResponseEntity):" + gson.toJson(baseResponseEntity));
                try {
                    JSONObject jsonObject = new JSONObject(gson.toJson(baseResponseEntity));
                    JSONObject resultObj = (JSONObject) jsonObject.get("ResultObj");
                    double value = (double) resultObj.get("Value");
                    LogUtil.d(TAG, "get UpperLimit value:" + value);
                    displayUperLimit(Integer.parseInt(new java.text.DecimalFormat("0").format(value)));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(@NonNull retrofit2.Call<BaseResponseEntity> call, @NonNull Throwable t) {
                LogUtil.d(TAG, "get UpperLimit error: \n" + t.getMessage());
            }
        });

        mNetWorkBusiness.getSensor(mDeviceId, DeviceInfo.apiTagLowerLimit, new retrofit2.Callback<BaseResponseEntity>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<BaseResponseEntity> call, @NonNull retrofit2.Response<BaseResponseEntity> response) {
                BaseResponseEntity baseResponseEntity = response.body();
                LogUtil.d(TAG, "get LowerLimit, gson.toJson(baseResponseEntity):" + gson.toJson(baseResponseEntity));
                try {
                    JSONObject jsonObject = new JSONObject(gson.toJson(baseResponseEntity));
                    JSONObject resultObj = (JSONObject) jsonObject.get("ResultObj");
                    double value = (double) resultObj.get("Value");
                    LogUtil.d(TAG, "get LowerLimit value:" + value);
                    displayLowerLimit(Integer.parseInt(new java.text.DecimalFormat("0").format(value)));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(@NonNull retrofit2.Call<BaseResponseEntity> call, @NonNull Throwable t) {
                LogUtil.d(TAG, "get LowerLimit error: \n" + t.getMessage());
            }
        });

        mNetWorkBusiness.getSensor(mDeviceId, DeviceInfo.apiTagCurrentTemp, new retrofit2.Callback<BaseResponseEntity>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<BaseResponseEntity> call, @NonNull retrofit2.Response<BaseResponseEntity> response) {
                BaseResponseEntity baseResponseEntity = response.body();
                LogUtil.d(TAG, "get CurrentTemp, gson.toJson(baseResponseEntity):" + gson.toJson(baseResponseEntity));
                try {
                    JSONObject jsonObject = new JSONObject(gson.toJson(baseResponseEntity));
                    JSONObject resultObj = (JSONObject) jsonObject.get("ResultObj");
                    double value = (double) resultObj.get("Value");
                    LogUtil.d(TAG, "get CurrentTemp value:" + value);
                    displayCurrentTemp(Integer.parseInt(new java.text.DecimalFormat("0").format(value)));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(@NonNull retrofit2.Call<BaseResponseEntity> call, @NonNull Throwable t) {
                LogUtil.d(TAG, "get CurrentTemp error: \n" + t.getMessage());
            }
        });

        mNetWorkBusiness.getSensor(mDeviceId, DeviceInfo.apiTagAlarm, new retrofit2.Callback<BaseResponseEntity>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<BaseResponseEntity> call, @NonNull retrofit2.Response<BaseResponseEntity> response) {
                BaseResponseEntity baseResponseEntity = response.body();
                LogUtil.d(TAG, "get Alarm, gson.toJson(baseResponseEntity):" + gson.toJson(baseResponseEntity));
                try {
                    JSONObject jsonObject = new JSONObject(gson.toJson(baseResponseEntity));
                    JSONObject resultObj = (JSONObject) jsonObject.get("ResultObj");
                    double value = (double) resultObj.get("Value");
                    LogUtil.d(TAG, "get Alarm value:" + value);
                    displayAlarmStatus(Integer.parseInt(new java.text.DecimalFormat("0").format(value)));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(@NonNull retrofit2.Call<BaseResponseEntity> call, @NonNull Throwable t) {
                LogUtil.d(TAG, "get Alarm error: \n" + t.getMessage());
            }
        });

        mNetWorkBusiness.getSensor(mDeviceId, DeviceInfo.apiTagPower, new retrofit2.Callback<BaseResponseEntity>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<BaseResponseEntity> call, @NonNull retrofit2.Response<BaseResponseEntity> response) {
                BaseResponseEntity baseResponseEntity = response.body();
                LogUtil.d(TAG, "get Power, gson.toJson(baseResponseEntity):" + gson.toJson(baseResponseEntity));
                try {
                    JSONObject jsonObject = new JSONObject(gson.toJson(baseResponseEntity));
                    JSONObject resultObj = (JSONObject) jsonObject.get("ResultObj");
                    double value = (double) resultObj.get("Value");
                    LogUtil.d(TAG, "get PowerCtrl value:" + value);
                    displayPowerStatus(Integer.parseInt(new java.text.DecimalFormat("0").format(value)));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(@NonNull retrofit2.Call<BaseResponseEntity> call, @NonNull Throwable t) {
                LogUtil.d(TAG, "get Power error: \n" + t.getMessage());
            }
        });

        mHandler.sendEmptyMessageDelayed(GET_REMOTE_INFO, GET_REMOTE_INFO_DELAY);
        dispalyBlueOrGrayState();
    }

    private void displayUperLimit(int value) {
        mUperLimitText.setText(value + "℃");
        mUpperLimitValue = value;
        spHelper.putData2SP(getApplicationContext(), Constant.UPPER_VALUE, String.valueOf(value));
    }

    private void displayLowerLimit(int value) {
        mLowerLimitText.setText(value + "℃");
        mLowerLimitValue = value;
        spHelper.putData2SP(getApplicationContext(), Constant.LOWER_VALUE, String.valueOf(value));
    }

    private void displayCurrentTemp(int value) {
        LogUtil.d(TAG, "displayCurrentTemp: " + value);
        mCurrentTempText.setText(value + "℃");
        mTempValue = value;
        displayAlarmStatus((value < mAlarmMaxValue) ? 0 : 1);
    }

    private void displayAlarmStatus(int value) {
        LogUtil.d(TAG, "displayAlarmStatus: " + value);
        if (value == DeviceInfo.alarmNomalValue) {
            isAlarmStatus = false;
            mCurrentTempLayout.setBackgroundResource(R.mipmap.control_blue);
            if (mTempValue < mLowerLimitValue) {
                mCurrentTempTextTitle.setText(R.string.heating_text);
            } else if (mTempValue > mUpperLimitValue) {
                mCurrentTempTextTitle.setText(R.string.refrigerating_text);
            } else {
                mCurrentTempTextTitle.setText(R.string.current_temperature_value);
            }

            if (isDeviceOnLine && isPowerOn) {
                mCurrentTempTextTitle.setTextColor(mBlueColor);
                mCurrentTempText.setTextColor(mBlueColor);
            } else {
                mCurrentTempTextTitle.setTextColor(mGrayColor);
                mCurrentTempText.setTextColor(mGrayColor);
            }
        } else if (value == DeviceInfo.alarmUnNomalValue) {
            isAlarmStatus = true;
            mCurrentTempLayout.setBackgroundResource(R.mipmap.control_red);
            mCurrentTempTextTitle.setText(R.string.alarm_temperature);
            mCurrentTempTextTitle.setTextColor(mAlarmColor);
            mCurrentTempText.setTextColor(mAlarmColor);
        }
    }

    private void displayPowerStatus(int control) {
        if (control == DeviceInfo.closePowerValue) {
            displayPowerStatusClose();
        } else if (control == DeviceInfo.openPowerValue) {
            displayPowerStatusOpen();
        }
    }

    private void displayPowerStatusOpen() {
        isPowerOn = true;
        mAirStateImageView.setBackground(getResources().getDrawable(R.mipmap.on));
        mAirStateImageView.setTag(true);
    }

    private void displayPowerStatusClose() {
        isPowerOn = false;
        mAirStateImageView.setBackground(getResources().getDrawable(R.mipmap.off));
        mAirStateImageView.setTag(false);
    }

    private void displayExistState(int status) {
        if (status == 0) {
            isDeviceExist = true;
            mOnlineLayout.setVisibility(View.GONE);
            queryRemoteInfo();
        } else {
            isDeviceExist = false;
            mOnlineLayout.setVisibility(View.VISIBLE);
            mOnlineText.setText(getResources().getString(R.string.device_not_exist_text));
        }
    }

    private void displayOnlineState(boolean status) {
        isDeviceOnLine = status;
        if (!status) {
            mOnlineLayout.setVisibility(View.VISIBLE);
            mOnlineText.setText(getResources().getString(R.string.online_text));
        } else {
            mOnlineLayout.setVisibility(View.GONE);
        }
    }

    private void dispalyBlueOrGrayState() {
        if (!isDeviceExist || !isDeviceOnLine || !isPowerOn) {
            dispalyGrayState();
        } else {
            dispalyBlueState();
        }
    }

    private void dispalyGrayState() {
        mCurrentTempTextTitle.setTextColor(mGrayColor);
        mCurrentTempText.setTextColor(mGrayColor);
        mUperLimitText.setTextColor(mGrayColor);
        mUperLimitTitle.setTextColor(mGrayColor);
        mLowerLimitText.setTextColor(mGrayColor);
        mLowerLimitTitle.setTextColor(mGrayColor);
    }

    private void dispalyBlueState() {
        if (!isAlarmStatus) {
            mCurrentTempTextTitle.setTextColor(mBlueColor);
            mCurrentTempText.setTextColor(mBlueColor);
        }
        mUperLimitText.setTextColor(mBlueColor);
        mUperLimitTitle.setTextColor(mBlueColor);
        mLowerLimitText.setTextColor(mBlueColor);
        mLowerLimitTitle.setTextColor(mBlueColor);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != mHandler) {
            mHandler.removeCallbacksAndMessages(null);
        }
    }

    private void getDeviceInfo() {
        LogUtil.d(TAG, "getDeviceInfo");
        final Gson gson = new Gson();
        mDeviceId = spHelper.getStringFromSP(getApplicationContext(), Constant.DEVICE_ID);
        //查询设备是否存在
        /* *
         * param String deviceId：设备ID
         * param Callback<BaseResponseEntity> callback回调对象
         * */
        mNetWorkBusiness.getDeviceInfo(mDeviceId, new Callback<BaseResponseEntity<Device>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponseEntity<Device>> call, @NonNull Response<BaseResponseEntity<Device>> response) {
                BaseResponseEntity<Device> baseResponseEntity = response.body();
                if (baseResponseEntity != null) {
                    LogUtil.d(TAG, "getDeviceInfo, baseResponseEntity: " + gson.toJson(baseResponseEntity));
                    int status = 1;
                    try {
                        JSONObject jsonObject = new JSONObject(gson.toJson(baseResponseEntity));
                        status = (int) jsonObject.get("Status");
                        displayExistState(status);
                        LogUtil.d(TAG, "getDeviceInfo Status: " + status);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    LogUtil.d(TAG, "getDeviceInfo error");
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponseEntity<Device>> call, @NonNull Throwable t) {
                LogUtil.d(TAG, "getDeviceInfo status error: \n" + t.getMessage());
            }
        });
    }
}
