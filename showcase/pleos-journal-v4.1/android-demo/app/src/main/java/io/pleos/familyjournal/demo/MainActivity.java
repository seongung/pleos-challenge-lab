package io.pleos.familyjournal.demo;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Pleos Journal demo shell.
 *
 * A single fullscreen, landscape, immersive WebView that shows the bundled
 * family-journal UI. Everything is served from the APK's assets; there is no
 * network access and the app requests no permissions.
 *
 * Launch extras (all optional, for screen recording):
 *   --ez autoplay true   (alias: demo)  -> index.html?autoplay=1&video=1
 *   --ez video false                    -> drop video=1 from the above
 *   --ef t 42.5          (alias: time)  -> index.html?t=42.5  (still frame)
 *   --es query "a=b"                    -> raw query string, overrides the above
 *   --ez fileMode true                  -> force file:///android_asset loading
 * Without extras the ordinary interactive home screen opens.
 */
public final class MainActivity extends Activity {

    private static final String TAG = "PleosJournal";

    /** Virtual, never-resolved origin. Requests are answered from APK assets. */
    private static final String ASSET_HOST = "appassets.androidplatform.net";
    private static final String ASSET_ROOT = "journal";
    private static final String ENTRY = "index.html";
    private static final String HTTPS_BASE = "https://" + ASSET_HOST + "/" + ASSET_ROOT + "/";
    private static final String FILE_BASE = "file:///android_asset/" + ASSET_ROOT + "/";

    private static final int REQ_FILE_CHOOSER = 4101;

    /**
     * Asks the page to handle "back" itself. The page may expose
     * window.onAndroidBack() (return true when consumed). Otherwise we look for
     * a visible in-app back/close control and tap it. Returns "true" if handled.
     */
    private static final String BACK_JS =
            "(function(){try{"
            + "if(typeof window.onAndroidBack==='function'){return !!window.onAndroidBack();}"
            + "var vis=function(e){if(!e||e.disabled)return false;var r=e.getBoundingClientRect();"
            + "if(r.width<2||r.height<2)return false;var s=getComputedStyle(e);"
            + "if(s.visibility==='hidden'||s.display==='none'||s.pointerEvents==='none'||+s.opacity<0.05)return false;"
            + "var x=r.left+r.width/2,y=r.top+r.height/2;"
            + "if(x<0||y<0||x>innerWidth||y>innerHeight)return false;"
            + "var t=document.elementFromPoint(x,y);return !!t&&(t===e||e.contains(t)||t.contains(e));};"
            + "var sel=['[data-android-back]','[data-back]','[data-action=\"back\"]','[data-action=\"close\"]',"
            + "'[data-action=\"cancel\"]','[aria-label*=\"뒤로\"]','[aria-label*=\"닫기\"]','[aria-label*=\"취소\"]',"
            + "'.back','.btn-back','.back-btn','.close','.btn-close'];"
            + "for(var i=0;i<sel.length;i++){var n=document.querySelectorAll(sel[i]);"
            + "for(var j=n.length-1;j>=0;j--){if(vis(n[j])){n[j].click();return true;}}}"
            + "return false;}catch(e){return false;}})()";

