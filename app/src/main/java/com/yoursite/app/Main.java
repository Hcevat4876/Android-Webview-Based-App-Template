package com.yoursite.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.Timer;
import java.util.TimerTask;

public class Main extends Activity {

	private WebView webView;
	private String url;
	
	@Override
	protected void onStop() {
		super.onStop();
		saveCookies();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// CRITICAL: Buraya kendi canlı Flask sitenin linkini yazıyorsun!
		url = "https://seninflasksiten.com"; 

		// UI referansı
		webView = (WebView) findViewById(R.id.webview_compontent);

		webView.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				return true;
			}
		});
		webView.setLongClickable(false);

		// Tarayıcının dışarı taşmasını önleme ve JS ayarları
		webView.setWebViewClient(new MyWebViewClient());
		
		WebSettings settings = webView.getSettings();
		settings.setJavaScriptEnabled(true);
		settings.setDomStorageEnabled(true); // DABI yapay zeka fetch istekleri için şart
		settings.setDatabaseEnabled(true);
		settings.setCacheMode(WebSettings.LOAD_DEFAULT);

		// Çerez (Session) yönetimini aktif ediyoruz
		CookieManager cookieManager = CookieManager.getInstance();
		cookieManager.setAcceptCookie(true);
		cookieManager.setAcceptThirdPartyCookies(webView, true);

		restoreCookies();
		
		if (savedInstanceState == null) {
			webView.loadUrl(url);
		}

		/* Çerezleri her 10 saniyede bir arka planda kaydeder */
		new Timer().scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				saveCookies();
			}
		}, 0, 10000);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		webView.saveState(outState);
		saveCookies();
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		webView.restoreState(savedInstanceState);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return super.onOptionsItemSelected(item);
	}
	
	/* Çerezleri cihaz hafızasına kaydetme metodu */
	private void saveCookies() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				String cookies = CookieManager.getInstance().getCookie(url);
				SharedPreferences sp = getSharedPreferences("MyPrefs", MODE_PRIVATE);
				SharedPreferences.Editor prefsEditor = sp.edit();
				prefsEditor.putString("cookies", cookies);
				prefsEditor.commit();
			}
		});
	}
	
	/* Çerezleri cihaz hafızasından geri yükleme metodu */
	private void restoreCookies() {
		SharedPreferences sp = getSharedPreferences("MyPrefs", MODE_PRIVATE);
		String cookies = sp.getString("cookies", "");
		CookieManager.getInstance().setCookie(url, cookies);
	}

	@Override
	public void onBackPressed() {
		if(webView != null && webView.canGoBack()) {
			webView.goBack();
			saveCookies();
		} else {
			saveCookies();
			super.onBackPressed();
		}
	}

	private class MyWebViewClient extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			if (url.startsWith("mailto:")) {
				url = url.replaceFirst("mailto:", "").trim();
				Intent i = new Intent(Intent.ACTION_SEND);
				i.setType("plain/text").putExtra(Intent.EXTRA_EMAIL, new String[] { url });
				startActivity(i);
				return true;
			}
			view.loadUrl(url);
			return true;
		}
	}
}
