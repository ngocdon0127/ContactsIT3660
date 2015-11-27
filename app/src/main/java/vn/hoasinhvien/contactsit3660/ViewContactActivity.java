package vn.hoasinhvien.contactsit3660;

import android.annotation.TargetApi;
import android.app.Activity;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by NgocDon on 11/19/2015.
 */
public class ViewContactActivity extends Activity {

    TextView tvPhone;
    TextView tvEmail;
    TextView textView;
    Button btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_contact_layout);

        tvPhone = (TextView) findViewById(R.id.tvPhone);
        tvEmail = (TextView) findViewById(R.id.tvEmail);
        textView = (TextView) findViewById(R.id.textView);
        btnBack = (Button) findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        findViewById(R.id.txtPhone).setEnabled(false);
        findViewById(R.id.txtEmail).setEnabled(false);
        Contact contact = (Contact) getIntent().getExtras().get("contact");
//        String selection = ContactsContract.Data.CONTACT_ID + " = ? ";
//        String[] contact_id = new String[]{id};
////            System.out.println("----\n");
//        Cursor c1 = getContentResolver().query(ContactsContract.Data.CONTENT_URI, null, selection, contact_id, null);
//        ArrayList<String > number = new ArrayList<>();
//        ArrayList<String> email = new ArrayList<>();
//        while (c1.moveToNext()){
//            String type = c1.getString(c1.getColumnIndex(ContactsContract.Data.MIMETYPE));
////            String bufId = c1.getString(c1.getColumnIndex(ContactsContract.Data._ID));
//            String bufData = c1.getString(c1.getColumnIndex(ContactsContract.Data.DATA1));
//            if (type.equals(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)){
//                email.add(bufData);
//                System.out.println(bufData);
//            }
//            else if (type.equals(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)){
//                number.add(SharedData.stdNumber(bufData));
//                System.out.println(bufData);
//            }
//        }
//        Contact contact =  new Contact(id, name, number, email);
        if (contact != null) {
//            tvContact.setText(contact.getFullDetails());
            Character ch = contact.getName().substring(0, 1).toUpperCase().charAt(0);
            if (Character.isAlphabetic(ch))
                textView.setText(ch.toString());
            else
                textView.setText("#");
//            textView.setText(contact.getName());
            String number = "";
            for (int i = 0; i < contact.getNumber().size(); i++) {
                number += contact.getNumber().get(i) + "\n";
            }
            tvPhone.setText(number);
            String email = "";
            for (int i = 0; i < contact.getEmail().size(); i++) {
                email += contact.getEmail().get(i) + "\n";
            }
            tvEmail.setText(email);
            btnBack.setText("  " + contact.getName());
        }

    }
}
