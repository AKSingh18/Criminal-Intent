package com.bignerdranch.android.criminalintent;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.List;

public class CrimeListFragment extends Fragment
{
    private static final String SAVED_SUBTITLE_VISIBLE = "subtitle";

    private TextView mEmptyListTextView;
    private RecyclerView mCrimeRecyclerView;
    private CrimeAdapter mAdapter;
    private boolean mSubtitleVisible;
    private Callbacks mCallbacks;

    private int lastUpdatedCrimePosition = -1;
    private int lastRemovedCrimePosition = -1;
    private int newlyAddedCrimePosition = -1;

    private class CrimeHolder extends RecyclerView.ViewHolder implements View.OnClickListener
    {
        private TextView mTitleTextView;
        private TextView mDateTextView;
        private ImageView mSolvedImageView;
        private Crime mCrime;

        public CrimeHolder(LayoutInflater inflater, ViewGroup parent)
        {
            super(inflater.inflate(R.layout.list_item_crime, parent, false));

            mTitleTextView = itemView.findViewById(R.id.crime_title);
            mDateTextView = itemView.findViewById(R.id.crime_date);
            mSolvedImageView = itemView.findViewById(R.id.crime_solved);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view)
        {
            lastUpdatedCrimePosition = getAdapterPosition();
            mCallbacks.onCrimeSelected(mCrime);
        }

        public void bind(Crime crime)
        {
            mCrime = crime;

            mTitleTextView.setText(mCrime.getTitle());
            DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.FULL);
            mDateTextView.setText(dateFormat.format(mCrime.getDate()));
            mSolvedImageView.setVisibility(crime.isSolved() ? View.VISIBLE : View.GONE);
        }
    }

    private class CrimeAdapter extends RecyclerView.Adapter<CrimeHolder>
    {
        public void setCrimes(List<Crime> crimes)
        {
            mCrimes = crimes;
        }

        private List<Crime> mCrimes;

        public CrimeAdapter(List<Crime> crimes)
        {
            mCrimes = crimes;
        }

        @NonNull
        @Override
        public CrimeHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
        {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());

            return new CrimeHolder(layoutInflater, parent);
        }

        @Override
        public void onBindViewHolder(@NonNull CrimeHolder holder, int position)
        {
            Crime crime = mCrimes.get(position);
            holder.bind(crime);
        }

        @Override
        public int getItemCount()
        {
            return mCrimes.size();
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_crime_list, container, false);

        if (savedInstanceState != null) mSubtitleVisible = savedInstanceState.getBoolean(SAVED_SUBTITLE_VISIBLE);

        mCrimeRecyclerView = view.findViewById(R.id.crime_recycler_view);
        mCrimeRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        mEmptyListTextView = view.findViewById(R.id.empty_list);
        mEmptyListTextView.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                initCrime();
            }
        });

        ItemTouchHelper itemTouchHelper =
                new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP |
                        ItemTouchHelper.DOWN, ItemTouchHelper.RIGHT)
                {
                    @Override
                    public boolean isLongPressDragEnabled()
                    {
                        return false;
                    }

                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target)
                    {
                        return false;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction)
                    {
                        CrimeLab crimeLab = CrimeLab.get(getContext());
                        Crime crime = crimeLab.getCrimes().get(viewHolder.getAdapterPosition());
                        crimeLab.deleteCrime(crime);

                        mCallbacks.onCrimeDeleted(crime);
                        lastRemovedCrimePosition = viewHolder.getAdapterPosition();

                        updateUI();
                    }
                });

        itemTouchHelper.attachToRecyclerView(mCrimeRecyclerView);

        updateUI();
        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        updateUI();
    }

    public void updateUI()
    {
        CrimeLab crimeLab = CrimeLab.get(getActivity());
        List<Crime> crimes = crimeLab.getCrimes();

        if (mAdapter == null)
        {
            mAdapter = new CrimeAdapter(crimes);
            mCrimeRecyclerView.setAdapter(mAdapter);
        }
        else
        {
            if (newlyAddedCrimePosition > -1)
            {
                mAdapter.notifyItemInserted(newlyAddedCrimePosition);
                newlyAddedCrimePosition = -1;
            }

            if (lastUpdatedCrimePosition > -1) mAdapter.notifyItemChanged(lastUpdatedCrimePosition);

            if (lastRemovedCrimePosition > -1)
            {
                mAdapter.notifyItemRemoved(lastRemovedCrimePosition);
                lastRemovedCrimePosition = -1;
            }

            mAdapter.setCrimes(crimes);
        }

        if (mAdapter.getItemCount() != 0) mEmptyListTextView.setVisibility(View.GONE);
        else mEmptyListTextView.setVisibility(View.VISIBLE);

        updateSubtitle();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater)
    {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_crime_list, menu);

        MenuItem subtitleItem = menu.findItem(R.id.show_subtitle);

        if (mSubtitleVisible) subtitleItem.setTitle(R.string.hide_subtitle);
        else subtitleItem.setTitle(R.string.show_subtitle);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.new_crime:
                initCrime();
                return true;

            case R.id.show_subtitle:
                mSubtitleVisible = !mSubtitleVisible;
                getActivity().invalidateOptionsMenu();
                updateSubtitle();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void initCrime()
    {
        Crime crime = new Crime();

        CrimeLab.get(getActivity()).addCrime(crime);

        newlyAddedCrimePosition = mAdapter.getItemCount();
        lastUpdatedCrimePosition = mAdapter.getItemCount();

        updateUI();

        mCallbacks.onCrimeSelected(crime);
    }

    private void updateSubtitle()
    {
        CrimeLab crimeLab = CrimeLab.get(getActivity());
        int crimeCount = crimeLab.getCrimes().size();

        String subtitle = getResources().getQuantityString(R.plurals.subtitle_plural, crimeCount, crimeCount);

        if (!mSubtitleVisible)
        {
            subtitle = null;
        }

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.getSupportActionBar().setSubtitle(subtitle);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putSerializable(SAVED_SUBTITLE_VISIBLE, mSubtitleVisible);
    }

    @Override
    public void onAttach(@NonNull Context context)
    {
        super.onAttach(context);
        mCallbacks = (Callbacks)context;
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        mCallbacks = null;
    }

    /**
     Required interface for hosting activities
     */
    public interface Callbacks
    {
        void onCrimeSelected(Crime crime);
        void onCrimeDeleted(Crime crime);
    }
}

