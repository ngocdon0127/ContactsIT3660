package vn.hoasinhvien.contactsit3660;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AppKeyPair;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * Created by NgocDon on 11/9/2015.
 */
public class DropboxActivity extends Activity {

    AndroidAuthSession session;
    AppKeyPair appKeyPair;
    private DropboxAPI<AndroidAuthSession> mDBApi;
    String fileName;
    String uploadFileName;
    String downloadFileName;
    int type;
    private String accessToken;
    private String uid = "";
    private int result = Activity.RESULT_CANCELED;
    public int progress = 0;
    ProgressDialog progressDialog;
    Handler handler = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.dropbox_layout);

        Bundle bl = getIntent().getExtras();
        fileName = bl.getString(Information.DROPBOX_LOCAL_UPLOAD_FILE_NAME);
        type = bl.getInt(Information.TYPE);
        uploadFileName = bl.getString(Information.DROPBOX_SERVER_UPLOAD_FILE_NAME);
        downloadFileName = bl.getString(Information.DROPBOX_SERVER_DOWNLOAD_FILE_NAME);
        System.out.println(fileName + ":" + uploadFileName);

        appKeyPair = new AppKeyPair(Information.DROPBOX_APP_KEY, Information.DROPBOX_APP_SECRET);

        session = buildSession();
        mDBApi = new DropboxAPI<AndroidAuthSession>(session);
        System.out.println("status checked");

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    @Override
    protected void onResume() {

        super.onResume();

        registerReceiver(receiver, new IntentFilter(Information.KEY_BROADCAST_UPLOAD_DROPBOX));

        session = mDBApi.getSession();

        if (session.authenticationSuccessful()) {
            System.out.println("auth ok");
            try {
                uid = session.finishAuthentication();

                accessToken = session.getOAuth2AccessToken();
                storeKeys(accessToken, uid);
            } catch (IllegalStateException e) {
                System.out.println("Couldn't authenticate with Dropbox:"
                        + e.getLocalizedMessage());
            }
        }

        if (session.isLinked()){
//            btnLogIn.setText("Log Out");
            switch (type){
                case Information.UPLOAD:
                    upload();
                    break;
                case Information.DOWNLOAD:
                    download();
                    break;
            }
        }
        else {
//            btnLogIn.setText("Log In");
            logIn();
        }
    }

    public void download(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SharedData.addProgressDialog("Downloading...", DropboxActivity.this);
            }
        });
        new Thread(new Runnable() {
            @Override
            public void run() {
                File file = new File(Environment.getExternalStorageDirectory(), "contacts.xml");
                if (file.exists()){
                    file.delete();
                    System.out.println("Xóa file cũ.");
                }
                DropboxAPI.Entry entry = null;
                try {
                    entry = mDBApi.metadata("/" + "contacts.xml", 1, null, true, null);

                    if ((entry.isDir) || (entry.isDeleted)){
                        System.out.println("file not found");
                        result = Activity.RESULT_CANCELED;
                        setResult(result);
                        sendBroadcast();
                        return;
                    }
                } catch (DropboxException e) {
                    e.printStackTrace();
                    result = Activity.RESULT_CANCELED;
                    setResult(result);
                    sendBroadcast();
                    return;
                }
                try {
                    FileOutputStream fos = new FileOutputStream(file);
//            DropboxAPI.DropboxFileInfo info = mDBApi.getFile("/" + downloadFileName, null, fos, null);
                    DropboxAPI.DropboxFileInfo info = mDBApi.getFile("/contacts.xml", null, fos, null);
                    System.out.println("download xong.");
                    result = Activity.RESULT_OK;
//                    Intent intent = new Intent();
//                    setResult(Activity.RESULT_OK, intent);
                    sendBroadcast();
                    showToast("Download xong.");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    setResult(Activity.RESULT_CANCELED);
                } catch (DropboxException e) {
                    e.printStackTrace();
                    setResult(Activity.RESULT_CANCELED);
                }
            }
        }).start();
    }

    public void showToast(final String s){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private AndroidAuthSession buildSession() {
        String[] stored = getKeys();
        if (stored != null) {
            session = new AndroidAuthSession(appKeyPair);
            session.setOAuth2AccessToken(stored[0]);
//            session = new AndroidAuthSession()
        } else {
            session = new AndroidAuthSession(appKeyPair);
        }
        return session;
    }

    public void logIn(){
        AppKeyPair appKeyPair = new AppKeyPair(Information.DROPBOX_APP_KEY, Information.DROPBOX_APP_SECRET);
        session = new AndroidAuthSession(appKeyPair);
        mDBApi.getSession().startOAuth2Authentication(DropboxActivity.this);
    }

    public String[] getKeys(){
        SharedPreferences sharedPreferences = getSharedPreferences(Information.ACCOUNT_PREFS_NAME, 0);
        String[] s = new String[2];
        s[0] = sharedPreferences.getString(Information.ACCESS_TOKEN_NAME, "");
        s[1] = sharedPreferences.getString(Information.ACCESS_UID, "");
        System.out.println("preference: " + s[0] + ".");
        System.out.println("preference: " + sharedPreferences.getString("dondon", "") + ".");
        if (s[0].equals(""))
            return null;
        return s;
    }

    private void storeKeys(String token, String uid) {
        SharedPreferences prefs = getSharedPreferences(
                Information.ACCOUNT_PREFS_NAME, 0);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString(Information.ACCESS_TOKEN_NAME, token);
        edit.putString(Information.ACCESS_UID, uid);
        edit.commit();
    }

    public void sendBroadcast(){
        Intent intent = new Intent(Information.KEY_BROADCAST_UPLOAD_DROPBOX);
        intent.putExtra(Information.KEY_BROADCAST_UPLOAD_DROPBOX, result);
        sendBroadcast(intent);
    }

    public void upload(){

        SharedData.addProgressDialog("Uploading...", DropboxActivity.this);
//        progress = 0;
//        progressDialog = new ProgressDialog(DropboxActivity.this);
//        progressDialog.setCancelable(false);
//        progressDialog.setMax(1);
//        progressDialog.setMessage("Uploading...");
//        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
//        progressDialog.show();
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while (true) {
//                    try {
//                        Thread.sleep(100);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    handler.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            progressDialog.setProgress(progress);
//                        }
//                    });
//                    if (progress == 1)
//                        break;
//                }
//                progressDialog.dismiss();
//            }
//        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                DropboxAPI.Entry file = null;
                try {
                    file = mDBApi.metadata("/" + uploadFileName, 1, null, true, null);

                    if ((file.isDir) || (!file.isDeleted)){
                        System.out.println("file existed");
                        mDBApi.delete("/" + uploadFileName);
                    }
                } catch (DropboxException e) {
                    e.printStackTrace();
                    result = Activity.RESULT_CANCELED;
//                    sendBroadcast();
//                    return;
                }
                File localFile = new File(Environment.getExternalStorageDirectory(), fileName);
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(localFile);
                    DropboxAPI.Entry response = mDBApi.putFile("/" + uploadFileName, fis, localFile.length(), null, null);
                    System.out.println("DbExampleLog : The uploaded file's rev is: " + response.rev);
                    result = Activity.RESULT_OK;
                    sendBroadcast();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    result = Activity.RESULT_CANCELED;
                    sendBroadcast();
                } catch (DropboxException e) {
                    e.printStackTrace();
                    result = Activity.RESULT_CANCELED;
                    sendBroadcast();
                }

            }
        }).start();
    }

    public void finishUpload(){
        setResult(result);
        finish();
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            progress = 1;
            finishUpload();
        }
    };

    public void delete(final String path){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    DropboxAPI.Entry file = mDBApi.metadata(path, 0, null, false, null);
                    if (file.isDir){
                        System.out.println("'dir");
                    }
                    else {
                        if (!file.isDeleted){
                            System.out.println("file");
                        }
                        else {
                            System.out.println("not exist");
                        }
                    }
                } catch (DropboxException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
