/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.ucclkp.syosetureader.recipientchip;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * RecipientChipSpan defines a ReplacementSpan that contains information relevant to a
 * particular recipient and renders a background asset to go with it.
 */
public class RecipientChipSpan extends ReplacementDrawableSpan
{
    public RecipientChipSpan(int bgColor)
    {
        super(bgColor, 0, false);
    }

    public RecipientChipSpan(int bgColor, int textColor)
    {
        super(bgColor, textColor, true);
    }


    @Override
    public Rect getBounds()
    {
        return super.getBounds();
    }

    @Override
    public String toString()
    {
        return "<RecipientChip>";
    }
}
