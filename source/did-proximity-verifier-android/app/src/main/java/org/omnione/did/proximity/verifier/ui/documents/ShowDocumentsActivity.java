/*
 * Copyright 2026 OmniOne.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.omnione.did.proximity.verifier.ui.documents;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.lifecycle.ViewModelProvider;

import org.omnione.did.proximity.sdk.reader.nfc.NfcTagDispatchGuard;
import org.omnione.did.proximity.verifier.R;
import org.omnione.did.proximity.verifier.data.model.Claim;
import org.omnione.did.proximity.verifier.data.model.ReturnedDocument;
import org.omnione.did.proximity.verifier.data.model.VerificationItem;
import org.omnione.did.proximity.verifier.databinding.ActivityShowDocumentsBinding;
import org.omnione.did.proximity.verifier.databinding.ItemKvGroupRowBinding;
import org.omnione.did.proximity.verifier.databinding.ItemKvImageRowBinding;
import org.omnione.did.proximity.verifier.databinding.ItemKvRowBinding;
import org.omnione.did.proximity.verifier.databinding.ViewDocumentSectionBinding;
import org.omnione.did.proximity.verifier.ui.common.BaseActivity;
import org.omnione.did.proximity.verifier.util.IntentRouter;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ShowDocumentsActivity extends BaseActivity {

    public static final String EXTRA_SOURCE = "extra_source";
    public static final String SOURCE_NFC = "nfc";

    private ActivityShowDocumentsBinding binding;
    private ShowDocumentsViewModel viewModel;
    private NfcTagDispatchGuard tagDispatchGuard;
    private boolean fromNfc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityShowDocumentsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        fromNfc = SOURCE_NFC.equals(getIntent().getStringExtra(EXTRA_SOURCE));
        tagDispatchGuard = NfcTagDispatchGuard.create(this);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        applySystemBarInsets(binding.getRoot(), bars -> {
            applyTopInsetPadding(binding.appBar.getRoot(), bars.top);
            applyBottomInsetPadding(binding.bottomBar, dp(16), bars.bottom);
        });

        binding.appBar.appBarTitle.setText(R.string.documents_title);
        binding.appBar.appBarLeft.setImageResource(R.drawable.ic_back);
        binding.appBar.appBarLeft.setOnClickListener(v -> leave());
        binding.okButton.setOnClickListener(v -> leave());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                leave();
            }
        });

        viewModel = new ViewModelProvider(this).get(ShowDocumentsViewModel.class);
        viewModel.getDocuments().observe(this, this::render);
        viewModel.init();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (fromNfc) {
            tagDispatchGuard.engage();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        tagDispatchGuard.release();
    }

    private void leave() {
        viewModel.release();
        IntentRouter.startHome(this);
        finish();
    }

    private void render(List<ReturnedDocument> documents) {
        binding.resultCount.setText(
                getString(R.string.documents_returned_count, documents.size()));

        binding.documentContainer.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        for (ReturnedDocument document : documents) {
            ViewDocumentSectionBinding section =
                    ViewDocumentSectionBinding.inflate(inflater, binding.documentContainer, false);
            ((ViewGroup.MarginLayoutParams) section.getRoot().getLayoutParams()).topMargin = dp(20);

            section.doctypeTitle.setTitle(document.doctype);
            section.verificationHead.groupHeadLabel.setText(R.string.group_head_verification);
            section.attributesHead.groupHeadLabel.setText(R.string.group_head_attributes);

            bindVerification(inflater, section.verificationContainer, document.verification);
            bindClaims(inflater, section.attributesContainer, document.claims);

            binding.documentContainer.addView(section.getRoot());
        }
    }

    private void bindVerification(LayoutInflater inflater, ViewGroup container,
                                  List<VerificationItem> items) {
        for (int i = 0; i < items.size(); i++) {
            VerificationItem item = items.get(i);
            ItemKvRowBinding row = ItemKvRowBinding.inflate(inflater, container, false);
            row.kvLabel.setAllCaps(true);
            row.kvLabel.setText(item.label);
            row.kvValue.setText(item.value);
            if (item.note != null) {
                row.kvNote.setText(item.note);
                row.kvNote.setVisibility(View.VISIBLE);
            }
            if (item.danger) {
                row.kvValue.setTextColor(ContextCompat.getColor(this, R.color.brand_error));
                row.kvValue.setTypeface(row.kvValue.getTypeface(), Typeface.BOLD);
            }
            container.addView(row.getRoot());
            addDividerIfNeeded(inflater, container, i, items.size());
        }
    }

    private void bindClaims(LayoutInflater inflater, ViewGroup container, List<Claim> claims) {
        for (int i = 0; i < claims.size(); i++) {
            Claim claim = claims.get(i);
            if (claim.isGroup()) {
                bindClaimGroup(inflater, container, claim, dp(64));
            } else if (claim.isImage()) {
                bindClaimImage(inflater, container, claim, dp(64));
            } else {
                bindClaimValue(inflater, container, claim.label, claim.value);
            }
            addDividerIfNeeded(inflater, container, i, claims.size());
        }
    }

    private void bindClaimGroup(LayoutInflater inflater, ViewGroup container,
                                Claim claim, int inset) {
        ItemKvGroupRowBinding group = ItemKvGroupRowBinding.inflate(inflater, container, false);
        group.kvGroupLabel.setText(claim.label);

        List<Claim> children = claim.children;
        for (int i = 0; i < children.size(); i++) {
            Claim child = children.get(i);
            int childInset = inset + dp(26);
            if (child.isGroup()) {
                bindClaimGroup(inflater, group.kvGroupChildren, child, childInset);
            } else if (child.isImage()) {
                bindClaimImage(inflater, group.kvGroupChildren, child, childInset);
            } else {
                bindClaimValue(inflater, group.kvGroupChildren, child.label, child.value);
            }
            addDividerIfNeeded(inflater, group.kvGroupChildren, i, children.size());
        }

        group.kvGroupHeader.setOnClickListener(v -> {
            boolean open = group.kvGroupChildren.getVisibility() == View.VISIBLE;
            group.kvGroupChildren.setVisibility(open ? View.GONE : View.VISIBLE);
            group.kvGroupChevron.setImageResource(
                    open ? R.drawable.ic_chevron_down : R.drawable.ic_chevron_up);
        });

        container.addView(group.getRoot());
    }

    private void bindClaimValue(LayoutInflater inflater, ViewGroup container,
                                String label, String value) {
        ItemKvRowBinding row = ItemKvRowBinding.inflate(inflater, container, false);
        row.kvLabel.setText(label);
        row.kvValue.setText(value);
        container.addView(row.getRoot());
    }

    private void bindClaimImage(LayoutInflater inflater, ViewGroup container,
                                Claim claim, int inset) {
        byte[] image = claim.image;
        Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);
        if (bitmap == null) {
            bindClaimValue(inflater, container, claim.label,
                    claim.value != null ? claim.value : "(" + image.length + " bytes)");
            return;
        }

        ItemKvImageRowBinding row = ItemKvImageRowBinding.inflate(inflater, container, false);
        row.kvImageLabel.setText(claim.label);
        row.kvImage.setImageBitmap(bitmap);
        row.kvImage.setMaxWidth(getResources().getDisplayMetrics().widthPixels - inset);
        container.addView(row.getRoot());
    }

    private void addDividerIfNeeded(LayoutInflater inflater, ViewGroup container,
                                    int index, int size) {
        if (index >= size - 1) return;
        View divider = inflater.inflate(R.layout.view_divider, container, false);
        container.addView(divider);
    }
}
