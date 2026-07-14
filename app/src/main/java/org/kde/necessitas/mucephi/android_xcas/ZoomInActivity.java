/*
 *  Copyright (C) 2011 Leonel Hernández Sandoval.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.kde.necessitas.mucephi.android_xcas;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.snackbar.Snackbar;

import org.giac.xcaspad.Calculator;

public class ZoomInActivity extends AppCompatActivity {

    private ZoomableImageView imageView;
    private String currentExpression;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zoom_in);

        Toolbar toolbar = findViewById(R.id.zoom_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.zoom_title);
        }

        imageView = findViewById(R.id.zoom_image);

        currentExpression = getIntent().getStringExtra("operation");
        if (currentExpression != null) {
            Bitmap bitmap = Calculator.getImageBytes(currentExpression, 0.169, 0.282, 0.498);
            imageView.setImageBitmap(bitmap);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_zoom, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.zoom_reset) {
            if (imageView != null) imageView.resetZoom();
            return true;
        } else if (id == R.id.zoom_copy) {
            copyText();
            return true;
        } else if (id == R.id.zoom_share) {
            shareExpression();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void copyText() {
        if (currentExpression == null || currentExpression.isEmpty()) {
            return;
        }
        ClipboardManager clipboard =
                (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("xcas pad", currentExpression);
        clipboard.setPrimaryClip(clip);
        Snackbar.make(imageView, R.string.zoom_copied, Snackbar.LENGTH_SHORT).show();
    }

    private void shareExpression() {
        if (currentExpression == null || currentExpression.isEmpty()) {
            return;
        }
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, currentExpression);
        startActivity(Intent.createChooser(send, getString(R.string.zoom_share)));
    }
}
