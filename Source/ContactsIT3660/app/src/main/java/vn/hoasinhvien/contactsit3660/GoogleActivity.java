package vn.hoasinhvien.contactsit3660;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.view.View;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;


public class GoogleActivity extends Activity{
    static final int 				REQUEST_ACCOUNT_PICKER = 1;
    static final int 				REQUEST_AUTHORIZATION = 2;
    static final int 				RESULT_STORE_FILE = 4;
    private static Uri 				mFileUri;
    private static Drive 			mService;
    private GoogleAccountCredential mCredential;
    private Context 				mContext;
    private List<File> 				mResultList;
    private ListView 				mListView;
    private String[] 				mFileArray;
    private String 					mDLVal;
    private ArrayAdapter 			mAdapter;

    int type;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        type = getIntent().getExtras().getInt(Information.TYPE);
        System.out.println("type = " + type);

        // Connect to Google Drive
        try {
            mCredential = GoogleAccountCredential.usingOAuth2(GoogleActivity.this, Arrays.asList(DriveScopes.DRIVE));
        }catch (Exception e) {
            System.out.print("Error: "+ e.toString());
        }
        startActivityForResult(mCredential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);

        mContext = getApplicationContext();

        setContentView(R.layout.activity_google);
        mListView = (ListView) findViewById(R.id.lvDownload);

    }

    private void getDriveContents(){
        Thread t = new Thread(new Runnable(){
            @Override
            public void run(){
                mResultList = new ArrayList<>();
                com.google.api.services.drive.Drive.Files f1 = mService.files();
                com.google.api.services.drive.Drive.Files.List request = null;
                try {
                    request = f1.list();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                request.setQ("trashed=false");
                com.google.api.services.drive.model.FileList fileList = null;
                try {
                    fileList = request.execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                mResultList.addAll(fileList.getItems());
                request.setPageToken(fileList.getNextPageToken());
                populateListView();
                Intent intent = new Intent(Information.BROADCAST_GOOGLE_GET_DRIVE_CONTENT);
                sendBroadcast(intent);
            }
        });
        t.start();
    }

    private void download(){
        System.out.println("start download");
        Thread t = new Thread(new Runnable(){
            @Override
            public void run(){
                int exist = 0;
                for(File tmp : mResultList){
                    if (tmp.getTitle().equalsIgnoreCase("Contacts.xml")){
                        exist = 1;
                        if (tmp.getDownloadUrl() != null && tmp.getDownloadUrl().length() >0){
                            try{
                                com.google.api.client.http.HttpResponse resp = mService.getRequestFactory()
                                        .buildGetRequest(new GenericUrl(tmp.getDownloadUrl()))
                                        .execute();
                                InputStream iStream = resp.getContent();
                                try{
//                                    final java.io.File file = new java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath(),
                                    final java.io.File file = new java.io.File(Environment.getExternalStorageDirectory(),
                                            tmp.getTitle());
//                                    showToast("Downloading: " + tmp.getTitle() + " to " + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath());
                                    showToast("Downloading: " + tmp.getTitle() + " to " + Environment.getExternalStorageDirectory());
                                    storeFile(file, iStream);
                                } finally {
                                    iStream.close();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                if (exist == 0){
                    setResult(Activity.RESULT_CANCELED);
                    finish();
                }
            }
        });
        t.start();
    }

    private void populateListView(){
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                mFileArray = new String[mResultList.size()];
                int i = 0;
                for(File tmp : mResultList){
                    mFileArray[i] = tmp.getTitle();
                    i++;
                }
//                mAdapter = new ArrayAdapter<>(mContext, android.R.layout.simple_list_item_1, mFileArray);
//                mListView.setAdapter(mAdapter);
            }
        });
    }

    private void storeFile(java.io.File file, InputStream iStream){
        try{
            final OutputStream oStream = new FileOutputStream(file);
            try
            {
                try
                {
                    final byte[] buffer = new byte[1024];
                    int read;
                    while ((read = iStream.read(buffer)) != -1)
                    {
                        oStream.write(buffer, 0, read);
                    }
                    oStream.flush();
                    setResult(Activity.RESULT_OK);
                    finish();
                } finally {
                    oStream.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data){
        switch (requestCode){
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null && data.getExtras() != null)
                {
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        mCredential.setSelectedAccountName(accountName);
                        mService = getDriveService(mCredential);
                    }
                }
                if (type == Information.UPLOAD){
                    SharedData.addProgressDialog("Uploading to Google Drive...", GoogleActivity.this);
                    saveFileToDrive();
                }
                else if (type == Information.DOWNLOAD){
                    getDriveContents();
//                    download();
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == Activity.RESULT_OK) {
                    //account already picked
                } else {
                    startActivityForResult(mCredential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
                }
                break;
            case RESULT_STORE_FILE:
                mFileUri = data.getData();
                // Save the file to Google Drive
                saveFileToDrive();
                break;
        }
    }

    private Drive getDriveService(GoogleAccountCredential credential) {
        return new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential)
                .build();
    }


    private void saveFileToDrive(){
        Thread t = new Thread(new Runnable(){
            @Override
            public void run(){
                try{
                    // Create URI from real path
                    String path;
//                    path = Environment.getExternalStorageDirectory()+ "/Download/Contacts.vcf";
                    path = Environment.getExternalStorageDirectory()+ "/Contacts.xml";
                    mFileUri = Uri.fromFile(new java.io.File(path));

                    ContentResolver cR = GoogleActivity.this.getContentResolver();

                    // File's binary content
                    java.io.File fileContent = new java.io.File(mFileUri.getPath());
                    FileContent mediaContent = new FileContent(cR.getType(mFileUri), fileContent);

                    showToast("Selected " + mFileUri.getPath() + " to upload");

                    // File's meta data.
                    File body = new File();
                    body.setTitle(fileContent.getName());
                    body.setMimeType(cR.getType(mFileUri));

                    com.google.api.services.drive.Drive.Files f1 = mService.files();
                    com.google.api.services.drive.Drive.Files.Insert i1 = f1.insert(body, mediaContent);
                    File file = i1.execute();

                    if (file != null){
                        showToast("Uploaded: " + file.getTitle());
                        setResult(Activity.RESULT_OK);
                        SharedData.progress = 1;
                        finish();
                    }
                } catch (UserRecoverableAuthIOException e) {
                    startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
                } catch (IOException e) {
                    e.printStackTrace();
                    showToast("Transfer ERROR: " + e.toString());
                }
            }
        });
        t.start();
    }

    public void showToast(final String toast){
        runOnUiThread(new Runnable() {
            @Override
            public void run(){
                Toast.makeText(getApplicationContext(), toast, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, new IntentFilter(Information.BROADCAST_GOOGLE_GET_DRIVE_CONTENT));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Information.BROADCAST_GOOGLE_GET_DRIVE_CONTENT)){
                download();
            }
        }
    };
}
