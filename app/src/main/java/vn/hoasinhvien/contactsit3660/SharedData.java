package vn.hoasinhvien.contactsit3660;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

/**
 * Created by NgocDon on 11/12/2015.
 */
public class SharedData {

    public static Handler handler = new Handler();
    public static ProgressDialog dialog;
    public static int progress = 0;

    public static void addProgressDialog(String s, Context context){
        progress = 0;
        dialog = new ProgressDialog(context);
        dialog.setMax(1);
        dialog.setMessage(s);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setProgress(progress);
        dialog.setCancelable(false);
        dialog.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true){
                        Thread.sleep(100);
//                        System.out.println(progress);
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                dialog.setProgress(progress);
                            }
                        });
                        if (progress == 1){
                            break;
                        }
                    }
                    dialog.dismiss();

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

}
