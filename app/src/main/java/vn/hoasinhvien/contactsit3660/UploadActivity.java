package vn.hoasinhvien.contactsit3660;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by NgocDon on 11/9/2015.
 */
public class UploadActivity extends Activity {

    EditText txtFileName;
    Button btnUploadToDropbox;
    Button btnUploadToGoogleDrive;
    TextView tvResult;
    String uploadFileName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.upload_activity);

        txtFileName = (EditText) findViewById(R.id.txtFileName);
        btnUploadToDropbox = (Button) findViewById(R.id.btnUploadToDropbox);
        btnUploadToGoogleDrive = (Button) findViewById(R.id.btnUploadToGoogleDrive);
        tvResult = (TextView) findViewById(R.id.tvResult);

        btnUploadToDropbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getFileName()){
                    Intent intent = new Intent(UploadActivity.this, DropboxActivity.class);
                    intent.putExtra(Information.DROPBOX_LOCAL_UPLOAD_FILE_NAME, "new.txt");
                    intent.putExtra(Information.DROPBOX_SERVER_UPLOAD_FILE_NAME, uploadFileName);
                    System.out.println("start calling service");
                    startActivityForResult(intent, 1);
                    System.out.println("intent sent");
                }
                else {
                    Toast.makeText(getApplicationContext(), "Nhap ten file", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK){
            tvResult.setText("Successful.");
        }
        else {
            tvResult.setText("Fail");
        }
    }

    private boolean getFileName(){
        uploadFileName = txtFileName.getText().toString();
        if (uploadFileName.length() < 1)
            return false;
        return true;
    }

}
