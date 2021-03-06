package com.example.shanggmiqr.transaction;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.shanggmiqr.Url.iUrl;
import com.example.shanggmiqr.adapter.SaleDeliveryAdapter;
import com.example.shanggmiqr.bean.SaleDeliveryBean;
import com.example.shanggmiqr.util.DataHelper;
import com.example.weiytjiang.shangmiqr.R;
import com.example.shanggmiqr.adapter.PurchaseArrivalAdapter;
import com.example.shanggmiqr.bean.PurchaseArrivalBean;
import com.example.shanggmiqr.bean.PurchaseArrivalQuery;
import com.example.shanggmiqr.util.MyDataBaseHelper;
import com.example.shanggmiqr.util.Utils;
import com.google.gson.Gson;
import com.zyao89.view.zloading.ZLoadingDialog;
import com.zyao89.view.zloading.Z_TYPE;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 采购发货
 */

public class PurchaseArrival extends AppCompatActivity implements OnClickListener {

    private Button downloadDeliveryButton;
    private Button querySaleDeliveryButton;
    private Button displayallSaleDeliveryButton;
    private SQLiteDatabase db3;
    private MyDataBaseHelper helper3;
    private Handler saleDeliveryHandler = null;
    private ListView tableListView;
    private List<PurchaseArrivalBean> listAllPostition;

