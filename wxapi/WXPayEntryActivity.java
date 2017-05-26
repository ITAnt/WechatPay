package com.sf.cargoowner.wxapi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.sf.toast.SFToast;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

public class WXPayEntryActivity extends Activity implements IWXAPIEventHandler {

    private static final String TAG = "wx";

    private IWXAPI api;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        api = WXAPIFactory.createWXAPI(this, "wxeb4865c134822398");
        api.handleIntent(getIntent(), this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        api.handleIntent(intent, this);
    }

    @Override
    public void onReq(BaseReq req) {}

    @Override
    public void onResp(BaseResp resp) {
        SFToast.showShort(this, "onresponse");
        Log.e(TAG, "wwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwww");

        //resp.getType() == ConstantsAPI.COMMAND_PAY_BY_WX时表示是微信支付返回
        switch (resp.errCode){
            case 0:
                /*Intent in=new Intent();
                in.setClass(WXPayEntryActivity.this, PayFinishedActivity.class);
                startActivity(in);
                finish();*/
                SFToast.showShort(this, "onresponse");
                break;
            case -1:
                Log.d(TAG, "onResp: -1");

                break;
            case -2:
                Log.d(TAG, "onResp: -2");
                break;
        }
    }
}