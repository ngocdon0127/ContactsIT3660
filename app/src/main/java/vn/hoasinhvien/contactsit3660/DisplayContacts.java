package vn.hoasinhvien.contactsit3660;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

public class DisplayContacts extends AppCompatActivity {

    TextView id, name, sdt, email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_contacts);
        addControl();
    }

    public void addControl(){
        id = (TextView)findViewById(R.id.tvID);
        name = (TextView)findViewById(R.id.tvName);
        email = (TextView)findViewById(R.id.tvEmail);
        sdt = (TextView)findViewById(R.id.tvSdt);
    }
}
