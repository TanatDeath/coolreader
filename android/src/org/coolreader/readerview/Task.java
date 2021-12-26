package org.coolreader.readerview;

import android.util.Log;

import org.coolreader.crengine.Engine;
import org.coolreader.crengine.L;
import org.coolreader.crengine.Logger;

abstract class Task implements Engine.EngineTask {

	public static final Logger log = L.create("rvtask", Log.VERBOSE);

	public void done() {
		// override to do something useful
	}

	public void fail(Exception e) {
		// do nothing, just log exception
		// override to do custom action
		log.e("Task " + this.getClass().getSimpleName() + " is failed with exception " + e.getMessage(), e);
	}
}