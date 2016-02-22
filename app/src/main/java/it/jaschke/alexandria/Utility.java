package it.jaschke.alexandria;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by squirrel on 2/21/16.
 */
public class Utility {

    /**
     * Checks if the interner connection is available
     * @param c - context
     * @return true if connection availbale, false - if not
     */
    static public boolean isNetworkAvailable(Context c) {
        ConnectivityManager cm =
                (ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }

    static boolean validateEAN(String ean){
        //TODO check other conditions to validate the barcode EAN13
        return (ean.length()==13);
    }

    static String fixISBN10(String ean){
        if(ean.length()==10 && !ean.startsWith("978")){
             return "978"+ean;
        } else return ean;
    }
}
