package com.bignerdranch.android.criminalintent;

import android.content.Intent;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

public class CrimeListActivity extends SingleFragmentActivity implements CrimeListFragment.Callbacks, CrimeFragment.Callbacks
{
    @Override
    protected Fragment createFragment()
    {
        return new CrimeListFragment();
    }

    @Override
    protected int getLayoutResId()
    {
        return R.layout.activity_masterdetail;
    }

    @Override
    public void onCrimeSelected(Crime crime)
    {
        if(findViewById(R.id.detail_fragment_container) == null)
        {
            Intent intent = CrimePagerActivity.newIntent(this, crime.getID());
            startActivity(intent);
        }
        else
        {
            CrimeFragment newDetail = CrimeFragment.newInstance(crime.getID());
            getSupportFragmentManager().
                    beginTransaction().
                    replace(R.id.detail_fragment_container, newDetail).
                    commit();
        }
    }

    @Override
    public void onCrimeDeleted(Crime crime)
    {
        FragmentManager fm = getSupportFragmentManager();
        CrimeFragment crimeFragment = (CrimeFragment)fm.findFragmentById(R.id.detail_fragment_container);

        if (crimeFragment != null && crimeFragment.equalsCrime(crime))
            fm.beginTransaction().remove(crimeFragment).commit();
    }

    @Override
    public void onCrimeUpdated(Crime crime)
    {
        CrimeListFragment listFragment = (CrimeListFragment)getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        listFragment.updateUI();
    }
}
