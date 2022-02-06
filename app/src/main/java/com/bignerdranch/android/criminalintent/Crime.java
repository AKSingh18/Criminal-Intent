package com.bignerdranch.android.criminalintent;

import java.util.Date;
import java.util.UUID;
import java.text.DateFormat;

public class Crime
{
    private UUID mID;
    private String mTitle;
    private String mSuspect;
    private Date mDate;
    private boolean mSolved;

    public Crime()
    {
        this(UUID.randomUUID());
    }

    public Crime(UUID id)
    {
        mID = id;
        mDate = new Date();
    }

    public UUID getID() {
        return mID;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getSuspect() {
        return mSuspect;
    }

    public void setSuspect(String suspect) {
        mSuspect = suspect;
    }

    public Date getDate()
    {
        return mDate;
    }

    public void setDate(Date date) {
        mDate = date;
    }

    public boolean isSolved() {
        return mSolved;
    }

    public void setSolved(boolean solved) {
        mSolved = solved;
    }

    public String getPhotoFilename() { return "IMG_" + getID().toString() + ".jpg"; }
}
