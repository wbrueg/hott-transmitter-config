/**
 *  HoTT Transmitter Config
 *  Copyright (C) 2013  Oliver Treichel
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.treichels.hott.ui.android;

import gde.model.BaseModel;
import gde.model.enums.ModelType;
import gde.model.enums.Section;
import gde.model.enums.TransmitterType;
import gde.model.serial.ModelInfo;

import java.net.MalformedURLException;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintManager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;
import de.treichels.hott.ui.android.dialogs.DialogClosedListener;
import de.treichels.hott.ui.android.dialogs.OpenFromMemoryDialog;
import de.treichels.hott.ui.android.dialogs.OpenFromSdDialog;
import de.treichels.hott.ui.android.html.GenerateHtmlTask;
import de.treichels.hott.ui.android.html.GetModelFromUriTask;
import de.treichels.hott.ui.android.html.SectionAdapter;
import de.treichels.hott.ui.android.usb.GetModelFromMemoryTask;
import de.treichels.hott.ui.android.usb.GetModelFromSdTask;
import de.treichels.hott.ui.android.usb.UsbUtils;

/**
 * The main activity for the MdlViewer.
 *
 * @author oli@treichels.de
 */
public class MdlViewerActivity extends Activity implements ListView.OnItemClickListener {
    public static final String  CACHE_FILE_NAME   = "MdlViewer.html";                                      //$NON-NLS-1$
    private static final String MDL_MIME_TYPE     = "application/octet-stream";                            //$NON-NLS-1$
    private static final String MODEL_NAME        = MdlViewerActivity.class.getName() + ".modelName";
    private static final String MODEL_TYPE        = MdlViewerActivity.class.getName() + ".modelType";
    private static final int    READ_REQUEST_CODE = 42;
    private static final String TRANSMITTER_TYPE  = MdlViewerActivity.class.getName() + ".transmitterType";

    /** Table of contents list */
    private ListView            drawer            = null;

    /** Table of contents container */
    private DrawerLayout        drawerLayout      = null;

    /**
     * Url to temporary file.
     *
     * This is a ugly workaround, to enable TOC navigation via
     * {@code webView.loadUrl(loadUrl + "#" + section.name());}. It would be
     * more elegant to navigate via JavaScript calls. However, this is only
     * supported on Anroid 4.4 or higher.
     */
    private String              loadUrl           = null;

    /** Model name */
    private String              modelName         = null;

    /** Model type */
    private ModelType           modelType         = null;

    /** Progress Bar */
    private ProgressBar         progressBar       = null;

    private Toast               toast             = null;

    /** Transmitter type */
    private TransmitterType     transmitterType   = null;

    /** Html view */
    private WebView             webView           = null;

