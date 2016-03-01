package it.jaschke.alexandria;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import it.jaschke.alexandria.CameraPreview.ScannerActivity;
import it.jaschke.alexandria.data.AlexandriaContract;
import it.jaschke.alexandria.services.BookService;
import it.jaschke.alexandria.services.DownloadImage;


public class AddBook extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "INTENT_TO_SCAN_ACTIVITY";
    public static final int SCAN_REQUEST_CODE = 1;
    private EditText ean;
    private final int LOADER_ID = 1;
    private View rootView;
    private final String EAN_CONTENT="eanContent";
    private static final String SCAN_FORMAT = "scanFormat";
    private static final String SCAN_CONTENTS = "scanContents";

    private String mScanFormat = "Format:";
    private String mScanContents = "Contents:";


    private IntentFilter mIntentFilter;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadBooksIfBarcodeValid();
        }
    };
    private Cursor mData;


    public AddBook(){
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(ean!=null) {
            outState.putString(EAN_CONTENT, ean.getText().toString());
        }
    }

    /**
     * Noticed that when rotating fragment do not store the data
     * @param savedInstanceState
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            // Restore last state for checked position.
            ean.setText(savedInstanceState.getString(EAN_CONTENT));
            if(mData != null){
                loadBookToUI(mData);
            }
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_add_book, container, false);
        ean = (EditText) rootView.findViewById(R.id.ean);

        ean.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //no need
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //no need
            }

            @Override
            public void afterTextChanged(Editable s) {
                loadBooksIfBarcodeValid();
            }
        });

        rootView.findViewById(R.id.scan_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //check if camera permission granted
                SharedPreferences prefs = getActivity().getSharedPreferences(MainActivity.PREFS_NAME, MainActivity.MODE_PRIVATE);
                boolean permissionGranted = prefs.getBoolean(MainActivity.CAMERA_PERMISSION, true);
                if(permissionGranted){
                    Intent scanIntent = new Intent (getActivity(), ScannerActivity.class);
                    //the requestCode is changed by the Activity that owns the Fragment, so need to call getActivity()
                    getActivity().startActivityForResult(scanIntent, SCAN_REQUEST_CODE);
                }
                else {
                    Toast.makeText(getContext(), R.string.error_no_camera_permission, Toast.LENGTH_SHORT).show();
                }
            }
        });

        rootView.findViewById(R.id.save_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ean.setText("");
            }
        });

        rootView.findViewById(R.id.delete_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent bookIntent = new Intent(getActivity(), BookService.class);
                bookIntent.putExtra(BookService.EAN, ean.getText().toString());
                bookIntent.setAction(BookService.DELETE_BOOK);
                getActivity().startService(bookIntent);
                ean.setText("");
            }
        });

        if(savedInstanceState!=null){
            ean.setText(savedInstanceState.getString(EAN_CONTENT));
            ean.setHint("");
        }

        return rootView;
    }

    /**
     * Updates the UI. Updates the list of books by the barcode in the
     * textview
     */
    private void loadBooksIfBarcodeValid(){
        String barcode = ean.getText().toString();
        if(barcode.equals("")){
            return;
        }

        barcode = Utility.fixISBN10(barcode);

        if(!Utility.validateEAN(barcode)){
            clearFields();
            Toast.makeText(getContext(), R.string.error_barcode_not_valid, Toast.LENGTH_SHORT).show();
            return;
        }

        //load the list of the books
        //check if there is an internet connection
        if(Utility.isNetworkAvailable(getActivity())){
            Intent bookIntent = new Intent(getActivity(), BookService.class);
            bookIntent.putExtra(BookService.EAN, barcode);
            bookIntent.setAction(BookService.FETCH_BOOK);
            getActivity().startService(bookIntent);
            AddBook.this.restartLoader();
        } else {
            Toast.makeText(getActivity(),
                    R.string.error_no_internet_connection,
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //check if the scanner returned barcode
        String barcode = getBarcodeFromSharedPreferences();
        if(barcode != null){
            ean.setText(barcode);
            //clear the SharedPrefs
            SharedPreferences prefs = getActivity().getSharedPreferences(MainActivity.PREFS_NAME, MainActivity.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.apply();
        }

        //register the receiver
        mIntentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        getActivity().registerReceiver(mReceiver, mIntentFilter);

    }


    @Override
    public void onPause() {
        //unregister the connectivity manager
        getActivity().unregisterReceiver(mReceiver);
        super.onPause();

    }

    private void restartLoader(){
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if(ean.getText().length()==0){
            return null;
        }
        String eanStr= ean.getText().toString();
        eanStr = Utility.fixISBN10(eanStr);
        mData = null;

        return new CursorLoader(
                getActivity(),
                AlexandriaContract.BookEntry.buildFullBookUri(Long.parseLong(eanStr)),
                null,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor data) {
        if (!data.moveToFirst()) {
            return;
        }
        loadBookToUI(data);
        mData = data;
    }


    private void loadBookToUI(Cursor data){
        if(data != null){
            String bookTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.TITLE));
            if(bookTitle != null) {
                ((TextView) rootView.findViewById(R.id.bookTitle)).setText(bookTitle);
            } else {
                ((TextView) rootView.findViewById(R.id.bookTitle)).setText(R.string.no_data_title);
            }

            String bookSubTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.SUBTITLE));
            if(bookSubTitle != null){
                ((TextView) rootView.findViewById(R.id.bookSubTitle)).setText(bookSubTitle);
            } else {
                ((TextView) rootView.findViewById(R.id.bookTitle)).setText(R.string.no_data_subtitle);
            }

            String authors = data.getString(data.getColumnIndex(AlexandriaContract.AuthorEntry.AUTHOR));
            if(authors != null){
                String[] authorsArr = authors.split(",");
                ((TextView) rootView.findViewById(R.id.authors)).setLines(authorsArr.length);
                ((TextView) rootView.findViewById(R.id.authors)).setText(authors.replace(",","\n"));
            }else {
                ((TextView) rootView.findViewById(R.id.authors)).setText(R.string.no_data_author);
            }

            String imgUrl = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.IMAGE_URL));
            if(Patterns.WEB_URL.matcher(imgUrl).matches()){
                new DownloadImage((ImageView) rootView.findViewById(R.id.bookCover)).execute(imgUrl);
                rootView.findViewById(R.id.bookCover).setVisibility(View.VISIBLE);
            }

            String categories = data.getString(data.getColumnIndex(AlexandriaContract.CategoryEntry.CATEGORY));
           if(categories != null){
               ((TextView) rootView.findViewById(R.id.categories)).setText(categories);
           } else{
               ((TextView) rootView.findViewById(R.id.categories)).setText(R.string.no_data_categories);
           }

            rootView.findViewById(R.id.save_button).setVisibility(View.VISIBLE);
            rootView.findViewById(R.id.delete_button).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {

    }

    private void clearFields(){
        ((TextView) rootView.findViewById(R.id.bookTitle)).setText("");
        ((TextView) rootView.findViewById(R.id.bookSubTitle)).setText("");
        ((TextView) rootView.findViewById(R.id.authors)).setText("");
        ((TextView) rootView.findViewById(R.id.categories)).setText("");
        rootView.findViewById(R.id.bookCover).setVisibility(View.INVISIBLE);
        rootView.findViewById(R.id.save_button).setVisibility(View.INVISIBLE);
        rootView.findViewById(R.id.delete_button).setVisibility(View.INVISIBLE);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        activity.setTitle(R.string.scan);
    }

    private String getBarcodeFromSharedPreferences(){
        SharedPreferences prefs = getActivity().getSharedPreferences(MainActivity.PREFS_NAME, MainActivity.MODE_PRIVATE);
        String restoredText = prefs.getString(ScannerActivity.RESULT_BARCODE, null);
        if (restoredText != null) {
            String barcode = prefs.getString(ScannerActivity.RESULT_BARCODE,
                    getActivity().getString(R.string.prefs_result_barcode_default_value));
            String barcodeFormat = prefs.getString(ScannerActivity.RESULT_BARCODE_FORMAT,
                    getActivity().getString(R.string.prefs_result_format_default_value));
            if(!barcode.equals(getActivity().getString(R.string.prefs_result_barcode_default_value))){
                return barcode;
            } else {
                return null;
            }
        }
        return null;
    }
}
