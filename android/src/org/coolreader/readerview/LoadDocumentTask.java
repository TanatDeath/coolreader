package org.coolreader.readerview;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.cloud.CloudAction;
import org.coolreader.cloud.CloudSync;
import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.BookInfo;
import org.coolreader.crengine.Bookmark;
import org.coolreader.crengine.DeviceInfo;
import org.coolreader.crengine.FileInfo;
import org.coolreader.crengine.Properties;
import org.coolreader.crengine.ReaderCommand;
import org.coolreader.crengine.Services;
import org.coolreader.crengine.Settings;
import org.coolreader.utils.FileUtils;
import org.coolreader.utils.StrUtils;
import org.coolreader.db.CRDBService;
import org.coolreader.eink.sony.android.ebookdownloader.SonyBookSelector;
import org.coolreader.userdic.UserDicDlg;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class LoadDocumentTask extends Task {
	String filename;
	String path;
	byte[] docBuffer;
	Runnable doneHandler;
	Runnable errorHandler;
	String pos;
	int profileNumber;
	boolean disableInternalStyles;
	boolean disableTextAutoformat;
	
	final ReaderView mReaderView;
	final CoolReader mActivity;
	
	LoadDocumentTask(ReaderView rv, BookInfo bookInfo, byte[] docBuffer, Runnable doneHandler, Runnable errorHandler) {
		mReaderView = rv;
		mActivity = rv.getActivity();
		BackgroundThread.ensureGUI();
		mReaderView.mBookInfo = bookInfo;
		mReaderView.skipFallbackWarning = false;
		FileInfo fileInfo = bookInfo.getFileInfo();
		log.v("LoadDocumentTask for " + fileInfo);
		if (fileInfo.getTitle() == null && docBuffer == null) {
			// As a book 'should' have a title, no title means we should
			// retrieve the book metadata from the engine to get the
			// book language.
			// Is it OK to do this here???  Should we use isScanned?
			// Should we use another fileInfo flag or a new flag?
			mReaderView.mEngine.scanBookProperties(fileInfo);
			if (StrUtils.isEmptyStr(fileInfo.getAuthors()))
				fileInfo = FileUtils.getFileProps(fileInfo, new File(fileInfo.getBasePath()),
						new FileInfo(new File(fileInfo.getBasePath()).getParent()), true);
			if (StrUtils.isEmptyStr(fileInfo.getAuthors())) Services.getEngine().scanBookProperties(fileInfo);
		}
		String language = fileInfo.getLanguage();
		log.v("update hyphenation language: " + language + " for " + fileInfo.getTitle());
		this.filename = fileInfo.getPathName();
		this.path = fileInfo.getBasePath();
		this.docBuffer = docBuffer;
		this.doneHandler = doneHandler;
		this.errorHandler = errorHandler;
		//FileInfo fileInfo = new FileInfo(filename);
		disableInternalStyles = mReaderView.mBookInfo.getFileInfo().getFlag(FileInfo.DONT_USE_DOCUMENT_STYLES_FLAG);
		if (mReaderView.mBookInfo.getFileInfo().getFlags() == 0) {
			boolean embed = mReaderView.mSettings.getBool(Settings.PROP_EMBEDDED_STYLES_DEF, false);
			disableInternalStyles = !embed;
			mReaderView.mBookInfo.getFileInfo().setFlag(FileInfo.DONT_USE_DOCUMENT_STYLES_FLAG, disableInternalStyles);
		}
		disableTextAutoformat = mReaderView.mBookInfo.getFileInfo().getFlag(FileInfo.DONT_REFLOW_TXT_FILES_FLAG);
		profileNumber = mReaderView.mBookInfo.getFileInfo().getProfileId();
		//Properties oldSettings = new Properties(mSettings);
		// it was decided, than there should not be profile switching, depending on book
		// https://github.com/plotn/coolreader/issues/589
//		int curProf = mActivity.getCurrentProfile();
//		if (curProf != profileNumber) {
//			mReaderView.showCenterPopup(mActivity.getString(R.string.settings_profile) + ":" +profileNumber, -1, false);
//		}
//		mActivity.setCurrentProfile(profileNumber);
//		if (profileNumber == 0) { // if there is no book profile, then set it to current
//			if (mActivity.getCurrentProfile() != 0)
//				if (mReaderView.mBookInfo != null && mReaderView.mBookInfo.getFileInfo() != null) {
//					mReaderView.mBookInfo.getFileInfo().setProfileId(mActivity.getCurrentProfile());
//					mActivity.getDB().saveBookInfo(mReaderView.mBookInfo);
//				}
//		}
//		log.v("BookProfileNumber : "+ profileNumber);
		Bookmark lastPos = null;
		if (mReaderView.mBookInfo != null)
			lastPos = mReaderView.mBookInfo.getLastPosition();
		if (lastPos != null)
			pos = lastPos.getStartPos();
		log.v("LoadDocumentTask : book info " + mReaderView.mBookInfo);
		log.v("LoadDocumentTask : last position = " + pos);
		if (lastPos != null)
			mReaderView.setTimeElapsed(lastPos.getTimeElapsed());
		//mBitmap = null;
		//showProgress(1000, R.string.progress_loading);
		//draw();
		BackgroundThread.instance().postGUI(() -> mReaderView.bookView.draw(false));
		//init();
		// close existing document
		log.v("LoadDocumentTask : closing current book");
		mReaderView.close();
		// it was decided, than there should not be profile switching, depending on book
//		final Properties currSettings = new Properties(mReaderView.mSettings);
//		BackgroundThread.instance().postBackground(() -> {
//			log.v("LoadDocumentTask : switching current profile");
//			mReaderView.applySettings(currSettings); //enforce settings reload
//			log.i("Switching done");
//		});
	}

	@Override
	public void work() throws IOException {
		BackgroundThread.ensureBackground();
		mReaderView.coverPageBytes = null;
		log.i("Loading document " + filename);
		mReaderView.doc.doCommand(ReaderCommand.DCMD_SET_INTERNAL_STYLES.nativeId, disableInternalStyles ? 0 : 1);
		mReaderView.doc.doCommand(ReaderCommand.DCMD_SET_TEXT_FORMAT.nativeId, disableTextAutoformat ? 0 : 1);
		mReaderView.doc.doCommand(ReaderCommand.DCMD_SET_REQUESTED_DOM_VERSION.nativeId, mReaderView.mBookInfo.getFileInfo().domVersion);
		if (0 == mReaderView.mBookInfo.getFileInfo().domVersion) {
			mReaderView.doc.doCommand(ReaderCommand.DCMD_SET_RENDER_BLOCK_RENDERING_FLAGS.nativeId, 0);
		} else {
			mReaderView.doc.doCommand(ReaderCommand.DCMD_SET_RENDER_BLOCK_RENDERING_FLAGS.nativeId, mReaderView.mBookInfo.getFileInfo().blockRenderingFlags);
		}
		boolean success;
		if (null != docBuffer)
			success = mReaderView.doc.loadDocumentFromBuffer(docBuffer, filename);
		else
			success = mReaderView.doc.loadDocument(filename);
		if (success) {
			log.v("loadDocumentInternal completed successfully");

			mReaderView.doc.requestRender();

			mReaderView.findCoverPage();
			log.v("requesting page image, to render");
			if (mReaderView.internalDX == 0 || mReaderView.internalDY == 0) {
				mReaderView.internalDX = mReaderView.surface.getWidth();
				mReaderView.internalDY = mReaderView.surface.getHeight();
				log.d("LoadDocument task: no size defined, resizing using widget size");
				mReaderView.doc.resize(mReaderView.internalDX, mReaderView.internalDY);
			}
			mReaderView.preparePageImage(0);
			log.v("updating loaded book info");
			mReaderView.updateLoadedBookInfo(null != docBuffer);
			log.i("Document " + filename + " is loaded successfully");
			if (pos == null) {
				Bookmark bmk = mActivity.readCurPosFile(false);
				if (bmk!=null) {
					boolean bSameBook=true;
					if (!bmk.bookFile.equals(mReaderView.mBookInfo.getFileInfo().getFilename())) bSameBook=false;
					if (!bmk.bookPath.equals(mReaderView.mBookInfo.getFileInfo().pathname)) bSameBook=false;
					if (!StrUtils.isEmptyStr(bmk.bookFileArc))
						if (!bmk.bookFileArc.equals(mReaderView.mBookInfo.getFileInfo().arcname))
							bSameBook=false;
					if (bSameBook) {
						pos=bmk.getStartPos();
						mActivity.showToast(mActivity.getString(R.string.pos_recovered));
					}
				}
			}
			if (null == docBuffer) {
				// Opened existing file
				log.i("Document " + filename + " is loaded successfully");
				if (pos != null) {
					log.i("Restoring position : " + pos);
					mReaderView.restorePositionBackground(pos);
				}
			} else {
				// Opened from memory buffer
				log.i("Stream " + filename + " loaded successfully");
				// restore the last read position and other tasks are
				// performed in the done () function, since we must
				// receive data from the database through callbacks
				// and cannot control the completion of the operation.
			}
			if (pos == null) mReaderView.checkOpenBookStyles(false);
			CoolReader.dumpHeapAllocation();
		} else {
			log.e("Error occurred while trying to load document " + filename);
			throw new IOException("Cannot read document");
		}
	}

	@Override
	public void done() {
		BackgroundThread.ensureGUI();
		log.d("LoadDocumentTask, GUI thread is finished successfully");
		if (!Services.isStopped()) {
			if (null == docBuffer) {
				// Opened from existing file
				Services.getHistory().updateBookAccess(mReaderView.mBookInfo, mReaderView.getTimeElapsed());
				final BookInfo finalBookInfo = new BookInfo(mReaderView.mBookInfo);
				mActivity.waitForCRDBService(() -> mActivity.getDB().saveBookInfo(finalBookInfo));
				if (mReaderView.coverPageBytes != null && mReaderView.mBookInfo.getFileInfo() != null) {
					// TODO: fix it
						/*
						DocumentFormat format = mReaderView.mBookInfo.getFileInfo().format;
						if (null != format) {
							if (format.needCoverPageCaching()) {
//			        			if (mActivity.getBrowser() != null)
//			        				mActivity.getBrowser().setCoverpageData(new FileInfo(mReaderView.mBookInfo.getFileInfo()), coverPageBytes);
							}
						}
						*/
					if (DeviceInfo.EINK_NOOK)
						mReaderView.updateNookTouchCoverpage(mReaderView.mBookInfo.getFileInfo().getPathName(), mReaderView.coverPageBytes);
					//mEngine.setProgressDrawable(coverPageDrawable);
				}
				if (DeviceInfo.EINK_SONY) {
					SonyBookSelector selector = new SonyBookSelector(mActivity);
					long l = selector.getContentId(path);
					if (l != 0) {
						selector.setReadingTime(l);
						selector.requestBookSelection(l);
					}
				}
				mActivity.setLastBook(filename);
			} else {
				// Opened from memory buffer
				// After stream successfully opened, find corresponding file it in DB
				// Now mBookInfo already contains updated data
				if (0 != mReaderView.mBookInfo.getFileInfo().crc32) {
					ArrayList<String> fingerprints = new ArrayList<String>(1);
					String fingerprint = Long.toString(mReaderView.mBookInfo.getFileInfo().crc32);
					fingerprints.add(fingerprint);
					mActivity.waitForCRDBService(() -> mActivity.getDB().findByFingerprints(10, fingerprints,
							new CRDBService.BookSearchCallback() {

								@Override
								public void onBooksSearchBegin() {

								}

								@Override
								public void onBooksFound(ArrayList<FileInfo> fileList) {
									FileInfo result = null;
									// TODO: select more recent file
									//  or may be file with maximum read pos
									for (FileInfo f : fileList) {
										if (f.exists()) {
											result = f;
											break;
										}
									}
									if (null == result) {
										// Tier 1, not found or not exist: save stream as file in app private directory,
										// At this point, the inputStream has already been fully read to the end
										// and cannot be reset to its original position.
										// So, we create a new input stream from docBuffer.
										ByteArrayInputStream inputStream = new ByteArrayInputStream(docBuffer);
										BookInfo bi = Services.getDocumentCache().saveStream(mReaderView.mBookInfo.getFileInfo(), inputStream);
										if (null != bi) {
											mReaderView.mBookInfo = new BookInfo(bi);
											Services.getHistory().updateBookAccess(mReaderView.mBookInfo, mReaderView.getTimeElapsed());
											final BookInfo finalBookInfo = new BookInfo(mReaderView.mBookInfo);
											mActivity.waitForCRDBService(() -> mActivity.getDB().saveBookInfo(finalBookInfo));
											mActivity.setLastBook(finalBookInfo.getFileInfo().getPathName());
										} else {
											log.e("Failed to save document memory buffer to file!");
											// Show error? Or something other action?
											// We cannot throw an exception here so that the fail() function
											// is called later, since we are in the done() function, not work().
											// And we cannot move this block of code to the work() function,
											// since we use callback functions to get information from the database,
											// i.e. this block of code is not continuously executing.
											// Therefore, we leave this exception unhandled.
											mActivity.showToast(R.string.failed_to_save_memory_stream);
										}
										if (inputStream != null) {
											try {
												inputStream.close();
											} catch (IOException e) {
											}
										}
									} else {
										// Tier 2, found: update mBookInfo, fileInfo, filename, pos
										mActivity.getDB().loadBookInfo(result, bookInfo -> {
											if (null != bookInfo) {
												// ok, bookmarks is loaded
												mReaderView.mBookInfo = new BookInfo(bookInfo);
												FileInfo fileInfo = mReaderView.mBookInfo.getFileInfo();
												filename = fileInfo.getPathName();
												path = fileInfo.arcname != null ? fileInfo.arcname : fileInfo.pathname;
												if (mReaderView.mBookInfo.getLastPosition() != null)
													pos = mReaderView.mBookInfo.getLastPosition().getStartPos();
												if (pos != null) {
													final String finalPos = pos;
													BackgroundThread.instance().executeBackground(() -> {
														log.i("Restoring position : " + finalPos);
														mReaderView.restorePositionBackground(finalPos);
													});
												}
												Services.getHistory().updateBookAccess(mReaderView.mBookInfo, mReaderView.getTimeElapsed());
												final BookInfo finalBookInfo = new BookInfo(mReaderView.mBookInfo);
												mActivity.waitForCRDBService(() -> mActivity.getDB().saveBookInfo(finalBookInfo));
												mActivity.setLastBook(filename);
												if (null != doneHandler)
													doneHandler.run();
											} else {
												// Logic error: not found by pathname, but found by fingerprint
												log.e("Failed to load bookmarks for book with fingerprint: " + fingerprint);
												if (null != errorHandler)
													errorHandler.run();
											}
										});
									}
								}
							}));
				} else {
					log.e("Invalid CRC32 (0)");
					// See comment above...
				}
			}
			mReaderView.hideProgress();

			mReaderView.selectionModeActive = false;
			mReaderView.selectionModeWasActive = false;
			mReaderView.inspectorModeActive = false;
			mReaderView.toggleScreenUpdateModeMode();

			mReaderView.drawPage(); //plotn - possibly it is unnesessary - due to new progress. But maybe not - page was empty last time

			mActivity.showReader();
			if (null != doneHandler)
				doneHandler.run();
			final String booknameF = mReaderView.getBookInfo().getFileInfo().getFilename();

			BackgroundThread.instance().postGUI(() -> {
				String bookname = mReaderView.getBookInfo().getFileInfo().getFilename();
				if (bookname.equals(booknameF)) {
					log.i("Load last rpos from CLOUD");
					int iSyncVariant3 = mReaderView.mSettings.getInt(Settings.PROP_CLOUD_SYNC_VARIANT, 0);
					if (iSyncVariant3 != 0) {
						if (mActivity.mCurrentFrame == mActivity.getmReaderFrame())
							CloudSync.loadFromJsonInfoFileList(((CoolReader) mActivity),
									CloudSync.CLOUD_SAVE_READING_POS, true, iSyncVariant3 == 1, CloudAction.FINDING_LAST_POS, true);
					}
				}
			}, 5000);

			UserDicDlg.updDicSearchHistoryAll(mActivity);
			mReaderView.mOpened = true;
			mReaderView.highlightBookmarks();
		}
	}

	public void fail(Exception e)
	{
		BackgroundThread.ensureGUI();
		mReaderView.close();
		log.v("LoadDocumentTask failed for " + mReaderView.mBookInfo, e);
		final FileInfo finalFileInfo = new FileInfo(mReaderView.mBookInfo.getFileInfo());

		mActivity.waitForCRDBService(() -> {
			if (!Services.isStopped())
				Services.getHistory().removeBookInfo(mActivity.getDB(), finalFileInfo, true, false);
		});
		mReaderView.mBookInfo = null;
		log.d("LoadDocumentTask is finished with exception " + e.getMessage());
		mReaderView.mOpened = false;
		BackgroundThread.instance().executeBackground(() -> {
			mReaderView.doc.createDefaultDocument(mActivity.getString(R.string.error), mActivity.getString(R.string.error_while_opening, filename));
			mReaderView.doc.requestRender();
			mReaderView.preparePageImage(0);
			mReaderView.drawPage();
		});
		mReaderView.hideProgress();
		mActivity.showToast("Error while loading document");
		if (errorHandler != null) {
			log.e("LoadDocumentTask: Calling error handler");
			errorHandler.run();
		}
	}
}