    private ZLoadingDialog dialog;
    private String query_cwarename;
    private String query_uploadflag;
    private List<String> test;
    private List<String> uploadflag;
    private TextView lst_downLoad_ts;
    private TextView time;
    private Button buttonCheck;
    private Button buttonExport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.purchase_arrival_manage);
        helper3 = new MyDataBaseHelper(PurchaseArrival.this, "ShangmiData", null, 1);
        //创建或打开一个现有的数据库（数据库存在直接打开，否则创建一个新数据库）
        //创建数据库操作必须放在主线程，否则会报错，因为里面有直接加的toast。。。
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(getIntent().getStringExtra("title"));
        }
        lst_downLoad_ts = (TextView)findViewById(R.id.last_downLoad_ts_purchase_arrival);

        //显示最后一次的下载时间
        SharedPreferences latestDBTimeInfo = getSharedPreferences("LatestPurchaseArrivalTSInfo", 0);
        String begintime = latestDBTimeInfo.getString("latest_download_ts_begintime", "");
        lst_downLoad_ts.setText("最后一次下载:"+begintime);

        db3 = helper3.getWritableDatabase();//获取到了 SQLiteDatabase 对象
        dialog = new ZLoadingDialog(PurchaseArrival.this);
        downloadDeliveryButton = (Button) findViewById(R.id.download_purchase_arrival);
        downloadDeliveryButton.setOnClickListener(this);

        querySaleDeliveryButton = (Button) findViewById(R.id.query_purchase_arrival);
        querySaleDeliveryButton.setOnClickListener(this);
        displayallSaleDeliveryButton = (Button) findViewById(R.id.displayall_purchase_arrival);
        //displayallSaleDeliveryButton.setVisibility(View.INVISIBLE);
        displayallSaleDeliveryButton.setOnClickListener(this);
        buttonCheck=findViewById(R.id.query_purchase_arrival);
        buttonCheck.setOnClickListener(this);
        buttonExport=findViewById(R.id.b_export);
        buttonExport.setOnClickListener(this);

        tableListView = (ListView) findViewById(R.id.list_purchase_arrival);
        List<PurchaseArrivalBean> list = querySaleDelivery();
        listAllPostition = list;
        final PurchaseArrivalAdapter adapter1 = new PurchaseArrivalAdapter(PurchaseArrival.this, list, mListener);
        tableListView.setAdapter(adapter1);
        tableListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                adapter1.select(position);
                PurchaseArrivalBean saleDelivery1Bean = (PurchaseArrivalBean) adapter1.getItem(position);


            }
        });
        saleDeliveryHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 0x10:
                        Toast.makeText(PurchaseArrival.this, "请检查网络连接", Toast.LENGTH_LONG).show();
                        break;
                    case 0x11:
                        //插入UI表格数据
                        dialog.dismiss();
                        List<PurchaseArrivalBean> list = querySaleDelivery();
                        listAllPostition = list;
                        final PurchaseArrivalAdapter adapter = new PurchaseArrivalAdapter(PurchaseArrival.this, list, mListener);
                        tableListView.setAdapter(adapter);
                        tableListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                adapter.select(position);


                            }
                        });

                        break;
                    case 0x18:
                        String s = msg.getData().getString("uploadResp");
                        Toast.makeText(PurchaseArrival.this, s, Toast.LENGTH_LONG).show();
                        break;
                    case 0x19:
                        dialog.dismiss();
                        String exception = msg.getData().getString("Exception");
                        Toast.makeText(PurchaseArrival.this, "采购到货单下载异常，错误："+exception, Toast.LENGTH_LONG).show();
                        break;
                    default:
                        break;
                }
            }
        };
        Intent intent = getIntent();
        String str = intent.getStringExtra("from_business_operation");
        if ("Y".equals(str)) {
            downloadDeliveryButton.performClick();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
       saleDeliveryHandler.sendEmptyMessage(0x11);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.download_purchase_arrival:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (isNetworkConnected(PurchaseArrival.this)) {
                            try {
                                if (isWarehouseDBDownloaed()) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            dialog.setLoadingBuilder(Z_TYPE.CHART_RECT)//设置类型
                                                    .setLoadingColor(Color.BLUE)//颜色
                                                    .setCancelable(false)
                                                    .setCanceledOnTouchOutside(false)
                                                    .setHintTextSize(16) // 设置字体大小 dp
                                                    .setHintTextColor(Color.GRAY)  // 设置字体颜色
                                                    .setDurationTime(0.5) // 设置动画时间百分比 - 0.5倍
                                                    //     .setDialogBackgroundColor(Color.parseColor("#CC111111")) // 设置背景色，默认白色
                                                    .show();
                                        }
                                    });
                                    //R07发货单
                                    String saleDeliveryData = DataHelper.downloadDatabase("1",PurchaseArrival.this,6);
                                    if (null == saleDeliveryData) {
                                        dialog.dismiss();
                                        return;
                                    }

                                    Gson gson7 = new Gson();
                                    final PurchaseArrivalQuery saleDeliveryQuery = gson7.fromJson(saleDeliveryData, PurchaseArrivalQuery.class);

                                    int pagetotal = Integer.parseInt(saleDeliveryQuery.getPagetotal());
                                    if (pagetotal == 1) {
                                        insertDownloadDataToDB(saleDeliveryQuery);
                                        Message msg = new Message();
                                        msg.what = 0x11;
                                        saleDeliveryHandler.sendMessage(msg);
                                    } else if (pagetotal < 1) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                dialog.dismiss();
                                                Toast.makeText(PurchaseArrival.this, saleDeliveryQuery.getErrmsg(), Toast.LENGTH_LONG).show();
                                            }
                                        });
                                    } else {
                                        insertDownloadDataToDB(saleDeliveryQuery);
                                        for (int pagenum = 2; pagenum <= pagetotal; pagenum++) {
                                            String saleDeliveryData2 = DataHelper.downloadDatabase(pagenum+"",PurchaseArrival.this,6);
                                            PurchaseArrivalQuery saleDeliveryQuery2 = gson7.fromJson(saleDeliveryData2, PurchaseArrivalQuery.class);

                                            insertDownloadDataToDB(saleDeliveryQuery2);
                                        }
                                        Message msg = new Message();
                                        msg.what = 0x11;
                                        saleDeliveryHandler.sendMessage(msg);
                                    }

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            DataHelper.putLatestdownloadbegintime(getIntent().getIntExtra("type",-1),PurchaseArrival.this);
                                            SharedPreferences latestDBTimeInfo = getSharedPreferences("LatestPurchaseArrivalTSInfo", 0);
                                            String begintime = latestDBTimeInfo.getString("latest_download_ts_begintime", "2018-09-01 00:00:01");
                                            lst_downLoad_ts.setText("最后一次下载:"+begintime);
                                        }
                                    });
                                }else {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            dialog.dismiss();
                                            Toast.makeText(PurchaseArrival.this, "请先到基础数据管理界面下载仓库信息", Toast.LENGTH_LONG).show();
                                        }
                                    });
                                }
                            } catch (Exception e) {
                                //e.printStackTrace();
                                Bundle bundle = new Bundle();
                                bundle.putString("Exception", e.toString());
                                Message msg = new Message();
                                msg.what = 0x19;
                                msg.setData(bundle);
                                saleDeliveryHandler.sendMessage(msg);
                            }
                        } else {
                            Message msg = new Message();
                            msg.what = 0x10;
                            saleDeliveryHandler.sendMessage(msg);
                        }
                    }
                }).start();
                break;
            case R.id.query_purchase_arrival:
                popupQuery();
                break;
            case R.id.displayall_purchase_arrival:
                List<PurchaseArrivalBean> list = displayAllSaleDelivery();
                listAllPostition = list;
                final PurchaseArrivalAdapter adapter = new PurchaseArrivalAdapter(PurchaseArrival.this, list, mListener);
                tableListView.setAdapter(adapter);
                tableListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        adapter.select(position);
                        PurchaseArrivalBean saleDelivery1Bean = (PurchaseArrivalBean) adapter.getItem(position);

                    }
                });
                break;
            case R.id.b_export:
                exportData(exportList);
                break;
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:

                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private boolean isWarehouseDBDownloaed() {
        Cursor cursor = db3.rawQuery("select name from Warehouse",
                null);
        if (cursor != null && cursor.getCount() > 0) {
            return true;
        }else {
            return false;
        }
    }

    private void insertDownloadDataToDB(PurchaseArrivalQuery saleDeliveryQuery) {
        Log.i("data1-->",new Gson().toJson(saleDeliveryQuery));
        List<PurchaseArrivalQuery.DataBean> saleDeliveryBeanList = saleDeliveryQuery.getData();
        for (PurchaseArrivalQuery.DataBean ob : saleDeliveryBeanList) {

            String vbillcode = ob.getVbillcode();
            //0:新增-正常下载保持 1：删除，删除对应单据 2：修改，先删除对应单据再保持


              switch (Integer.parseInt(ob.getDr())){
                  case 0:
                      insertDb(ob);
                      break;
                  case 1:
                      db3.delete("PurchaseArrival", "vbillcode=?", new String[]{vbillcode});
                      db3.delete("PurchaseArrivalBody", "vbillcode=?", new String[]{vbillcode});
                      db3.delete("PurchaseArrivalScanResult", "vbillcode=?", new String[]{vbillcode});
                      break;
                  case 2:
                      db3.delete("PurchaseArrival", "vbillcode=?", new String[]{vbillcode});
                      db3.delete("PurchaseArrivalBody", "vbillcode=?", new String[]{vbillcode});
                      db3.delete("PurchaseArrivalScanResult", "vbillcode=?", new String[]{vbillcode});
                      insertDb(ob);
                      break;

              }


        }
    }

    private void insertDb(PurchaseArrivalQuery.DataBean ob) {
        Log.i("data2",new Gson().toJson(ob));
        String headpk = ob.getHeadpk();
        String dbilldate = ob.getDbilldate();
        String dr = ob.getDr();
        String vbillcode = ob.getVbillcode();
        String num = ob.getNum();
        String ts = ob.getTs();
        String org = ob.getOrg();
        List<PurchaseArrivalQuery.DataBean.BodyBean> saleDeliveryDatabodysList = ob.getBody();
        //使用 ContentValues 来对要添加的数据进行组装
        ContentValues values = new ContentValues();
        for (PurchaseArrivalQuery.DataBean.BodyBean obb : saleDeliveryDatabodysList) {
            String itempk = obb.getItempk();
            String materialcode = obb.getMaterialcode();
            String nnum = obb.getNnum();
            String maccode = obb.getMaccode();
            String warehouse = obb.getWarehouse();
            String materialname = obb.getMaterialname();
            //这里应该执行的是插入第二个表的操作
            ContentValues valuesInner = new ContentValues();
            valuesInner.put("headpk", ob.getHeadpk());
            valuesInner.put("vbillcode", ob.getVbillcode());
            valuesInner.put("itempk", itempk);
            valuesInner.put("materialcode", materialcode);
            valuesInner.put("materialname", materialname);
            valuesInner.put("maccode", maccode);
            valuesInner.put("nnum", nnum);
            valuesInner.put("scannum", "0");
            valuesInner.put("warehouse", warehouse);
            //N代表尚未上传
            valuesInner.put("uploadflag", "N");
            db3.insert("PurchaseArrivalBody", null, valuesInner);
            valuesInner.clear();
        }
        values.put("headpk", headpk);
        values.put("dbilldate", dbilldate);
        values.put("vbillcode", vbillcode);
        values.put("ts", ts);
        values.put("org", org);
        values.put("num", num);
        values.put("dr", dr);
        values.put("flag", "N");
        // 插入第一条数据
        db3.insert("PurchaseArrival", null, values);
        values.clear();
    }







    private void popupQuery() {
        List<String> listWarehouse;

        LayoutInflater layoutInflater = LayoutInflater.from(PurchaseArrival.this);
        View textEntryView = layoutInflater.inflate(R.layout.query_outgoing_dialog, null);
        final EditText codeNumEditText = (EditText) textEntryView.findViewById(R.id.codenum);
        final Spinner spinner = (Spinner) textEntryView.findViewById(R.id.warehouse_spinner);
        final Spinner flag_spinner = (Spinner) textEntryView.findViewById(R.id.upload_flag_spinner);
        final Button showdailogTwo = (Button)  textEntryView.findViewById(R.id.showdailogTwo);
        time = (TextView)  textEntryView.findViewById(R.id.timeshow_saledelivery);

        String tempperiod =DataHelper.getQueryTime(PurchaseArrival.this,getIntent().getIntExtra("type",-1));
        showdailogTwo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialogTwo();
            }
        });

        //仓库选择
        listWarehouse = DataHelper.queryWarehouseInfo(db3);
        listWarehouse.add("");
        final ArrayAdapter arrayAdapter = new ArrayAdapter(
                PurchaseArrival.this, android.R.layout.simple_spinner_item, listWarehouse);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(arrayAdapter);
        spinner.setSelection(listWarehouse.size()-1);
        spinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                query_cwarename=arrayAdapter.getItem(i).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });


        //订单选择
        uploadflag = new ArrayList();
        uploadflag.add("否");
        uploadflag.add("部分上传");
        uploadflag.add("是");
        uploadflag.add("全部");
        final ArrayAdapter adapter2 = new ArrayAdapter(
                PurchaseArrival.this, android.R.layout.simple_spinner_item, uploadflag);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        flag_spinner.setAdapter(adapter2);

        flag_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position){
                    case 0:
                        query_uploadflag = "N";
                        break;
                    case 1:
                        query_uploadflag = "PY";
                        break;
                    case 2:
                        query_uploadflag = "Y";
                        break;
                    case 3:
                        query_uploadflag = "ALL";
                        break;

                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        AlertDialog.Builder ad1 = new AlertDialog.Builder(PurchaseArrival.this);
        ad1.setTitle("出入查询条件:");
        ad1.setView(textEntryView);
        time.setText(tempperiod);
        ad1.setPositiveButton("查询", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int i) {
                String temp=codeNumEditText.getText().toString();
                exportList= queryexport(temp,query_cwarename,query_uploadflag);
                listAllPostition=new ArrayList<>();
                listAllPostition=removeDuplicate(exportList);
               PurchaseArrivalAdapter adapter = new PurchaseArrivalAdapter(PurchaseArrival.this, listAllPostition, mListener);
                tableListView.setAdapter(adapter);



            }
        });
        ad1.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int i) {

            }
        });
        ad1.show();// 显示对话框

    }

    List<PurchaseArrivalBean> exportList;
    private    List<PurchaseArrivalBean>  removeDuplicate(List<PurchaseArrivalBean> list)  {
        List<PurchaseArrivalBean>  beanList=new ArrayList<>();
        beanList.addAll(list);

        for  ( int  i  =   0 ; i  <  beanList.size()  -   1 ; i ++ )  {

            for  ( int  j  =  beanList.size()  -   1 ; j  >  i; j -- )  {

                if  (beanList.get(j).getVbillcode().equals(beanList.get(i).getVbillcode()))  {
                    beanList.remove(j);
                }
            }
        }
        return beanList;
    }
    private ArrayList<PurchaseArrivalBean> queryexport(String vbillcode,String current_cwarename,String query_uploadflag) {
        ArrayList<PurchaseArrivalBean> list = new ArrayList<>();
        SharedPreferences currentTimePeriod= getSharedPreferences("query_purchasearrival", 0);
        String start_temp = currentTimePeriod.getString("starttime", iUrl.begintime);
        String end_temp = currentTimePeriod.getString("endtime", Utils.getDefaultEndTime());
        Cursor cursor=null;
        if(query_uploadflag.equals("ALL")){
            cursor = db3.rawQuery("select PurchaseArrival.vbillcode, PurchaseArrival.dbilldate,PurchaseArrivalbody.materialcode,PurchaseArrival.dr," +
                    "PurchaseArrivalbody.materialname,PurchaseArrivalbody.maccode,PurchaseArrivalbody.nnum,saledeliveryscanresult.prodcutcode," +
                    "saledeliveryscanresult.xlh" + " from PurchaseArrival inner join PurchaseArrivalbody on PurchaseArrival.vbillcode=PurchaseArrivalbody.vbillcode " +
                    "left join saledeliveryscanresult on PurchaseArrivalbody.vbillcode=saledeliveryscanresult.vbillcode " +
                    "and PurchaseArrivalbody.itempk=saledeliveryscanresult.vcooporderbcode_b where PurchaseArrival.vbillcode" +
                    " like '%" + vbillcode + "%' and PurchaseArrivalbody.warehouse"+ " like '%" + current_cwarename + "%' order by dbilldate desc", null);

        }else {
            cursor = db3.rawQuery("select PurchaseArrival.vbillcode, PurchaseArrival.dbilldate,PurchaseArrivalbody.materialcode,PurchaseArrival.dr," +
                    "PurchaseArrivalbody.materialname,PurchaseArrivalbody.maccode,PurchaseArrivalbody.nnum,saledeliveryscanresult.prodcutcode," +
                    "saledeliveryscanresult.xlh" + " from PurchaseArrival inner join PurchaseArrivalbody on PurchaseArrival.vbillcode=PurchaseArrivalbody.vbillcode " +
                    "left join saledeliveryscanresult on PurchaseArrivalbody.vbillcode=saledeliveryscanresult.vbillcode " +
                    "and PurchaseArrivalbody.itempk=saledeliveryscanresult.vcooporderbcode_b where PurchaseArrivalbody.uploadflag=? and PurchaseArrival.vbillcode" +
                    " like '%" + vbillcode + "%' and PurchaseArrivalbody.warehouse"+ " like '%" + current_cwarename + "%' order by dbilldate desc", new String[]{query_uploadflag});

        }


        //判断cursor中是否存在数据
        while (cursor.moveToNext()) {

            PurchaseArrivalBean bean = new PurchaseArrivalBean();

            bean.vbillcode = cursor.getString(cursor.getColumnIndex("vbillcode"));
            bean.dbilldate = cursor.getString(cursor.getColumnIndex("dbilldate"));
            bean.setMaterialcode(cursor.getString(cursor.getColumnIndex("materialcode")));
            bean.setMaterialname(cursor.getString(cursor.getColumnIndex("materialname")));
            bean.setMaccode(cursor.getString(cursor.getColumnIndex("maccode")));
            bean.setNnum(cursor.getString(cursor.getColumnIndex("nnum")));
            bean.setProdcutcode(cursor.getString(cursor.getColumnIndex("prodcutcode")));
            bean.setXlh(cursor.getString(cursor.getColumnIndex("xlh")));
            bean.dr= cursor.getInt(cursor.getColumnIndex("dr"));


            if (DataHelper.queryTimePeriod(bean.vbillcode,start_temp,end_temp,getIntent().getIntExtra("type",-1),db3)) {
                list.add(bean);
            }

        }
        cursor.close();

        return list;
    }

    private void showDialogTwo() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_date, null);
        final DatePicker startTime = (DatePicker) view.findViewById(R.id.st);
        final DatePicker endTime = (DatePicker) view.findViewById(R.id.et);
        startTime.updateDate(startTime.getYear(), startTime.getMonth(), 01);
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("选择时间");
        builder.setView(view);
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String st=Utils.parseDate(startTime.getYear()+"-"+(startTime.getMonth()+1)+"-"+startTime.getDayOfMonth());
                String et =  Utils.parseDate(endTime.getYear()+"-"+(endTime.getMonth()+1)+"-"+endTime.getDayOfMonth());
                SharedPreferences currentTimePeriod= getSharedPreferences("query_purchasearrival", 0);
                SharedPreferences.Editor editor1 = currentTimePeriod.edit();
                editor1.putString("current_account",st+" 至 "+et);
                editor1.putString("starttime",st+ " "+"00:00:00");
                editor1.putString("endtime",et+ " "+"23:59:59");
                editor1.commit();
                time.setText(st+" 至 "+et);
            }
        });
        builder.setNegativeButton("取消", null);
        android.app.AlertDialog dialog = builder.create();
        dialog.show();
        //自动弹出键盘问题解决
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        startTime.setDescendantFocusability(DatePicker.FOCUS_BLOCK_DESCENDANTS);
        endTime.setDescendantFocusability(DatePicker.FOCUS_BLOCK_DESCENDANTS);
    }







    public boolean isNetworkConnected(Context context) {
        if (context != null) {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
            if (mNetworkInfo != null) {
                return mNetworkInfo.isAvailable();
            }
        }
        return false;
    }

    public ArrayList<PurchaseArrivalBean> querySaleDelivery() {
        ArrayList<PurchaseArrivalBean> list = new ArrayList<PurchaseArrivalBean>();
        List<String> list_update = new ArrayList<String>();

        Cursor cursor = db3.rawQuery("select vbillcode,dbilldate,dr,flag from PurchaseArrival order by dbilldate desc", null);
        if (cursor != null && cursor.getCount() > 0) {
            //判断cursor中是否存在数据
            while (cursor.moveToNext()) {
                PurchaseArrivalBean bean = new PurchaseArrivalBean();
                bean.vbillcode = cursor.getString(cursor.getColumnIndex("vbillcode"));
                bean.dbilldate = cursor.getString(cursor.getColumnIndex("dbilldate"));
                bean.dr = cursor.getInt(cursor.getColumnIndex("dr"));
                bean.setFlag(cursor.getString(cursor.getColumnIndex("flag")));
                Log.i("query",new Gson().toJson(bean));
                if(!cursor.getString(cursor.getColumnIndex("flag")).equals("Y")) {
                    list.add(bean);
                }

            }

            cursor.close();
        }

        return list;
    }
    public ArrayList<PurchaseArrivalBean> displayAllSaleDelivery() {
        ArrayList<PurchaseArrivalBean> list = new ArrayList<PurchaseArrivalBean>();
        Cursor cursor = db3.rawQuery("select vbillcode,dbilldate,dr,flag from PurchaseArrival order by dbilldate desc", null);
        if (cursor != null && cursor.getCount() > 0) {
            //判断cursor中是否存在数据
            while (cursor.moveToNext()) {
                PurchaseArrivalBean bean = new PurchaseArrivalBean();
                bean.vbillcode = cursor.getString(cursor.getColumnIndex("vbillcode"));
                bean.dbilldate = cursor.getString(cursor.getColumnIndex("dbilldate"));
                bean.dr = cursor.getInt(cursor.getColumnIndex("dr"));
                bean.setFlag(cursor.getString(cursor.getColumnIndex("flag")));

                list.add(bean);
            }
            cursor.close();
        }
        return list;
    }
    private void exportData( List<PurchaseArrivalBean> exportList) {
        Log.i("exportList",new Gson().toJson(exportList));
        String sdCardDir = Environment.getExternalStorageDirectory().getAbsolutePath();
        SimpleDateFormat formatter   =   new   SimpleDateFormat   ("yyyy年MM月dd日HH时mm分ss秒");
        File file=new File(sdCardDir+"/sunmi/export");
        if(!file.exists()){
            file.mkdir();
        }
        Date curDate =  new Date(System.currentTimeMillis());
        file=new File(sdCardDir+"/sunmi/export",formatter.format(curDate)+".txt");
        Toast.makeText(PurchaseArrival.this,"导出数据位置："+file.getAbsolutePath(),Toast.LENGTH_SHORT).show();
        FileOutputStream outputStream=null;
        try {
            outputStream=new FileOutputStream(file);
            outputStream.write(("发货单号"+"\t"+ "单据日期"+"\t"+"物料编码"+"\t"+"物料名称"+"\t"+
                    "物料大类"+"\t"+"序列号"+"\t"+"条形码"+"\t").getBytes());
            for (int j = 0; j <exportList.size() ; j++) {
                if(exportList.get(j).getXlh()!=null ) {
                    outputStream.write("\r\n".getBytes());
                    outputStream.write((exportList.get(j).getVbillcode()+"\t"
                            +exportList.get(j).getDbilldate()+"\t"
                            +exportList.get(j).getMaterialcode()+"\t"
                            +exportList.get(j).getMaterialname()+"\t"
                            +exportList.get(j).getMaccode()+"\t"
                            +exportList.get(j).getXlh()+"\t"
                            +exportList.get(j).getProdcutcode()).getBytes());
                }

            }
            outputStream.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 实现类，响应按钮点击事件
     */
    private PurchaseArrivalAdapter.MyClickListener mListener = new PurchaseArrivalAdapter.MyClickListener() {
        @Override
        public void myOnClick(int position, View v) {

            Intent intent = new Intent(PurchaseArrival.this, PurchaseReturnDetail.class);
            intent.putExtra("current_sale_delivery_vbillcode", listAllPostition.get(position).getVbillcode());
            intent.putExtra("current_sale_delivery_dbilldate", listAllPostition.get(position).getDbilldate());
            intent.putExtra("type",getIntent().getIntExtra("type",-1));

            startActivity(intent);

        }
    };
}
