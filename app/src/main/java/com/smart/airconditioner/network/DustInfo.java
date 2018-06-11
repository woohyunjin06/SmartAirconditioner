package com.smart.airconditioner.network;


import android.content.Context;

import com.smart.airconditioner.R;

public class DustInfo {

    private Context context;
    private String API_ID;

    public DustInfo(Context context){
        this.context = context;
        API_ID = context.getString(R.string.api_id);
    }
    public String getCurrentDust(){

        return null;
    }
}
