package org.coolreader.readerview;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.coolreader.crengine.L;
import org.coolreader.crengine.Logger;
import org.coolreader.utils.StrUtils;
import org.coolreader.utils.Utils;

import java.io.File;
import java.util.ArrayList;

public class RingBuffer {
	private long[] mArray;
	private long mSum;
	private long mAvg;
	private int mPos;
	private int mCount;
	private final int mSize;
	private ReaderView mReaderView;
	public int mUpdCnt = 0;

	public static final Logger log = L.create("rvrb", Log.VERBOSE);

	public RingBuffer(ReaderView rv, int size, long initialAvg) {
		mSize = size;
		mArray = new long[size];
		mPos = 0;
		mCount = 0;
		mAvg = initialAvg;
		mSum = 0;
		mReaderView = rv;
	}

	public long average() {
		return mAvg;
	}

	public void add(long val) {
		if (mCount < mSize)
			mCount++;
		else							// array is full
			mSum -= mArray[mPos];		// subtract from sum the value to replace
		mArray[mPos] = val;				// write new value
		mSum += val;					// update sum
		mAvg = mSum /mCount;			// calculate average value
		mPos++;
		if (mPos >= mSize)
			mPos = 0;
		if (mUpdCnt > 30) { // every 30 pages we'll save the stats
			saveRingBuffer();
			mUpdCnt = 0;
		}
	}

	public boolean isEmpty() {
		return 0 == mCount;
	}

	public void fill(long value) {
		mPos = 0;
		mCount = 0;
		mSum = 0;
		for (int i = 0; i < mSize; i++) {
			add(value);
		}
	}

	public void readRingBuffer()
	{
		log.d("Reading rbuf.json");
		try {
			String spath = mReaderView.getActivity().getSettingsFileF(0).getParent() + "/rbuf.json";
			File f = new File(spath);
			if (f.exists()) {
				String rb = Utils.readFileToString(spath);
				fullRecalc(new ArrayList<>(StrUtils.stringToArray(rb, Long[].class)));
			}
		} catch (Exception e) {
		}
	}

	public void saveRingBuffer()
	{
		log.d("Starting save rbuf.json");
		try {
			final Gson gson = new GsonBuilder().setPrettyPrinting().create();
			final String prettyJson = gson.toJson(getBuffer());
			Utils.saveStringToFileSafe(prettyJson,mReaderView.getActivity().getSettingsFileF(0).getParent() + "/rbuf.json");
		} catch (Exception e) {
		}
	}

	public void fullRecalc(ArrayList<Long> newArray) {
		if (newArray.size() > 0) {
			mArray = new long[mSize];
			mSum = 0L;
			mCount = 0;
			for (int i = 0; i < newArray.size(); i++) {
				if (i < mSize)
					mArray[i] = newArray.get(i);
				mSum += mArray[i];
				mCount++;
			}
			mAvg = mSum / mCount;
			log.d("Ring buffer fullRecalc, cnt = " + mCount);
		}
	}

	public ArrayList<Long> getBuffer() {
		ArrayList<Long> res = new ArrayList<>();
		for (int i = 0; i < mArray.length; i++) {
			res.add(mArray[i]);
		}
		return res;
	}
}
