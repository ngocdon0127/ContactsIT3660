package vn.hoasinhvien.contactsit3660;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.BroadcastReceiver;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class MainActivity extends TabActivity {


    ListView lvContact;
    ArrayList<Contact> contacts = new ArrayList<>();
    ContactAdapter adapter = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Tab Main");
        TabHost tabHost = getTabHost();
        LayoutInflater inflater = getLayoutInflater();
        inflater.inflate(R.layout.home_layout, tabHost.getTabContentView(), true);
        tabHost.addTab(tabHost.newTabSpec("tab1").setIndicator("Main Menu").setContent(R.id.tab1));
        tabHost.addTab(tabHost.newTabSpec("tab2").setIndicator("All Contacts").setContent(R.id.tab2));

        lvContact = (ListView) findViewById(R.id.lvContact);

    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, new IntentFilter(Information.BROADCAST_DONE_READ_DATA));
        registerReceiver(receiver, new IntentFilter(Information.BROADCAST_DONE_WRITE_FILE_TO_BACKUP));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    public void btnUpload(View v){
        Intent intent = new Intent(MainActivity.this,GoogleActivity.class);
        intent.putExtra(Information.TYPE, Information.UPLOAD);
        startActivity(intent);
    }


    public void btnContacts(View v){
        SharedData.addProgressDialog("Reading...", MainActivity.this);
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                readContactsFromDB();
            }
        }).start();
    }

    public void btnBackup(View v){
        writeContactsToXML();
    }

    public void btnRestore(View v){
        Intent intent = new Intent(MainActivity.this, ServerCommunicateActivity.class);
        intent.putExtra(Information.TYPE, Information.DOWNLOAD);
        startActivity(intent);
    }

    public void btnExit(View v){
        finish();
    }



    private void readContactsFromDB(){

        contacts.clear();
        int countdel = 0;
        Cursor c = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null, null, ContactsContract.Contacts._ID + " ASC");
        String name;
        ArrayList<String> number;
        ArrayList<String> email;
        String id;
        String lookup_key;
        Contact contact;
        while (c.moveToNext()){
            id = c.getString(c.getColumnIndex(ContactsContract.Contacts._ID));
            lookup_key = c.getString(c.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
            name =  c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
//
            // finish
            String selection = ContactsContract.Data.CONTACT_ID + " = ? ";
            String[] contact_id = new String[]{id};
//            System.out.println("----\n");
            Cursor c1 = getContentResolver().query(ContactsContract.Data.CONTENT_URI, null, selection, contact_id, null);
            number = new ArrayList<>();
            email = new ArrayList<>();
            while (c1.moveToNext()){
                String type = c1.getString(c1.getColumnIndex(ContactsContract.Data.MIMETYPE));
                String bufId = c1.getString(c1.getColumnIndex(ContactsContract.Data._ID));
                String bufData = c1.getString(c1.getColumnIndex(ContactsContract.Data.DATA1));
                if (type.equals(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)){
                    if (email.contains(bufData)){
                        int affected = getContentResolver().delete(ContactsContract.Data.CONTENT_URI, ContactsContract.Data._ID + " = ?", new String[]{bufId});
                        System.out.println("delete " + affected + " " + bufData + " of " + name);
                        countdel += affected;
                    }
                    else
                        email.add(bufData);
                }
                else if (type.equals(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)){
                    if (number.contains(stdNumber(bufData))){
                        int affected = getContentResolver().delete(ContactsContract.Data.CONTENT_URI, ContactsContract.Data._ID + " = ?", new String[]{bufId});
                        System.out.println("delete " + affected + " " + bufData + " of " + name);
                        countdel += affected;
                    }
                    else
                        number.add(stdNumber(bufData));
                }
            }
            c1.close();
            contact =  new Contact(id, name, number, email);
            if (!contacts.contains(contact)) {
                if (!contact.isNull())
                    contacts.add(contact);
            }
            else {
                try{
                    System.out.println("delete whole contact " + id);
                    int affected = getContentResolver().delete(ContactsContract.Data.CONTENT_URI, ContactsContract.Data.CONTACT_ID + " = ? ", new String[]{id});
                    System.out.println(affected + " info");
                    affected = getContentResolver().delete(ContactsContract.Contacts.CONTENT_URI, ContactsContract.Contacts._ID + " = ? ", new String[]{id});
                    System.out.println(affected + " contact");
                    Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookup_key);
                    System.out.println("lk: " + lookup_key);
                    System.out.println("uri: " + uri.toString());
                    affected = getContentResolver().delete(uri, null, null);
                    System.out.println(affected + " contact with lookupkey");
                }
                catch (Exception e){

                }
            }
        }
        c.close();
        System.out.println(countdel + " deleted.");
        sendBroadcast();
    }

    private void writeContactsToXML(){
        SharedData.addProgressDialog("Writing Contacts to File...", MainActivity.this);
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                contacts.clear();
                readContactsFromDB();
                File file = new File(Environment.getExternalStorageDirectory(), "contacts.xml");
                System.out.println(Environment.getExternalStorageDirectory());
                System.out.println(Environment.getExternalStorageDirectory().getAbsolutePath());
                if (file.exists()){
                    file.delete();
                    System.out.println("Xóa file cũ.");
                }
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(file);
                    PrintWriter pw = new PrintWriter(fos);
//            pw.write("<count>" + contacts.size() + "</count>\n\n");
                    pw.write("<contacts>\n\n");
                    for (int i = 0; i < contacts.size(); i++) {
                        Contact c = contacts.get(i);
                        pw.write("\t<contact>\n");
                        pw.write("\t\t<id>" + c.getId() + "</id>\n");
                        pw.write("\t\t<name>" + c.getName() + "</name>\n");
                        for(int j = 0; j < c.getNumber().size(); j++){
                            pw.write("\t\t<phone>" + c.getNumber().get(j) + "</phone>\n");
                        }
                        for(int j = 0; j < c.getEmail().size(); j++){
                            pw.write("\t\t<email>" + c.getEmail().get(j) + "</email>\n");
                        }
                        pw.write("\t</contact>\n\n");
                    }
                    pw.write("</contacts>");
                    pw.close();
                    fos.close();
//                    SharedData.progress = 1;
                    showToast("File saved.");
                    Intent intent = new Intent("vn.hoasinhvien.contactsit3660.mainActivity.backup");
                    sendBroadcast(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                    showToast("Failed.");
                }
            }
        }).start();
    }

    private void showToast(String s){
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
    }

    public static String stdNumber(String s){
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            if ((s.charAt(i) < '0' ) || (s.charAt(i) > '9' ))
                continue;
            count++;
        }
        char[] newNumber = new char[count];
        int index = 0;
        for (int i = 0; i < s.length(); i++) {
            if ((s.charAt(i) < '0' ) || (s.charAt(i) > '9' ))
                continue;
            newNumber[index] = s.charAt(i);
            index++;
        }
        return String.valueOf(newNumber);
    }

    public void sendBroadcast(){
        Intent intent = new Intent(Information.BROADCAST_DONE_READ_DATA);
        sendBroadcast(intent);
        SharedData.progress = 1;
    }

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {
            if (intent.getAction().equals(Information.BROADCAST_DONE_READ_DATA)) {
                SharedData.progress = 1;
                for (int i = contacts.size() - 1; i > 0 ; i--) {
                    for (int j = 0; j < i; j++) {
                        Contact c1 = contacts.get(j);
                        Contact c2 = contacts.get(j + 1);
                        try {
                            if (c1.getName().toLowerCase().compareTo(c2.getName().toLowerCase()) > 0){
                                contacts.set(j, c2);
                                contacts.set(j + 1, c1);
                            }
                        }
                        catch (Exception e){

                        }
                    }
                }
                adapter = new ContactAdapter();
                lvContact.setAdapter(adapter);
                lvContact.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        System.out.println(position);
//                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("content://contacts/people/" + contacts.get(position).getId()));
                        System.out.println(contacts.get(position).getId());
                        Intent intent = new Intent(MainActivity.this, ViewContactActivity.class);
                        intent.putExtra("contact", contacts.get(position));
                        startActivity(intent);
                    }
                });
                getTabHost().setCurrentTab(1);
            }
            else if (intent.getAction().equals(Information.BROADCAST_DONE_WRITE_FILE_TO_BACKUP)){
                Intent intent1 = new Intent(MainActivity.this, ServerCommunicateActivity.class);
                intent1.putExtra(Information.TYPE, Information.UPLOAD);
                startActivity(intent1);
            }
        }
    };

//

    class ContactAdapter extends ArrayAdapter<Contact>{

        ContactAdapter(){
            super(MainActivity.this, android.R.layout.simple_list_item_1, contacts);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            ContactHolder holder = null;

            if (row == null){
                LayoutInflater inflater = getLayoutInflater();
                row = inflater.inflate(R.layout.row, parent, false);
                holder = new ContactHolder(row);
                row.setTag(holder);
            }
            else{
                holder = (ContactHolder) row.getTag();
            }

            holder.populateFrom(contacts.get(position));
            return row;
        }
    }

    static class ContactHolder{
        private TextView tvName = null;
        private TextView tvFirstLetter = null;

        ContactHolder(View row){
            tvName = (TextView) row.findViewById(R.id.tvRowName);
            tvFirstLetter = (TextView) row.findViewById(R.id.tvFirstLetter);
        }

        void populateFrom(Contact c){
            tvName.setText(c.getName());
            Character ch = c.getName().substring(0, 1).toUpperCase().charAt(0);
            if (Character.isAlphabetic(ch))
                tvFirstLetter.setText(ch.toString());
            else
                tvFirstLetter.setText("#");
        }

    }

}
