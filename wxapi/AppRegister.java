package com.sf.cargoowner.wxapi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

public class AppRegister extends BroadcastReceiver {

   @Override
   public void onReceive(Context context, Intent intent) {
      final IWXAPI api = WXAPIFactory.createWXAPI(context, "wxeb4865c134822398", false);
      api.registerApp("wxeb4865c134822398");
   }
}