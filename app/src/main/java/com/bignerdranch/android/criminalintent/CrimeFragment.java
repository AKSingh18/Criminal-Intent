package com.bignerdranch.android.criminalintent;

import static android.widget.CompoundButton.OnCheckedChangeListener;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ShareCompat.IntentBuilder;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class CrimeFragment extends Fragment
{
    private static final String TAG = "CRIME_FRAGMENT";

    private static final String ARG_CRIME_ID = "crime_id";
    private static final String DIALOG_DATE = "DialogDate";
    private static final String DIALOG_TIME = "DialogTime";
    private static final String DIALOG_IMAGE = "DialogImage";

    private static final int REQUEST_DATE = 0;
    private static final int REQUEST_CONTACT = 1;
    private static final int REQUEST_PHOTO = 2;
    private static final int REQUEST_TIME = 3;

    private Crime mCrime;
    private File mPhotoFile;
    private EditText mTitleField;
    private Button mDateButton;
    private Button mTimeButton;
    private CheckBox mSolvedCheckBox;
    private Button mReportButton;
    private Button mSuspectButton;
    private Button mDialSuspect;
    private ImageButton mPhotoButton;
    private ImageView mPhotoView;
    private Callbacks mCallbacks;

    public static CrimeFragment newInstance(UUID crimeID)
    {
        Bundle args = new Bundle();
        args.putSerializable(ARG_CRIME_ID, crimeID);

        CrimeFragment fragment = new CrimeFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater)
    {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_crime, menu);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (resultCode != Activity.RESULT_OK) return;

        if (requestCode == REQUEST_DATE)
        {
            Date date = DatePickerFragment.getDate(data);
            Date crimeDate = mCrime.getDate();

            date.setHours(crimeDate.getHours());
            date.setMinutes(crimeDate.getMinutes());

            mCrime.setDate(date);

            updateCrime();
            updateDate();
        }
        else if (requestCode == REQUEST_CONTACT && data != null)
        {
            Uri contactUri = data.getData();
            // Specify which fields you want your query to return values for
            String[] queryFields = new String[]{ContactsContract.Contacts.DISPLAY_NAME};
            // Perform your query - the contactUri is like a "where" clause here
            Cursor cursor = getActivity().getContentResolver().query(contactUri, queryFields, null, null, null);
            try
            {
                // Double-check that you actually got results
                if (cursor.getCount() == 0) return;

                // Pull out the first column of the first row of data - that is your suspect's name
                cursor.moveToFirst();
                String suspect = cursor.getString(0);
                mCrime.setSuspect(suspect);
                updateSuspect(suspect);
                updateCrime();
            }
            finally
            {
                cursor.close();
            }
        }
        else if (requestCode == REQUEST_PHOTO)
        {
            Uri uri = FileProvider.getUriForFile(getActivity(), "com.bignerdranch.android.criminalintent.fileprovider", mPhotoFile);
            getActivity().revokeUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            updateCrime();
            updatePhotoView(mPhotoView.getWidth(), mPhotoView.getHeight());


            mPhotoView.postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    mPhotoView.announceForAccessibility(getString(R.string.crime_photo_change));
                }
            }, 200);

        }
        else if (requestCode == REQUEST_TIME)
        {
            Date date = (Date)data.getSerializableExtra(TimePickerFragment.EXTRA_TIME);
            mCrime.setDate(date);

            updateTime();
        }
    }

    private void updateDate()
    {
        java.text.DateFormat dateFormat = java.text.DateFormat
                .getDateInstance(java.text.DateFormat.FULL, Locale.getDefault());
        mDateButton.setText(dateFormat.format(mCrime.getDate()));
    }

    private void updateTime()
    {
        java.text.DateFormat dateFormat = java.text.DateFormat
                .getTimeInstance(java.text.DateFormat.SHORT, Locale.getDefault());
        mTimeButton.setText(dateFormat.format(mCrime.getDate()));
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        UUID crimeID = (UUID)getArguments().getSerializable(ARG_CRIME_ID);
        mCrime = CrimeLab.get(getActivity()).getCrime(crimeID);
        mPhotoFile = CrimeLab.get(getActivity()).getPhotoFile(mCrime);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item)
    {
        if (item.getItemId() == R.id.delete_crime)
        {
            CrimeLab crimeLab = CrimeLab.get(getContext());
            crimeLab.deleteCrime(mCrime);
            mCallbacks.onCrimeDeleted(mCrime);

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        super.onCreateView(inflater, container, savedInstanceState);
        PackageManager packageManager = getActivity().getPackageManager();

        View v = inflater.inflate(R.layout.fragment_crime, container, false);

        mTitleField = v.findViewById(R.id.crime_title);
        mTitleField.setText(mCrime.getTitle());
        mTitleField.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                mCrime.setTitle(s.toString());
                updateCrime();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mDateButton = v.findViewById(R.id.crime_date);
        mDateButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                FragmentManager fragmentManager = getFragmentManager();
                DatePickerFragment dialog = DatePickerFragment.newInstance(mCrime.getDate());
                dialog.setTargetFragment(CrimeFragment.this, REQUEST_DATE);
                dialog.show(fragmentManager, DIALOG_DATE);
            }
        });

        mTimeButton = (Button)v.findViewById(R.id.crime_time);
        mTimeButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                FragmentManager fragmentManager = getFragmentManager();
                TimePickerFragment dialog = TimePickerFragment.newInstance(mCrime.getDate());
                dialog.setTargetFragment(CrimeFragment.this, REQUEST_TIME);
                dialog.show(fragmentManager, DIALOG_TIME);
            }
        });

        mSolvedCheckBox = v.findViewById(R.id.crime_solved);
        mSolvedCheckBox.setChecked(mCrime.isSolved());
        mSolvedCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                mCrime.setSolved(isChecked);
                updateCrime();
            }
        });

        mReportButton = v.findViewById(R.id.crime_report);
        mReportButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                IntentBuilder intentBuilder = IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText(getCrimeReport())
                        .setSubject(getString(R.string.crime_report_subject))
                        .setChooserTitle(R.string.crime_report);

                Intent intent = intentBuilder.getIntent();

                startActivity(intent);
            }
        });

        final Intent pickContact = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);

        mSuspectButton = v.findViewById(R.id.crime_suspect);
        mSuspectButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                startActivityForResult(pickContact, REQUEST_CONTACT);
            }
        });

        mDialSuspect = v.findViewById(R.id.dial_suspect);
        mDialSuspect.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                String number = getSuspectNumber();
                Uri uriNumber = Uri.parse("tel:" + number);

                final Intent dialContact = new Intent(Intent.ACTION_DIAL, uriNumber);
                startActivity(dialContact);
            }
        });

        mPhotoButton = (ImageButton)v.findViewById(R.id.crime_camera);

        final Intent captureImage = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        boolean canTakePhoto = mPhotoFile != null && captureImage.resolveActivity(packageManager) != null;

        mPhotoButton.setEnabled(canTakePhoto);
        mPhotoButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Uri uri = FileProvider.getUriForFile(getActivity(), "com.bignerdranch.android.criminalintent.fileprovider", mPhotoFile);
                captureImage.putExtra(MediaStore.EXTRA_OUTPUT, uri);

                List<ResolveInfo> cameraActivities = getActivity().getPackageManager().queryIntentActivities(captureImage, PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo activity : cameraActivities)
                {
                    getActivity().grantUriPermission(activity.activityInfo.packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                }

                startActivityForResult(captureImage, REQUEST_PHOTO);
            }
        });

        mPhotoView = v.findViewById(R.id.crime_photo);
        mPhotoView.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (mPhotoFile != null && mPhotoFile.exists())
                {
                    FragmentManager fragmentManager = getFragmentManager();
                    ImageFragment imageFragment = ImageFragment.newInstance(mPhotoFile);
                    imageFragment.show(fragmentManager, DIALOG_IMAGE);
                }
            }
        });

        mPhotoView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener()
        {
            @Override
            public void onGlobalLayout()
            {
                mPhotoView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                updatePhotoView(mPhotoView.getWidth(), mPhotoView.getHeight());
            }
        });

        updateDate();
        updateTime();

        if (packageManager.resolveActivity(pickContact, PackageManager.MATCH_DEFAULT_ONLY) == null) mSuspectButton.setEnabled(false);
        else updateSuspect(mCrime.getSuspect());

        return v;
    }

    private String getCrimeReport()
    {
        String solvedString;

        if (mCrime.isSolved()) solvedString = getString(R.string.crime_report_solved);
        else solvedString = getString(R.string.crime_report_unsolved);

        String dateFormat = "EEE, MMM dd";
        String dateString = DateFormat.format(dateFormat, mCrime.getDate()).toString();
        String suspect = mCrime.getSuspect();
        if (suspect == null) suspect = getString(R.string.crime_report_no_suspect);
        else suspect = getString(R.string.crime_report_suspect, suspect);

        String report = getString(R.string.crime_report, mCrime.getTitle(), dateString, solvedString, suspect);
        return report;
    }

    private void updatePhotoView(int sourceWidth, int sourceHeight)
    {
        if (mPhotoFile == null || !mPhotoFile.exists())
        {
            mPhotoView.setImageDrawable(null);
            mPhotoView.setContentDescription(getString(R.string.crime_photo_no_image_description));
        }
        else
        {
            Bitmap bitmap = PictureUtils.getScaledBitmap(mPhotoFile.getPath(), sourceWidth, sourceHeight);
            mPhotoView.setImageBitmap(bitmap);
            mPhotoView.setContentDescription(getString(R.string.crime_photo_image_description));
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
        CrimeLab.get(getActivity()).updateCrime(mCrime);
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

    private void updateCrime()
    {
        CrimeLab.get(getActivity()).updateCrime(mCrime);
        mCallbacks.onCrimeUpdated(mCrime);
    }

    private void updateSuspect(String name)
    {
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
        {
            mSuspectButton.setEnabled(false);
            mDialSuspect.setEnabled(false);
        }
        else if (name == null) mDialSuspect.setEnabled(false);
        else
        {
            mSuspectButton.setText(name);
            mDialSuspect.setEnabled(true);
            mDialSuspect.setText(getString(R.string.dial_suspect_text, name));
        }
    }

    public boolean equalsCrime(Crime crime)
    {
        return mCrime.getID().compareTo(crime.getID()) == 0;
    }

    private String getSuspectNumber()
    {
        Cursor cursor = getActivity().getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI,
                new String[]{ContactsContract.Contacts._ID, ContactsContract.Contacts.HAS_PHONE_NUMBER},
                "DISPLAY_NAME = '" + mCrime.getSuspect() + "'", null, null);

        try
        {
            if (cursor.getCount() == 0) return null;

            cursor.moveToFirst();
            long id = cursor.getLong(0);
            int hasPhoneNumber = cursor.getInt(1);

            if (hasPhoneNumber > 0)
            {
                Cursor phones = getActivity().getContentResolver().query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + id,
                        null, null);

                try
                {
                    if (phones.getCount() == 0) return null;

                    phones.moveToFirst();

                    String number = phones.getString(0);
                    return number;
                }
                finally
                {
                    phones.close();
                }
            }
            else return null;
        }
        finally
        {
            cursor.close();
        }
    }

    /**
     * Required interface for hosting activities
     */
    public interface Callbacks
    {
        void onCrimeUpdated(Crime crime);
        void onCrimeDeleted(Crime crime);
    }
}
