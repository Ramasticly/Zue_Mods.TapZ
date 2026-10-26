package com.zue.gd.util;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.namcobandaigames.dragonballtap.apk.Utility;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PacBuilderView {

    private static Dialog progressDialog;
    private static ProgressBar progressBar;
    private static TextView statusText;
    private static Button actionButton; 
    private static boolean isCancelled = false;
    private static final Handler uiHandler = new Handler(Looper.getMainLooper());

    public static void init() {
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                final File assetsDir = new File("/storage/emulated/0/Tap Battle/assets/");

                if (!assetsDir.exists() || !assetsDir.isDirectory()) {
                    Toast.makeText(Utility.activ, "Pasta assets não encontrada!", Toast.LENGTH_SHORT).show();
                    return;
                }

                File[] subFiles = assetsDir.listFiles();
                List<String> sortedFolders = new ArrayList<String>();
                if (subFiles != null) {
                    for (File f : subFiles) {
                        if (f.isDirectory()) sortedFolders.add(f.getName());
                    }
                }

                Collections.sort(sortedFolders);
                final List<String> folderNames = new ArrayList<String>();
                folderNames.add("Gerar todas");
                folderNames.addAll(sortedFolders);

                AlertDialog.Builder builder = new AlertDialog.Builder(Utility.activ);
                builder.setTitle("Escolha uma pasta para gerar PAC");
                final String[] options = folderNames.toArray(new String[0]);
                
                builder.setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        List<String> selection = new ArrayList<String>();
                        if (which == 0) {
                            for (int i = 1; i < options.length; i++) selection.add(options[i]);
                        } else {
                            selection.add(options[which]);
                        }
                        isCancelled = false; 
                        startProcess(selection);
                    }
                });
                builder.show();
            }
        });
    }

    private static void startProcess(final List<String> folders) {
        uiHandler.post(new Runnable() {
            @Override public void run() { showProgressDialog(); }
        });
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                File outRoot = new File("/storage/emulated/0/Tap Battle/PACS/");
                for (int i = 0; i < folders.size(); i++) {
                    if (isCancelled) break; 
                    final String fName = folders.get(i);
                    final int prog = (i * 100) / folders.size();
                    
                    RealPacBuilder.buildSingle(new File("/storage/emulated/0/Tap Battle/assets/", fName), outRoot, new RealPacBuilder.ProgressCallback() {
                        @Override public void onBuildStart(String p, int idx, int t) { updateUI("Iniciando: " + p, prog); }
                        @Override public void onSegmentPacked(String p, int idx, String f, int s) { updateUI("Processando: " + f, -1); }
                        @Override public void onBuildDone(String p) { updateUI("Concluído: " + p, prog + (100/Math.max(1, folders.size()))); }
                        @Override public void onError(final String e) {
                            uiHandler.post(new Runnable() {
                                @Override public void run() { Toast.makeText(Utility.activ, "Erro: " + e, Toast.LENGTH_LONG).show(); }
                            });
                        }
                    });
                }
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (progressDialog != null) progressDialog.dismiss();
                        Toast.makeText(Utility.activ, isCancelled ? "Cancelado" : "Concluído!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    private static void showProgressDialog() {
        progressDialog = new Dialog(Utility.activ);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);

        LinearLayout layout = new LinearLayout(Utility.activ);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 60, 60, 60);
        layout.setGravity(Gravity.CENTER);
        layout.setBackgroundColor(Color.WHITE);

        statusText = new TextView(Utility.activ);
        statusText.setTextColor(Color.BLACK);
        statusText.setPadding(0, 0, 0, 40);
        layout.addView(statusText);

        progressBar = new ProgressBar(Utility.activ, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        progressBar.setMax(100);
        layout.addView(progressBar);

        actionButton = new Button(Utility.activ);
        actionButton.setText("CANCELAR");
        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { isCancelled = true; actionButton.setEnabled(false); }
        });
        layout.addView(actionButton);

        progressDialog.setContentView(layout);
        progressDialog.show();
    }

    private static void updateUI(final String m, final int p) {
        uiHandler.post(new Runnable() {
            @Override public void run() {
                if (statusText != null) statusText.setText(m);
                if (progressBar != null && p != -1) progressBar.setProgress(p);
            }
        });
    }
}