    private WebView webView;
    private boolean debuggable;
    private boolean fileMode;
    private boolean fellBack;
    private String query = "";
    private ValueCallback<Uri[]> pendingFileCallback;
    private long lastBackExitPress;
    private Object backCallback; // OnBackInvokedCallback on API 33+

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        debuggable = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            window.setAttributes(lp);
        }

        if (debuggable) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(getColor(R.color.paper));
        webView = new WebView(this);
        webView.setBackgroundColor(getColor(R.color.paper));
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setHapticFeedbackEnabled(false);
        webView.setLongClickable(false);
        webView.setOnLongClickListener(v -> true); // no text-selection / image context UI
        root.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);

        configureWebView();
        registerBackHandler();
        enterImmersive();

        fileMode = readBoolean(getIntent(), false, "fileMode");
        query = buildQuery(getIntent());
        if (savedInstanceState != null && webView.restoreState(savedInstanceState) != null) {
            Log.i(TAG, "restored WebView state");
        } else {
            loadEntry();
        }
    }

    // ------------------------------------------------------------------ setup

    @SuppressWarnings({"deprecation", "SetJavaScriptEnabled"})
    private void configureWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);            // localStorage -> saved journal survives restarts
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false); // bundled voice clips may start from script
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setTextZoom(100);                      // ignore system font scale; layout is an artboard
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true);           // picked photos arrive as content:// uris
        s.setCacheMode(WebSettings.LOAD_NO_CACHE); // assets change between builds; never go stale
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setSupportMultipleWindows(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            s.setForceDark(WebSettings.FORCE_DARK_OFF);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            s.setAlgorithmicDarkeningAllowed(false);
        }

        webView.setWebViewClient(new LocalClient());
        webView.setWebChromeClient(new ChromeClient());
    }

    private void loadEntry() {
        String url = (fileMode ? FILE_BASE : HTTPS_BASE) + ENTRY + query;
        if (fileMode) {
            enableFileModeSettings();
        }
        Log.i(TAG, "loading " + url);
        webView.loadUrl(url);
    }

    @SuppressWarnings("deprecation")
    private void enableFileModeSettings() {
        WebSettings s = webView.getSettings();
        s.setAllowFileAccess(true);
        s.setAllowFileAccessFromFileURLs(true); // lets the page XHR its own bundled json
    }

    /** Translates launch extras into the page's query string. */
    private static String buildQuery(Intent intent) {
        if (intent == null) return "";
        String raw = intent.getStringExtra("query");
        if (raw != null && !raw.trim().isEmpty()) {
            raw = raw.trim();
            return raw.startsWith("?") ? raw : "?" + raw;
        }
        List<String> parts = new ArrayList<>();
        boolean auto = readBoolean(intent, false, "autoplay", "demo");
        if (auto) {
            parts.add("autoplay=1");
            if (readBoolean(intent, true, "video")) parts.add("video=1");
        } else if (readBoolean(intent, false, "video")) {
            parts.add("video=1");
        }
        Double t = readNumber(intent, "t", "time");
        if (t != null && t >= 0) {
            parts.add("t=" + (t == Math.rint(t)
                    ? String.valueOf(t.longValue())
                    : String.format(Locale.US, "%.3f", t)));
        }
        if (parts.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("?");
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) sb.append('&');
            sb.append(parts.get(i));
        }
        return sb.toString();
    }

    /** Accepts --ez, --ei and --es forms so any adb invocation works. */
    @SuppressWarnings("deprecation")
    private static boolean readBoolean(Intent intent, boolean fallback, String... keys) {
        Bundle extras = intent == null ? null : intent.getExtras();
        if (extras == null) return fallback;
        for (String key : keys) {
            Object v = extras.get(key);
            if (v == null) continue;
            if (v instanceof Boolean) return (Boolean) v;
            if (v instanceof Number) return ((Number) v).doubleValue() != 0;
            String str = v.toString().trim().toLowerCase(Locale.US);
            return str.equals("1") || str.equals("true") || str.equals("yes") || str.equals("on");
        }
        return fallback;
    }

    @SuppressWarnings("deprecation")
    private static Double readNumber(Intent intent, String... keys) {
        Bundle extras = intent == null ? null : intent.getExtras();
        if (extras == null) return null;
        for (String key : keys) {
            Object v = extras.get(key);
            if (v == null) continue;
            if (v instanceof Number) return ((Number) v).doubleValue();
            try {
                return Double.parseDouble(v.toString().trim());
            } catch (NumberFormatException ignored) {
                // try next key
            }
        }
        return null;
    }

    // -------------------------------------------------------------- lifecycle

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        // Re-launching with explicit extras (e.g. from adb) reloads in that mode.
        // A plain launcher tap just brings the running journal back as it was.
        if (intent != null && intent.getExtras() != null && !intent.getExtras().isEmpty()) {
            fileMode = fellBack || readBoolean(intent, false, "fileMode");
            query = buildQuery(intent);
            loadEntry();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
        webView.resumeTimers();
        enterImmersive();
    }

    @Override
    protected void onPause() {
        webView.onPause();
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    protected void onDestroy() {
        if (pendingFileCallback != null) {
            pendingFileCallback.onReceiveValue(null);
            pendingFileCallback = null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && backCallback != null) {
            getOnBackInvokedDispatcher()
                    .unregisterOnBackInvokedCallback((OnBackInvokedCallback) backCallback);
        }
        ViewGroup parent = (ViewGroup) webView.getParent();
        if (parent != null) parent.removeView(webView);
        webView.destroy();
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) enterImmersive();
    }

    @SuppressWarnings("deprecation")
    private void enterImmersive() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController c = window.getInsetsController();
            if (c != null) {
                c.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                c.hide(WindowInsets.Type.systemBars());
            }
        } else {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }

    // ------------------------------------------------------------------- back

    private void registerBackHandler() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            OnBackInvokedCallback cb = this::handleBack;
            backCallback = cb;
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, cb);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        handleBack(); // API < 33
    }

    /** In-app back first, then WebView history, then double-press to leave. */
    private void handleBack() {
        webView.evaluateJavascript(BACK_JS, result -> {
            if ("true".equals(result)) return;
            if (webView.canGoBack()) {
                webView.goBack();
                return;
            }
            long now = System.currentTimeMillis();
            if (now - lastBackExitPress < 2000) {
                moveTaskToBack(true); // keep the journal alive in the background
            } else {
                lastBackExitPress = now;
                Toast.makeText(this, "한 번 더 누르면 앱을 나갑니다", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ----------------------------------------------------------- file chooser

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQ_FILE_CHOOSER) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
        ValueCallback<Uri[]> cb = pendingFileCallback;
        pendingFileCallback = null;
        if (cb == null) return;
        Uri[] result = null;
        if (resultCode == RESULT_OK && data != null) {
            ClipData clip = data.getClipData();
            if (clip != null && clip.getItemCount() > 0) {
                result = new Uri[clip.getItemCount()];
                for (int i = 0; i < result.length; i++) result[i] = clip.getItemAt(i).getUri();
            } else if (data.getData() != null) {
                result = new Uri[]{data.getData()};
            }
        }
        cb.onReceiveValue(result); // null == cancelled; the page input stays usable
        enterImmersive();
    }

    private final class ChromeClient extends WebChromeClient {
        @Override
        @SuppressWarnings("deprecation")
        public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                         FileChooserParams params) {
            if (pendingFileCallback != null) pendingFileCallback.onReceiveValue(null);
            pendingFileCallback = callback;

            // System document picker: grants per-file read access, so the app
            // itself needs no storage/media permission.
            Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
            pick.addCategory(Intent.CATEGORY_OPENABLE);
            pick.setType("image/*");
            String[] accept = params == null ? null : params.getAcceptTypes();
            if (accept != null) {
                List<String> mimes = new ArrayList<>();
                for (String a : accept) {
                    if (a != null && a.contains("/")) mimes.add(a.trim());
                }
                if (!mimes.isEmpty()) {
                    boolean allImages = true;
                    for (String m : mimes) allImages &= m.startsWith("image/");
                    if (!allImages) pick.setType("*/*");
                    pick.putExtra(Intent.EXTRA_MIME_TYPES, mimes.toArray(new String[0]));
                }
            }
            if (params != null && params.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE) {
                pick.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            }
            try {
                startActivityForResult(
                        Intent.createChooser(pick, getString(R.string.chooser_title)),
                        REQ_FILE_CHOOSER);
            } catch (ActivityNotFoundException e) {
                pendingFileCallback = null;
                callback.onReceiveValue(null);
                Toast.makeText(MainActivity.this, R.string.chooser_unavailable,
                        Toast.LENGTH_SHORT).show();
            }
            return true;
        }

        @Override
        public boolean onConsoleMessage(ConsoleMessage m) {
            if (debuggable) {
                Log.d(TAG, "js[" + m.messageLevel() + "] " + m.message()
                        + " (" + m.sourceId() + ":" + m.lineNumber() + ")");
            }
            return true;
        }
    }

    // ------------------------------------------------------ offline asset host

    private final class LocalClient extends WebViewClient {

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            String scheme = uri.getScheme() == null ? "" : uri.getScheme();
            if (scheme.equals("file") || scheme.equals("blob") || scheme.equals("data")
                    || scheme.equals("content") || scheme.equals("about")) {
                return null; // local by nature; let WebView handle it
            }
            if (scheme.equals("https") && ASSET_HOST.equalsIgnoreCase(uri.getHost())) {
                return serveAsset(uri, request);
            }
            // Anything else would be the network. This demo has none.
            Log.w(TAG, "blocked non-local request: " + uri);
            return status(404, "Not Found");
        }

        private WebResourceResponse serveAsset(Uri uri, WebResourceRequest request) {
            String path = uri.getPath() == null ? "" : uri.getPath();
            while (path.startsWith("/")) path = path.substring(1);
            if (path.isEmpty() || path.equals(ASSET_ROOT)) path = ASSET_ROOT + "/" + ENTRY;
            if (path.endsWith("/")) path += ENTRY;
            if (!path.startsWith(ASSET_ROOT + "/") || path.contains("..")) {
                return status(404, "Not Found");
            }
            String mime = mimeOf(path);
            String encoding = isText(mime) ? "UTF-8" : null;
            Map<String, String> headers = new HashMap<>();
            headers.put("Cache-Control", "no-store");
            headers.put("Accept-Ranges", "bytes");

            String range = request.getRequestHeaders() == null ? null
                    : firstHeader(request.getRequestHeaders(), "Range");
            try {
                if (range == null || !"GET".equalsIgnoreCase(request.getMethod())) {
                    InputStream in = getAssets().open(path);
                    return new WebResourceResponse(mime, encoding, 200, "OK", headers, in);
                }
                // Media elements ask for byte ranges; answer them properly so
                // audio duration/seek work exactly as on a real server.
                byte[] all = readFully(getAssets().open(path));
                long[] r = parseRange(range, all.length);
                if (r == null) {
                    headers.put("Content-Range", "bytes */" + all.length);
                    return new WebResourceResponse(mime, encoding, 416,
                            "Range Not Satisfiable", headers,
                            new ByteArrayInputStream(new byte[0]));
                }
                int start = (int) r[0];
                int len = (int) (r[1] - r[0] + 1);
                headers.put("Content-Range", "bytes " + r[0] + "-" + r[1] + "/" + all.length);
                headers.put("Content-Length", String.valueOf(len));
                return new WebResourceResponse(mime, encoding, 206, "Partial Content", headers,
                        new ByteArrayInputStream(all, start, len));
            } catch (IOException e) {
                if (!path.endsWith("favicon.ico")) Log.w(TAG, "asset missing: " + path);
                return status(404, "Not Found");
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            String s = uri.toString();
            boolean local = s.startsWith(HTTPS_BASE) || s.startsWith(FILE_BASE);
            if (!local) Log.w(TAG, "blocked navigation: " + s);
            return !local; // never leave the bundled journal
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request,
                                    WebResourceError error) {
            Log.w(TAG, "load error " + error.getErrorCode() + " " + error.getDescription()
                    + " for " + request.getUrl());
            // Safety net: if this WebView build refuses the virtual https origin
            // (e.g. because the app holds no INTERNET permission), fall back to
            // loading the very same files straight from file:///android_asset.
            if (request.isForMainFrame() && !fileMode && !fellBack) {
                fellBack = true;
                fileMode = true;
                view.post(MainActivity.this::loadEntry);
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            Log.i(TAG, "page finished: " + url);
        }
    }

    private static WebResourceResponse status(int code, String reason) {
        return new WebResourceResponse("text/plain", "UTF-8", code, reason,
                new HashMap<String, String>(), new ByteArrayInputStream(new byte[0]));
    }

    private static String firstHeader(Map<String, String> headers, String name) {
        for (Map.Entry<String, String> e : headers.entrySet()) {
            if (name.equalsIgnoreCase(e.getKey())) return e.getValue();
        }
        return null;
    }

    /** @return {start, endInclusive} or null when unsatisfiable. */
    static long[] parseRange(String header, long total) {
        try {
            String v = header.trim();
            if (!v.toLowerCase(Locale.US).startsWith("bytes=")) return new long[]{0, total - 1};
            v = v.substring(6).split(",")[0].trim();
            int dash = v.indexOf('-');
            if (dash < 0) return new long[]{0, total - 1};
            String a = v.substring(0, dash).trim();
            String b = v.substring(dash + 1).trim();
            long start;
            long end;
            if (a.isEmpty()) { // suffix range: last N bytes
                long n = Long.parseLong(b);
                if (n <= 0) return null;
                start = Math.max(0, total - n);
                end = total - 1;
            } else {
                start = Long.parseLong(a);
                end = b.isEmpty() ? total - 1 : Math.min(Long.parseLong(b), total - 1);
            }
            if (total == 0 || start >= total || start > end) return null;
            return new long[]{start, end};
        } catch (NumberFormatException e) {
            return new long[]{0, total - 1};
        }
    }

    private static byte[] readFully(InputStream in) throws IOException {
        try (InputStream src = in) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(8192, src.available()));
            byte[] buf = new byte[16384];
            int n;
            while ((n = src.read(buf)) > 0) out.write(buf, 0, n);
            return out.toByteArray();
        }
    }

    private static boolean isText(String mime) {
        return mime.startsWith("text/") || mime.equals("application/json")
                || mime.equals("application/javascript") || mime.equals("image/svg+xml");
    }

    private static String mimeOf(String path) {
        int dot = path.lastIndexOf('.');
        String ext = dot < 0 ? "" : path.substring(dot + 1).toLowerCase(Locale.US);
        switch (ext) {
            case "html": case "htm": return "text/html";
            case "js": case "mjs": return "application/javascript";
            case "css": return "text/css";
            case "json": return "application/json";
            case "txt": return "text/plain";
            case "png": return "image/png";
            case "jpg": case "jpeg": return "image/jpeg";
            case "webp": return "image/webp";
            case "gif": return "image/gif";
            case "svg": return "image/svg+xml";
            case "ico": return "image/x-icon";
            case "m4a": return "audio/mp4";
            case "mp3": return "audio/mpeg";
            case "wav": return "audio/wav";
            case "ogg": return "audio/ogg";
            case "mp4": return "video/mp4";
            case "woff": return "font/woff";
            case "woff2": return "font/woff2";
            case "ttf": return "font/ttf";
            default: return "application/octet-stream";
        }
    }
}
