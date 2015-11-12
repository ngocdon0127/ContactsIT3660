package vn.hoasinhvien.contactsit3660;

import android.app.Activity;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by NgocDon on 11/12/2015.
 */
public class RestoreActivity extends Activity {

    Button btnDownloadFromDropbox;
    Button btnDownloadFromGoogleDrive;
    Button btnRestore;
    Button btnDeleteAllContacts;

    ArrayList<Contact> contacts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.restore_layout);

        btnDownloadFromDropbox = (Button) findViewById(R.id.btnDownloadFromDropbox);
        btnDownloadFromGoogleDrive = (Button) findViewById(R.id.btnDownloadFromGoogleDrive);
        btnRestore = (Button) findViewById(R.id.btnRestore);
        btnDeleteAllContacts = (Button) findViewById(R.id.btnDeleteAllContacts);

        btnDownloadFromGoogleDrive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        btnDownloadFromDropbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RestoreActivity.this, DropboxActivity.class);
                intent.putExtra(Information.DROPBOX_LOCAL_UPLOAD_FILE_NAME, "d.txt");
                intent.putExtra(Information.DROPBOX_SERVER_DOWNLOAD_FILE_NAME, "g.txt");
                intent.putExtra(Information.TYPE, Information.DOWNLOAD);
                startActivityForResult(intent, 1);
            }
        });

        btnRestore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                writeContactsToDB();
                Toast.makeText(getApplicationContext(), "Contacts are restored successfully", Toast.LENGTH_SHORT).show();
            }
        });

        btnDeleteAllContacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Cursor c = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null, null, ContactsContract.Contacts._ID + " ASC");
                String lookup_key;
                String id;
                while (c.moveToNext()){
                    lookup_key = c.getString(c.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
                    id = c.getString(c.getColumnIndex(ContactsContract.Contacts._ID));
//                    System.out.println("delete whole contact " + id);
                    int affected = getContentResolver().delete(ContactsContract.Data.CONTENT_URI, ContactsContract.Data.CONTACT_ID + " = ? ", new String[]{id});
//                    System.out.println(affected + " info in Data");
                    affected = getContentResolver().delete(ContactsContract.RawContacts.CONTENT_URI, ContactsContract.Contacts._ID + " = ? ", new String[]{id});
//                    System.out.println(affected + " RawContacts");
                    affected = getContentResolver().delete(ContactsContract.Contacts.CONTENT_URI, ContactsContract.Contacts._ID + " = ? ", new String[]{id});
//                    System.out.println(affected + " contact");
                    Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookup_key);
//                    System.out.println("lk: " + lookup_key);
//                    System.out.println("uri: " + uri.toString());
                    affected = getContentResolver().delete(uri, null, null);
//                    System.out.println(affected + " contact with lookupkey");
                }
                Toast.makeText(getApplicationContext(), "Contacts are deleted successfully", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void readContactsFromFileXML(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SharedData.addProgressDialog("Reading from XML...", RestoreActivity.this);
            }
        });
//        contacts.clear();
        File file = new File(Environment.getExternalStorageDirectory(), "contacts.xml");
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = dbFactory.newDocumentBuilder();
            Document doc = builder.parse(file);
            doc.getDocumentElement().normalize();
            NodeList list = doc.getElementsByTagName("contact");
            for (int i = 0; i < list.getLength(); i++) {
                Node node = list.item(i);
                String id;
                String name;
                ArrayList<String> number = new ArrayList<>();
                ArrayList<String> email = new ArrayList<>();
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element e = (Element) node;
                    id = e.getElementsByTagName("id").item(0).getTextContent();
                    name = e.getElementsByTagName("name").item(0).getTextContent();
                    NodeList listNumber = e.getElementsByTagName("phone");
                    for(int j = 0; j < listNumber.getLength(); j++) {
                        String n = MainActivity.stdNumber(listNumber.item(j).getTextContent());
                        if (!number.contains(n))
                            number.add(n);
                        else
                            System.out.println("delete " + n);
                    }
                    NodeList listEmail = e.getElementsByTagName("email");
                    for(int j = 0; j < listEmail.getLength(); j++) {
                        email.add(listEmail.item(j).getTextContent());
                    }
                    Contact c = new Contact(id, name, number, email);
                    if (contacts.contains(c)) {
                        System.out.println("delete contact: ");
                        System.out.println(c.toString());
                    }
                    else
                        contacts.add(c);
                }
            }
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            sendBroadcast();
        }
    }

    public void sendBroadcast(){
        Intent intent = new Intent(Information.BROADCAST_DONE_READ_DATA);
        sendBroadcast(intent);
        SharedData.progress = 1;
    }

    private void writeContactsToDB(){
        readContactsFromFileXML();
        for (int i = 0; i < contacts.size(); i++) {
            Contact c = contacts.get(i);
            ArrayList<ContentProviderOperation> ops = new ArrayList<>();
            int rawContactInsertIndex = ops.size();
            ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null).build());
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, c.getName()).build());
            ArrayList<String> number = c.getNumber();
            for (int j = 0; j < number.size(); j++) {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.Data.DATA1, number.get(j)) // Can use ContactsContract.CommonDataKinds.Phone.NUMBER too.
                        .build());
            }
            ArrayList<String> email = c.getEmail();
            for (int j = 0; j < email.size(); j++) {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.Data.DATA1, email.get(j))
                        .build());
            }
            try {
                ContentProviderResult[] res = getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (OperationApplicationException e) {
                e.printStackTrace();
            }
//            System.out.println(i);
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "Recovery Contacts Successfully.", Toast.LENGTH_SHORT).show();
            }
        });

    }
}
