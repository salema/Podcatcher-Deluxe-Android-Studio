/**
 * Copyright 2012-2016 Kevin Hausmann
 *
 * This file is part of Podcatcher Deluxe.
 *
 * Podcatcher Deluxe is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * Podcatcher Deluxe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Podcatcher Deluxe. If not, see <http://www.gnu.org/licenses/>.
 */

package com.podcatcher.deluxe.view;

import com.podcatcher.deluxe.R;
import com.podcatcher.deluxe.listeners.OnChangePodcastListListener;
import com.podcatcher.deluxe.model.PodcastManager;
import com.podcatcher.deluxe.model.types.Genre;
import com.podcatcher.deluxe.model.types.Language;
import com.podcatcher.deluxe.model.types.MediaType;
import com.podcatcher.deluxe.model.types.Podcast;
import com.podcatcher.deluxe.model.types.Suggestion;
import com.podcatcher.deluxe.view.fragments.AddSuggestionFragment.AddSuggestionListener;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

/**
 * A card view to represent a podcast suggestion.
 */
public class SuggestionListItemViewHolder extends RecyclerView.ViewHolder
        implements Callback, Palette.PaletteAsyncListener, OnChangePodcastListListener {

    /**
     * Separator for meta data in the UI
     */
    private static final String METADATA_SEPARATOR = " • ";

    /**
     * The suggestioned podcast currently represented by this card
     */
    private Suggestion suggestion;
    /**
     * The view item root context
     */
    private Context context;
    /**
     * Our podcast manager handle
     */
    private final PodcastManager podcastManager = PodcastManager.getInstance();

    /**
     * The logo image view
     */
    private ImageView logoImageView;
    /**
     * The title bar wrapper view (holds alpha)
     */
    private View titleBar;
    /**
     * The feature icon view
     */
    private ImageView featuredIconView;
    /**
     * The explicit icon view
     */
    private ImageView explicitIconView;
    /**
     * The new icon view
     */
    private ImageView newIconView;
    /**
     * The title text view
     */
    private TextView titleTextView;
    /**
     * The meta information text view
     */
    private TextView metaTextView;
    /**
     * The add suggestion button
     */
    private ImageView addButton;
    /**
     * The description text view
     */
    private TextView descriptionTextView;

    /**
     * Expanded flag helper
     */
    private boolean expanded;

    /**
     * Create a new view holder.
     *
     * @param itemView The item's root view.
     */
    public SuggestionListItemViewHolder(View itemView) {
        super(itemView);

        context = itemView.getContext();
        logoImageView = (ImageView) itemView.findViewById(R.id.suggestion_logo);
        titleBar = itemView.findViewById(R.id.suggestion_title_bar);
        featuredIconView = (ImageView) itemView.findViewById(R.id.suggestion_featured);
        explicitIconView = (ImageView) itemView.findViewById(R.id.suggestion_explicit);
        newIconView = (ImageView) itemView.findViewById(R.id.suggestion_new);
        titleTextView = (TextView) itemView.findViewById(R.id.suggestion_name);
        metaTextView = (TextView) itemView.findViewById(R.id.suggestion_meta);
        addButton = (ImageView) itemView.findViewById(R.id.suggestion_add_button);
        descriptionTextView = (TextView) itemView.findViewById(R.id.suggestion_description);

        // Takes away alpha from the title bar as long as
        // the add button is pressed as a touch feedback
        addButton.setOnTouchListener(new View.OnTouchListener() {

            // Alpha value helper
            private float originalAlpha;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    originalAlpha = titleBar.getAlpha();
                    titleBar.setAlpha(1);
                } else if (event.getAction() == MotionEvent.ACTION_UP)
                    titleBar.setAlpha(originalAlpha);

                return false;
            }
        });

        descriptionTextView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (expanded) {
                    descriptionTextView.setMaxLines(
                            context.getResources().getInteger(R.integer.suggestion_description_lines));
                    logoImageView.setVisibility(View.VISIBLE);
                } else {
                    descriptionTextView.setMaxLines(Integer.MAX_VALUE);

                    final ViewMode mode = ViewMode.determineViewMode(context.getResources());
                    logoImageView.setVisibility(mode.isSmall() ? View.GONE : View.VISIBLE);
                }

                expanded = !expanded;
            }
        });
    }

    /**
     * @return Podcast suggestion currently represented by this view holder.
     */
    @Nullable
    public Suggestion getSuggestion() {
        return suggestion;
    }

    /**
     * Make the view update all its child to represent input given.
     *
     * @param suggestion       Podcast suggestion to represent.
     * @param listener         Call-back to alert when the button is pressed.
     * @param selectedLanguage The currently filtered for language (give <code>null</code> for none)
     * @param selectedGenre    The currently filtered for category (give <code>null</code> for none)
     * @param selectedType     The currently filtered for media type (give <code>null</code> for none)
     */
    public void show(Suggestion suggestion, final AddSuggestionListener listener,
                     Language selectedLanguage, Genre selectedGenre, MediaType selectedType) {
        // We need to keep this handle for the Palette action onSuccess()
        this.suggestion = suggestion;

        // 1. Start loading suggestion logo
        Picasso.with(context).cancelRequest(logoImageView);
        titleBar.setBackgroundColor(Color.WHITE);
        logoImageView.setVisibility(View.VISIBLE); // Might have been hidden by text expansion
        if (suggestion.getLogoUrl() != null && suggestion.getLogoUrl().startsWith("http"))
            Picasso.with(context)
                    .load(suggestion.getLogoUrl())
                    .fit().centerCrop() // Resize logo and crop to fit image view
                    .into(logoImageView, this);

        // 2. Create/Set the texts to display
        titleTextView.setText(suggestion.getName());
        metaTextView.setText(createClassificationLabel(selectedLanguage, selectedGenre, selectedType));
        expanded = false; // Reset description textview to the original expansion state
        descriptionTextView.setMaxLines(context.getResources().getInteger(R.integer.suggestion_description_lines));
        descriptionTextView.setText(suggestion.getDescription());

        // 3. Prepare/Update the add button
        if (podcastManager.containsAllOf(suggestion)) {
            addButton.setEnabled(false);
            addButton.setImageResource(R.drawable.ic_checkmark);
        } else {
            addButton.setEnabled(true);
            addButton.setImageResource(R.drawable.ic_media_add);
            addButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    listener.onAddSuggestion(SuggestionListItemViewHolder.this.suggestion);
                }
            });
        }

        // 4. Decorate featured, explicit, and/or new suggestions
        featuredIconView.setVisibility(suggestion.isFeatured() ? View.VISIBLE : View.GONE);
        explicitIconView.setVisibility(suggestion.isExplicit() ? View.VISIBLE : View.GONE);
        newIconView.setVisibility(suggestion.isNew() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onSuccess() {
        // Callback from the Picasso when podcast logo is loaded -> generate palette
        if (suggestion != null && suggestion.isFeatured()) {
            final Bitmap bitmap = ((BitmapDrawable) logoImageView.getDrawable()).getBitmap();
            Palette.from(bitmap).generate(this);
        }
    }

    @Override
    public void onError() {
        // pass
    }

    @Override
    public void onGenerated(Palette palette) {
        // Callback from Palette, color title bar if featured
        if (suggestion != null && suggestion.isFeatured() && palette != null) {
            final Palette.Swatch swatch = palette.getLightVibrantSwatch() == null ?
                    palette.getVibrantSwatch() == null ? palette.getLightMutedSwatch() :
                            palette.getVibrantSwatch() : palette.getLightVibrantSwatch();

            if (swatch != null)
                titleBar.setBackgroundColor(swatch.getRgb());
        }
    }

    @Override
    public void onPodcastAdded(Podcast podcast) {
        // Callback from the Podcast Manager when a suggestion is added
        if (podcast != null && suggestion != null &&
                suggestion.hasFeed(podcast.getUrl()) && podcastManager.containsAllOf(suggestion)) {
            addButton.setEnabled(false);
            addButton.setImageResource(R.drawable.ic_checkmark);
        }
    }

    @Override
    public void onPodcastRemoved(Podcast podcast) {
        // pass
    }

    private String createClassificationLabel(Language selectedLanguage, Genre selectedGenre, MediaType selectedType) {
        final Resources resources = context.getResources();
        String result = "";

        if (selectedLanguage == null)
            for (Language language : suggestion.getLanguages())
                result += resources.getStringArray(R.array.languages)[language.ordinal()]
                        + METADATA_SEPARATOR;

        if (selectedType == null)
            for (MediaType type : suggestion.getMediaTypes())
                result += resources.getStringArray(R.array.types)[type.ordinal()]
                        + METADATA_SEPARATOR;

        // The genres will always be shown, but for the selected one
        for (Genre genre : suggestion.getGenres())
            if (!genre.equals(selectedGenre))
                result += resources.getStringArray(R.array.genres)[genre.ordinal()]
                        + METADATA_SEPARATOR;

        return result.length() > METADATA_SEPARATOR.length() ?
                result.substring(0, result.length() - METADATA_SEPARATOR.length()) :
                resources.getStringArray(R.array.genres)[((Genre) suggestion.getGenres().toArray()[0]).ordinal()];
    }
}
