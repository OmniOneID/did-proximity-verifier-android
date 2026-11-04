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

package org.omnione.did.proximity.verifier.ui.issuers;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.WindowCompat;
import androidx.lifecycle.ViewModelProvider;

import org.omnione.did.proximity.verifier.R;
import org.omnione.did.proximity.verifier.data.model.TrustedIssuer;
import org.omnione.did.proximity.verifier.databinding.ActivityTrustedIssuersBinding;
import org.omnione.did.proximity.verifier.databinding.ItemIssuerDidBinding;
import org.omnione.did.proximity.verifier.ui.common.BaseActivity;
import org.omnione.did.proximity.verifier.ui.common.FontSpan;
import org.omnione.did.proximity.verifier.util.IntentRouter;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class TrustedIssuersActivity extends BaseActivity {

    private ActivityTrustedIssuersBinding binding;
    private TrustedIssuersViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTrustedIssuersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        applySystemBarInsets(binding.getRoot(), bars -> {
            applyTopInsetPadding(binding.appBar.getRoot(), bars.top);
            applyBottomInsetPadding(binding.bottomBar, dp(16), bars.bottom);
        });

        binding.appBar.appBarTitle.setText(R.string.trusted_issuers_title);
        binding.appBar.appBarLeft.setImageResource(R.drawable.ic_back);
        binding.appBar.appBarLeft.setOnClickListener(v -> finish());
        binding.appBar.appBarRight.setImageResource(R.drawable.ic_gear);
        binding.appBar.appBarRight.setContentDescription(getText(R.string.server_url_title));
        binding.appBar.appBarRight.setVisibility(View.VISIBLE);
        binding.appBar.appBarRight.setOnClickListener(v -> IntentRouter.startServerUrl(this));

        binding.emptyState.emptyHint.setText(buildEmptyHint());
        binding.refreshButton.setOnClickListener(v -> viewModel.refresh());

        viewModel = new ViewModelProvider(this).get(TrustedIssuersViewModel.class);
        viewModel.getState().observe(this, this::render);
        viewModel.getRefreshFailed().observe(this, event -> {
            if (event.getIfNotHandled() == null) return;
            showNotice(R.string.common_error, R.string.transfer_error_unexpected, null);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.init();
    }

    private void render(TrustedIssuersUiState state) {
        boolean has = state.hasIssuers();
        binding.cachedContainer.setVisibility(has ? View.VISIBLE : View.GONE);
        binding.emptyState.getRoot().setVisibility(has ? View.GONE : View.VISIBLE);

        if (has) {
            binding.sectionTitle.setBadgeCount(state.issuers.size());
            if (state.updatedAt != null) {
                binding.lastUpdated.setText(
                        getString(R.string.trusted_issuers_last_updated, state.updatedAt));
                binding.lastUpdated.setVisibility(View.VISIBLE);
            } else {
                binding.lastUpdated.setVisibility(View.GONE);
            }
            bindIssuers(state.issuers);
        }

        binding.refreshButton.setEnabled(!state.refreshing);
        if (state.refreshing) {
            showProgress();
        } else {
            dismissProgress();
        }
    }

    private void bindIssuers(List<TrustedIssuer> issuers) {
        binding.issuerContainer.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        for (int i = 0; i < issuers.size(); i++) {
            TrustedIssuer issuer = issuers.get(i);
            ItemIssuerDidBinding row =
                    ItemIssuerDidBinding.inflate(inflater, binding.issuerContainer, false);

            row.issuerName.setText(issuer.name);
            row.issuerName.setVisibility(issuer.name.isEmpty() ? View.GONE : View.VISIBLE);
            row.issuerDid.setTypeface(Typeface.MONOSPACE);
            row.issuerDid.setText(issuer.did);
            binding.issuerContainer.addView(row.getRoot());
            if (i < issuers.size() - 1) {
                View divider = inflater.inflate(
                        R.layout.view_divider, binding.issuerContainer, false);
                binding.issuerContainer.addView(divider);
            }
        }
    }

    private CharSequence buildEmptyHint() {
        String action = getString(R.string.trusted_issuers_refresh);
        String full = getString(R.string.trusted_issuers_empty_hint, action);
        SpannableStringBuilder builder = new SpannableStringBuilder(full);
        int start = full.indexOf(action);
        if (start >= 0) {
            int end = start + action.length();
            builder.setSpan(
                    new ForegroundColorSpan(ContextCompat.getColor(this, R.color.brand_primary)),
                    start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.setSpan(
                    new FontSpan(ResourcesCompat.getFont(this, R.font.pretendard_semibold)),
                    start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return builder;
    }
}
