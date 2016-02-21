package it.jaschke.alexandria.CameraPreview;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;

import java.util.ArrayList;
import java.util.List;

import it.jaschke.alexandria.R;
import me.dm7.barcodescanner.zxing.ZXingScannerView;

/**
 * Created by squirrel on 2/20/16.
 */
public class ScannerActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler {
    private static final String LOG_TAG = ScannerActivity.class.getSimpleName();
    private ZXingScannerView mScannerView;

    public ScannerActivity() {
        super();
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_scanner);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mScannerView = new ZXingScannerView(this);

        mScannerView.setAutoFocus(true);
        mScannerView.setFlash(true);

        //set the format of the barcodes to scan
        List<BarcodeFormat> formats = new ArrayList<BarcodeFormat>();
        formats.add(BarcodeFormat.EAN_13);
        mScannerView.setFormats(formats);

        setContentView(mScannerView);
    }

    @Override
    public void onResume() {
        super.onResume();
        mScannerView.setResultHandler(this);
        mScannerView.startCamera();
    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();
    }

    @Override
    public void handleResult(Result rawResult) {
        Toast.makeText(this, "Contents = " + rawResult.getText() +
                ", Format = " + rawResult.getBarcodeFormat().toString(), Toast.LENGTH_SHORT).show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mScannerView.resumeCameraPreview(ScannerActivity.this);
            }
        }, 2000);
    }
}
