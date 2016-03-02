package it.jaschke.alexandria;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;

/**
 * Created by squirrel on 3/1/16.
 */
public class BookDetailActivity extends ActionBarActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.book_detail_activity);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        int id = R.id.detail_book_fragment;

        if (savedInstanceState == null) {
            Fragment newFragment = new BookDetail();
            getSupportFragmentManager().beginTransaction()
                    .replace(id, newFragment)
                    .commit();
        }
    }
}
