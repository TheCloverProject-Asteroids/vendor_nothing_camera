/*
 * SPDX-FileCopyrightText: Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package com.nothing.camera.review;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class ReviewTrampolineActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent != null) {
            Intent viewIntent = new Intent(Intent.ACTION_VIEW);
            Uri data = intent.getData();
            String type = intent.getType();

            if (data != null && type != null) {
                viewIntent.setDataAndType(data, type);
            } else if (data != null) {
                viewIntent.setData(data);
            } else if (type != null) {
                viewIntent.setType(type);
            }

            viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (intent.getExtras() != null) {
                viewIntent.putExtras(intent.getExtras());
            }

            try {
                viewIntent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
                startActivity(viewIntent);
            } catch (Exception e) {
                viewIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                try {
                    startActivity(viewIntent);
                } catch (ActivityNotFoundException ignored) {
                }
            }
        }

        finish();
    }
}
