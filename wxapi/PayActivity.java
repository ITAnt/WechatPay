package com.sf.cargoowner.wxapi;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.alipay.sdk.app.PayTask;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sf.alipay.PayResult;
import com.sf.cargoowner.R;
import com.sf.cargoowner.base.SFBaseActivity;
import com.sf.cargoowner.constants.Constants;
import com.sf.cargoowner.ui.NetworkInterceptor;
import com.sf.cargoowner.ui.UserInfoKeeper;
import com.sf.cargoowner.ui.order.entity.PayBean;
import com.sf.cargoowner.ui.order.entity.WXPayBean;
import com.sf.log.SFLog;
import com.sf.novate.BaseSubscriber;
import com.sf.novate.Novate;
import com.sf.novate.NovateResponse;
import com.sf.novate.Throwable;
import com.sf.toast.SFToast;
import com.sf.utils.Md5Utils;
import com.tencent.mm.opensdk.constants.ConstantsAPI;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelpay.PayReq;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.TreeMap;

import okhttp3.ResponseBody;

public class PayActivity extends SFBaseActivity {

    private RadioButton wx_rb;
    private RadioButton ali_rb;
    private TextView pay;
    private TextView order_price;
    private String orderPrice;
    private String orderNo;
    private Novate novate;
    private IWXAPI api;

    private static final int SDK_PAY_FLAG = 1;
    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @SuppressWarnings("unused")
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SDK_PAY_FLAG: {
                    @SuppressWarnings("unchecked")
                    PayResult payResult = new PayResult((Map<String, String>) msg.obj);
                    /**
                     对于支付结果，请商户依赖服务端的异步通知结果。同步通知结果，仅作为支付结束的通知。
                     */
                    String resultInfo = payResult.getResult();// 同步返回需要验证的信息
                    String resultStatus = payResult.getResultStatus();
                    // 判断resultStatus 为9000则代表支付成功
                    if (TextUtils.equals(resultStatus, "9000")) {
                        // 该笔订单是否真实支付成功，需要依赖服务端的异步通知。
                        SFToast.showShort(PayActivity.this, "支付成功");
                    } else {
                        // 该笔订单真实的支付结果，需要依赖服务端的异步通知。
                        Toast.makeText(PayActivity.this, "支付失败", Toast.LENGTH_SHORT).show();
                    }
                    break;
                }

                default:
                    break;
            }
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pay);
        initBackTitle("支付");
        api = WXAPIFactory.createWXAPI(this, "wxeb4865c134822398", false);
        // 将该app注册到微信
        api.registerApp("wxeb4865c134822398");


        orderPrice = getIntent().getStringExtra("orderPrice");
        orderNo = getIntent().getStringExtra("orderNo");
        wx_rb = (RadioButton) findViewById(R.id.wx_rb);
        ali_rb = (RadioButton) findViewById(R.id.ali_rb);
        pay = (TextView) findViewById(R.id.pay);
        order_price = (TextView) findViewById(R.id.order_price);

        order_price.setText("订单支付价格:" + "¥" + orderPrice);

        pay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (orderPrice.equals("0.0")) {
                    SFToast.showLong(PayActivity.this, "您支付的金额为零");
                    return;
                }
                if (ali_rb.isChecked()) {
                    pay(orderNo, 1);
                } else if (wx_rb.isChecked()) {
                    pay(orderNo, 2);
                } else {
                    SFToast.showLong(PayActivity.this, "请您选择支付方式");
                    return;
                }

            }
        });
    }

    private void to_pay_wx(WXPayBean wxPayBean) {
        PayReq request = new PayReq();
        request.appId = "wxeb4865c134822398";
        request.partnerId = wxPayBean.getPartnerid();
        request.prepayId = wxPayBean.getPrepayid();
        request.packageValue = "Sign=WXPay";
        request.nonceStr = wxPayBean.getNoncestr();
        request.timeStamp = wxPayBean.getTimestamp();
        request.sign = wxPayBean.getSign();
        SFLog.e(request.appId + "&" + request.partnerId + "&" + request.packageValue + "&" + request.nonceStr + "&" + request.timeStamp + "&" + request.sign);
        api.sendReq(request);
    }


    private void to_pay_ali(final String orderInfo) {
        Runnable payRunnable = new Runnable() {

            @Override
            public void run() {
                PayTask alipay = new PayTask(PayActivity.this);
//                String a = OrderInfoUtil2_0.getOrderInfo();
                Map<String, String> result = alipay.payV2(orderInfo, true);
                Log.i("msp", result.toString());

                Message msg = new Message();
                msg.what = SDK_PAY_FLAG;
                msg.obj = result;
                mHandler.sendMessage(msg);
            }
        };

        Thread payThread = new Thread(payRunnable);
        payThread.start();

    }

    private void pay(String order, final int pay_type) {
        novate = new Novate.Builder(this)
                .baseUrl(com.sf.sdk.constants.Constants.BASE_URL)
                .addInterceptor(new NetworkInterceptor(this))
                .addTrustAllSSL()
                .build();

        TreeMap<String, Object> parameters = new TreeMap<>();
        parameters.putAll(Constants.getCommonParameters(this));
        parameters.put("token", UserInfoKeeper.getToken());
        parameters.put("orderNo", order);
        if (pay_type == 1) {
            parameters.put("payType", "ALIPAY");
        } else {
            parameters.put("payType", "WXPAY");
        }
        parameters = Md5Utils.removeEmptyParameters(parameters);
        parameters.put("sign", Md5Utils.getSign(parameters));
        novate.post("aircap/order/getPaySign.shtml", parameters, new BaseSubscriber<ResponseBody>(this) {
            @Override
            public void onNext(ResponseBody responseBody) {
                try {
                    String s = new String(responseBody.bytes());
                    if (pay_type == 1) {
                        Type type = new TypeToken<NovateResponse<PayBean>>() {
                        }.getType();
                        NovateResponse<PayBean> response = new Gson().fromJson(s, type);
                        PayBean payBean = response.getData();

                        if (response.isResult()) {
                            to_pay_ali(payBean.getOrderSignInfo());
                        }
                    } else {
                        Type type1 = new TypeToken<NovateResponse<WXPayBean>>() {
                        }.getType();
                        NovateResponse<WXPayBean> response1 = new Gson().fromJson(s, type1);
                        WXPayBean payBean1 = response1.getData();
                        if (response1.isResult()) {
                            to_pay_wx(payBean1);
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Throwable e) {
                SFLog.d("response==>>" + "");
            }
        });
    }
}
