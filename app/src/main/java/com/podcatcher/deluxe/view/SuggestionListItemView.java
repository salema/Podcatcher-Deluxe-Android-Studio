/** Copyright 2012-2015 Kevin Hausmann
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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.podcatcher.deluxe.R;
import com.podcatcher.deluxe.model.types.MediaType;
import com.podcatcher.deluxe.model.types.Suggestion;
import com.podcatcher.deluxe.view.fragments.SuggestionFragment.AddSuggestionDialogListener;

/**
 * A list item view to represent a podcast suggestion.
 */
public class SuggestionListItemView extends RelativeLayout {

    /**
     * Separator for meta data in the UI
     */
    private static final String METADATA_SEPARATOR = " • ";

    /**
     * The feature icon view
     */
    private ImageView featuredIconView;
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
    private Button addButton;
    /**
     * The description text view
     */
    private TextView descriptionTextView;

    /**
     * Create a podcast suggestion item list view.
     *
     * @param context Context for the view to live in.
     * @param attrs   View attributes.
     */
    public SuggestionListItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        featuredIconView = (ImageView) findViewById(R.id.suggestion_featured);
        titleTextView = (TextView) findViewById(R.id.suggestion_name);
        metaTextView = (TextView) findViewById(R.id.suggestion_meta);
        addButton = (Button) findViewById(R.id.suggestion_add_button);
        descriptionTextView = (TextView) findViewById(R.id.suggestion_description);
    }

    /**
     * Make the view update all its child to represent input given.
     *
     * @param suggestion       Podcast suggestion to represent.
     * @param listener         Call-back to alert when the button is pressed.
     * @param alreadyAdded     Whether the suggestion is already added.
     * @param languageWildcard Whether the current filter language setting has a
     *                         wildcard.
     * @param genreWildcard    Whether the current filter setting has a genre
     *                         wildcard.
     * @param typeWildcard     Whether the current filter setting has a type
     *                         wildcard.
     */
    public void show(final Suggestion suggestion, final AddSuggestionDialogListener listener,
                     boolean alreadyAdded, boolean languageWildcard, boolean genreWildcard,
                     boolean typeWildcard) {
        // 1. Set the text to display for title
        titleTextView.setText(suggestion.getName());

        // 2. Set the text to display for classification
        metaTextView.setText(createClassificationLabel(suggestion,
                languageWildcard, genreWildcard, typeWildcard));

        // 3. Set the text to display for the description
        descriptionTextView.setText(suggestion.getDescription());

        // 4. Find and prepare the add button
        if (alreadyAdded) {
            addButton.setEnabled(false);
            addButton.setBackgroundResource(0);
            addButton.setText(null);
            addButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_checkmark, 0);
        } else {
            addButton.setEnabled(true);
            addButton.setBackgroundResource(R.drawable.button_green);
            addButton.setText(suggestion.getMediaType().equals(MediaType.AUDIO) ?
                    R.string.suggestion_listen : R.string.suggestion_watch);
            addButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            addButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    listener.onAddSuggestion(suggestion);
                }
            });
        }

        // 5. Decorate featured and explicit suggestions
        featuredIconView.setVisibility(suggestion.isFeatured() || suggestion.isExplicit() ?
                VISIBLE : GONE);
        featuredIconView.setImageResource(suggestion.isFeatured() ?
                R.drawable.ic_suggestion_featured : R.drawable.ic_suggestion_explicit);
        setBackgroundColor(suggestion.isFeatured() ?
                getResources().getColor(R.color.featured_suggestion) : Color.TRANSPARENT);
    }

    private String createClassificationLabel(Suggestion suggestion,
                                             boolean languageWildcard, boolean genreWildcard, boolean typeWildcard) {
        final Resources res = getResources();
        String result = "";

        if (languageWildcard)
            result += res.getStringArray(R.array.languages)[suggestion.getLanguage().ordinal()]
                    + METADATA_SEPARATOR;

        // The genre will always be shown
        result += res.getStringArray(R.array.genres)[suggestion.getGenre().ordinal()];

        if (typeWildcard)
            result += METADATA_SEPARATOR
                    + res.getStringArray(R.array.types)[suggestion.getMediaType().ordinal()];

        return result;
    }
}
