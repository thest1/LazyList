package com.fedorvlasov.lazylist;

import android.app.Application;
import android.content.Context;

public class ApplicationContext extends Application {
	private static ApplicationContext instance;

	public ApplicationContext() {
		instance = this;
	}

	public static Context getContext() {
		return instance;
	}
}