    /**
     * Handle file selection in response to a {@link Intent.ACTION_GET_CONTENT}
     * action intent.
     */
    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent resultData) {
        if (resultCode == Activity.RESULT_OK && resultData != null && requestCode == READ_REQUEST_CODE) {
            updateUI(resultData.getData());
        }
    }

    /**
     * Called when the activity is first created.
     *
     * @param savedInstanceState
     *            If the activity is being re-initialized after previously being
     *            shut down then this Bundle contains the data it most recently
     *            supplied in onSaveInstanceState(Bundle). <b>Note: Otherwise it
     *            is null.</b>
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        // temporary file to store html
        try {
            loadUrl = getFileStreamPath(CACHE_FILE_NAME).toURI().toURL().toString();
        } catch (final MalformedURLException e) {
            throw new RuntimeException(e);
        }

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);

        webView = (WebView) findViewById(R.id.webwiew);
        webView.getSettings().setBuiltInZoomControls(true);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer = (ListView) findViewById(R.id.drawer);
        drawer.setOnItemClickListener(this);

        final Intent intent = getIntent();
        if (intent != null && intent.getAction() == Intent.ACTION_VIEW) {
            // activity was started via an intent
            updateUI(intent.getData());
        } else if (savedInstanceState != null) {
            // restore from saved state
            if (savedInstanceState.containsKey(MODEL_NAME)) {
                modelName = savedInstanceState.getString(MODEL_NAME);
            }

            if (savedInstanceState.containsKey(MODEL_TYPE)) {
                modelType = ModelType.valueOf(savedInstanceState.getString(MODEL_TYPE));
            }

            if (savedInstanceState.containsKey(TRANSMITTER_TYPE)) {
                transmitterType = TransmitterType.valueOf(savedInstanceState.getString(TRANSMITTER_TYPE));
            }

            updateUI();
        } else {
            // fall back to load from file
            performFileSearch();
        }
    }

    /**
     * Handle menu button
     *
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.options_menu, menu);

        if (!UsbUtils.isUsbHost(this)) {
            // disable these menu items as they don't work without USB host mode
            menu.findItem(R.id.action_load_from_sd).setVisible(false);
            menu.findItem(R.id.action_load_from_tx).setVisible(false);
        }

        return true;
    }

    /**
     * Handle toc selection in drawer.
     *
     * Navigate webView to the anchor with the same name as the section in the
     * drawer that was clicked.
     *
     * @param parent
     * @param view
     * @param position
     * @param id
     */
    @Override
    public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
        drawerLayout.closeDrawer(drawer);

        final Section section = Section.values()[(int) id];
        webView.loadUrl(loadUrl + "#" + section.name()); //$NON-NLS-1$
    }

    /**
     * Handle menu selection.
     *
     * Launch functionality according to menu item selected.
     *
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_print:
            print();
            return true;

        case R.id.action_load_from_file:
            performFileSearch();
            return true;

        case R.id.action_load_from_sd:
            performUsbSdSearch(item);
            return true;

        case R.id.action_load_from_tx:
            performUsbMemorySearch(item);
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);

        if (modelName != null) {
            outState.putString(MODEL_NAME, modelName);
        }

        if (modelType != null) {
            outState.putString(MODEL_TYPE, modelType.name());
        }

        if (transmitterType != null) {
            outState.putString(TRANSMITTER_TYPE, transmitterType.name());
        }
    }

    /**
     * Fires an intent to spin up the "file chooser" UI and select a file.
     * Response will be handled by {@link MdlViewerActivity.onActivityResult()}
     */
    public void performFileSearch() {
        Toast.makeText(getApplicationContext(), R.string.msg_select_mdl, Toast.LENGTH_SHORT).show();

        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(MDL_MIME_TYPE);

        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    /**
     * Select models from transmitter memory
     *
     * @param item
     */
    public void performUsbMemorySearch(final MenuItem item) {
        final OpenFromMemoryDialog dialog = new OpenFromMemoryDialog();

        dialog.setDialogClosedListener(new DialogClosedListener() {
            @Override
            public void onDialogClosed(final int resultStatus) {
                if (resultStatus == DialogClosedListener.OK) {
                    final UsbDevice device = dialog.getUsbDevice();
                    final ModelInfo info = dialog.getResult();
                    if (info != null) {
                        new GetModelFromMemoryTask(MdlViewerActivity.this, device) {
                            @Override
                            protected void onPostExecute(final BaseModel result) {
                                switch (getResultStatus()) {
                                case ok:
                                    updateUI(result);
                                    break;

                                case error:
                                    if (getResultMessage() != null) {
                                        showDialog(getResultMessage());
                                    } else if (getResultException() != null) {
                                        showDialog(getResultException().getLocalizedMessage());
                                    }
                                    break;

                                default:
                                    // do nothing
                                }
                            }

                            @Override
                            protected void onPreExecute() {
                                // tell user to be patient
                                setWait(R.string.msg_decode_data);
                            }
                        }.execute(info.getModelNumber());
                    }
                }
            }
        });

        try {
            dialog.show(getFragmentManager(), "open_from_tx");
        } catch (final Exception e) {
            dialog.dismiss();
            showDialog(e.getLocalizedMessage());
        }
    }

    private void performUsbSdSearch(final MenuItem item) {
        final OpenFromSdDialog dialog = new OpenFromSdDialog();

        dialog.setDialogClosedListener(new DialogClosedListener() {
            @Override
            public void onDialogClosed(final int resultStatus) {
                if (resultStatus == DialogClosedListener.OK) {
                    final UsbDevice device = dialog.getUsbDevice();
                    final String filePath = dialog.getResult();
                    if (filePath != null) {
                        new GetModelFromSdTask(MdlViewerActivity.this, device) {
                            @Override
                            protected void onPostExecute(final BaseModel result) {
                                switch (getResultStatus()) {
                                case ok:
                                    updateUI(result);
                                    break;

                                case error:
                                    if (getResultMessage() != null) {
                                        showDialog(getResultMessage());
                                    } else if (getResultException() != null) {
                                        showDialog(getResultException().getLocalizedMessage());
                                    }
                                    break;

                                default:
                                    // do nothing
                                }
                            }

                            @Override
                            protected void onPreExecute() {
                                // tell user to be patient
                                setWait(R.string.msg_decode_data);
                            }
                        }.execute(filePath);
                    }
                }
            }
        });

        try {
            dialog.show(getFragmentManager(), "open_from_sd");
        } catch (final Exception e) {
            dialog.dismiss();
            showDialog(e.getLocalizedMessage());
        }
    }

    /**
     * Create a PDF version of the document. Only support on Android 4.4 and
     * newer.
     */
    @TargetApi(19)
    public void print() {
        final PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
        printManager.print("HoTTMdlViewer - " + modelName, webView.createPrintDocumentAdapter(), null); //$NON-NLS-1$
    }

    private void setWait(final int msgId) {
        if (toast != null) {
            toast.cancel();
        }

        if (msgId == 0) {
            webView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.INVISIBLE);
        } else {
            toast = Toast.makeText(MdlViewerActivity.this, msgId, Toast.LENGTH_LONG);
            toast.show();

            webView.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Show a simple error dialog.
     *
     * @param title
     * @param messages
     */
    private void showDialog(final String message) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.msg_error);
        builder.setMessage(message);
        builder.setPositiveButton(android.R.string.ok, null);
        builder.create().show();
    }

    /**
     * Reload the ui from saved state.
     */
    public void updateUI() {
        Log.d("updateUI()", modelName);
        Log.d("updateUI()", modelType.name());
        Log.d("updateUI()", transmitterType.name());

        if (modelName != null && modelType != null && transmitterType != null) {
            setWait(R.string.msg_loading);

            webView.loadUrl(loadUrl);
            setTitle(modelName);
            drawer.setAdapter(new SectionAdapter(MdlViewerActivity.this, modelType, transmitterType));

            // turn off busy indicator
            setWait(0);
        }
    }

    /**
     * Update WebView in a background thread without blocking the UI thread.
     * Convert a {@link BaseModel} to html.
     */
    public void updateUI(final BaseModel model) {
        Log.d("updateUI(BaseModel)", model.getModelName());

        modelName = model.getModelName();
        modelType = model.getModelType();
        transmitterType = model.getTransmitterType();

        // Generate HTML in background task
        new GenerateHtmlTask(this) {
            @Override
            protected void onPostExecute(final String result) {
                // update webwiew
                switch (getResultStatus()) {
                case ok:
                    updateUI();
                    break;

                case error:
                    if (getResultMessage() != null) {
                        showDialog(getResultMessage());
                    } else if (getResultException() != null) {
                        showDialog(getResultException().getLocalizedMessage());
                    }
                    break;

                default:
                    // do nothing
                }
            }

            @Override
            protected void onPreExecute() {
                // tell user to be patient
                setWait(R.string.msg_convert_to_html);
            }
        }.execute(model);
    }

    /**
     * Update WebView in a background thread without blocking the UI thread.
     * Read data from {@link Uri} and decode into a {@link BaseModel}.
     */
    public void updateUI(final Uri uri) {
        Log.d("updateUI(Uri)", uri.toString());

        new GetModelFromUriTask(this) {
            @Override
            protected void onPostExecute(final BaseModel result) {
                // convert to HTML
                updateUI(result);
            }

            @Override
            protected void onPreExecute() {
                // tell user to be patient
                setWait(R.string.msg_decode_data);
            }
        }.execute(uri);
    }
}