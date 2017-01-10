package com.example.ken.phone2dbox;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;


import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AppKeyPair;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String APP_KEY = "glv3h1o6846wnem";
    private static final String APP_SECRET = "hk6odxefa1wwf8c";
    private static final String TAG = "Phone2DBox";

    private static final int MY_PERMISSIONS_REQEST_WRITE_EXTERNAL_STORAGE = 1;

    private DropboxAPI<AndroidAuthSession>mApi;
    private boolean mLoggedIn = false;
    private Button mTakePicture;
    private ImageView mImage;
    private Button mSubmit;

    private static final int NEW_PICTURE = 1;
    private String mCameraFileName;
    private File mFile = null;

    private final String PHOTO_DIR = "/Photos/";
    private SwingListener mSwingListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppKeyPair appKeyPair = new AppKeyPair(APP_KEY,APP_SECRET);

        AndroidAuthSession session = new AndroidAuthSession(appKeyPair);

        mApi = new DropboxAPI<AndroidAuthSession>(session);

        setContentView(R.layout.activity_main);

        checkWriteExternalPermission();

        mSubmit = (Button)findViewById(R.id.submit);
        mSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mLoggedIn) {

                    logOut();
                    mSubmit.setText("ログイン");
                }else {
                    //コールバック先にMainAcivityを指定
                    mApi.getSession().startOAuth2Authentication(MainActivity.this);
                }
            }
        });

        mImage = (ImageView)findViewById(R.id.image_view);
        mTakePicture = (Button)findViewById(R.id.photo_button);

        mTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
                Date date = new Date();
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd-kk-ss", Locale.US);

                String newPicFile = df.format(date) + "jpg";
                String outPath = new File(Environment.getExternalStorageDirectory(),newPicFile).getPath();

                File outFile = new File(outPath);

                mCameraFileName = outFile.toString();

                Uri outuri = Uri.fromFile(outFile);

                intent.putExtra(MediaStore.EXTRA_OUTPUT,outuri);

                try{
                    startActivityForResult(intent,NEW_PICTURE);

                }catch (ActivityNotFoundException e) {
                }
            }
        });

        mSwingListener = new SwingListener(this);
        mSwingListener.setOnSwingListener(new SwingListener.OnSwingListener(){
            @Override
            public void onSwing(){
                if(mFile != null){
                    UploadPicture uploadPicture = new UploadPicture(MainActivity.this,mApi,PHOTO_DIR,mFile);
                    uploadPicture.execute();
                }

            }
        });
        mSwingListener.registSensor();
    }

    @Override
    protected void onResume(){
        super.onResume();
        AndroidAuthSession session = mApi.getSession();

        if(session.authenticationSuccessful()){
            try{
                session.finishAuthentication();
                String accessToken = session.getOAuth2AccessToken();
                mLoggedIn = true;
                mSubmit.setText("ログアウト");
            }catch (IllegalStateException e){
            }
        }
    }
    private void logOut(){
        mApi.getSession().unlink();
        mLoggedIn = false;
    }

    private void checkWriteExternalPermission(){
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)){

                new AlertDialog.Builder(this)
                        .setTitle("許可が必要です")
                        .setMessage("ファイルを保存してアップロードするために、WRITE_EXTERNAL_STRAGEを許可してください")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                requestWriteExternalStrage();
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .show();
            }else{
                requestWriteExternalStrage();
            }
        }
    }

    private void requestWriteExternalStrage(){
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},MY_PERMISSIONS_REQEST_WRITE_EXTERNAL_STORAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String permissions[],int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQEST_WRITE_EXTERNAL_STORAGE: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {

                }
                return;
            }
        }

    }


}
