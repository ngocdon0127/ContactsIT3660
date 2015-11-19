package vn.hoasinhvien.contactsit3660;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by NgocDon on 11/19/2015.
 */
public class ViewContactActivity extends Activity {

    TextView tvContact;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_contact_layout);

        tvContact = (TextView) findViewById(R.id.tvContact);
        String id = getIntent().getExtras().getString("id");
        String name = getIntent().getExtras().getString("name");
        String selection = ContactsContract.Data.CONTACT_ID + " = ? ";
        String[] contact_id = new String[]{id};
//            System.out.println("----\n");
        Cursor c1 = getContentResolver().query(ContactsContract.Data.CONTENT_URI, null, selection, contact_id, null);
        ArrayList<String > number = new ArrayList<>();
        ArrayList<String> email = new ArrayList<>();
        while (c1.moveToNext()){
            String type = c1.getString(c1.getColumnIndex(ContactsContract.Data.MIMETYPE));
//            String bufId = c1.getString(c1.getColumnIndex(ContactsContract.Data._ID));
            String bufData = c1.getString(c1.getColumnIndex(ContactsContract.Data.DATA1));
            if (type.equals(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)){
                email.add(bufData);
                System.out.println(bufData);
            }
            else if (type.equals(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)){
                number.add(SharedData.stdNumber(bufData));
                System.out.println(bufData);
            }
        }
        Contact contact =  new Contact(id, name, number, email);
        tvContact.setText(contact.getFullDetails());
    }
}
