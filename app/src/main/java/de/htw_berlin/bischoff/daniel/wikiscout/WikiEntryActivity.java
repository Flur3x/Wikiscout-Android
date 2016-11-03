package de.htw_berlin.bischoff.daniel.wikiscout;

import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

public class WikiEntryActivity extends FragmentActivity implements WikiEntryFragment.OnFragmentInteractionListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wiki_entry);

        if (findViewById(R.id.fragment_container) != null) {

            if (savedInstanceState != null) {
                return;
            }

            WikiEntryFragment fragment = new WikiEntryFragment();

            fragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, fragment).commit();
        }
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}
