package org.coolreader.readerview;

import org.coolreader.R;
import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.Properties;

public class CreateViewTask extends Task {

	final Properties props;
	final ReaderView mReaderView;

	public CreateViewTask(ReaderView rv, Properties props) {
		this.props = props;
		mReaderView = rv;
		Properties oldSettings = new Properties(); // may be changed by setAppSettings
		mReaderView.setAppSettings(props, oldSettings);
		props.setAll(oldSettings);
		mReaderView.mSettings = props;
	}

	public void work() throws Exception {
		BackgroundThread.ensureBackground();
		log.d("CreateViewTask - in background thread");
//			BackgroundTextureInfo[] textures = mEngine.getAvailableTextures();
//			byte[] data = mEngine.getImageData(textures[3]);
		byte[] data = mReaderView.mEngine.getImageData(mReaderView.currentBackgroundTexture);
		mReaderView.doc.setPageBackgroundTexture(data, mReaderView.currentBackgroundTexture.tiled?1:0);

		//File historyDir = activity.getDir("settings", Context.MODE_PRIVATE);
		//File historyDir = new File(Environment.getExternalStorageDirectory(), ".cr3");
		//historyDir.mkdirs();
		//File historyFile = new File(historyDir, "cr3hist.ini");

		//File historyFile = new File(activity.getDir("settings", Context.MODE_PRIVATE), "cr3hist.ini");
		//if ( historyFile.exists() ) {
		//log.d("Reading history from file " + historyFile.getAbsolutePath());
		//readHistoryInternal(historyFile.getAbsolutePath());
		//}
		String css = mReaderView.mEngine.loadResourceUtf8(R.raw.fb2);
		if (css != null && css.length()>0)
			mReaderView.doc.setStylesheet(css);
		mReaderView.applySettings(props);
		mReaderView.mInitialized = true;
		log.i("CreateViewTask - finished");
	}

	public void done() {
		log.d("InitializationFinishedEvent");
		//BackgroundThread.ensureGUI();
		//setSettings(props, new Properties());
	}

	public void fail(Exception e)
	{
		log.e("CoolReader engine initialization failed. Exiting.", e);
		mReaderView.mEngine.fatalError("Failed to init CoolReader engine");
	}

}
