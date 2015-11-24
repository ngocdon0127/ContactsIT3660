package vn.hoasinhvien.contactsit3660;

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

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);


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

        OnItemClickListener mMessageClickedHandler = new OnItemClickListener(){
            public void onItemClick(AdapterView parent, View v, int position, long id)
            {
                downloadItemFromList(position);
            }
        };

        mListView.setOnItemClickListener(mMessageClickedHandler);

        final Button button = (Button) findViewById(R.id.btnUploadToGoogleDrive);
        button.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                saveFileToDrive();
            }
        });

        final Button button2 = (Button) findViewById(R.id.btnDownload);
        button2.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                getDriveContents();
            }
        });
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
            }
        });
        t.start();
    }

    private void downloadItemFromList(int position){
        mDLVal = (String) mListView.getItemAtPosition(position);
        showToast("You just pressed: " + mDLVal);

        Thread t = new Thread(new Runnable(){
            @Override
            public void run(){
                for(File tmp : mResultList){
                    if (tmp.getTitle().equalsIgnoreCase(mDLVal)){
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
                mAdapter = new ArrayAdapter<>(mContext, android.R.layout.simple_list_item_1, mFileArray);
                mListView.setAdapter(mAdapter);
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

                    showToast("Selected " + mFileUri.getPath() + " to upload. Hehe");

                    // File's meta data.
                    File body = new File();
                    body.setTitle(fileContent.getName());
                    body.setMimeType(cR.getType(mFileUri));

                    com.google.api.services.drive.Drive.Files f1 = mService.files();
                    com.google.api.services.drive.Drive.Files.Insert i1 = f1.insert(body, mediaContent);
                    File file = i1.execute();

                    if (file != null){
                        showToast("Uploaded: " + file.getTitle());
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
}
