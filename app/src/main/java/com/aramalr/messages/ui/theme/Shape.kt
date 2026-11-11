package com.aramalr.messages.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** From MyOS `mfvc_radius` / `mfvc_card_bg_radius` = 16dp */
val MyOsCorner = 16.dp

/** From MyOS `mfvc_card_item_radius` = 12dp */
val MyOsCornerSmall = 12.dp

val AramalrShapes =
    Shapes(
        extraSmall = RoundedCornerShape(MyOsCornerSmall),
        small = RoundedCornerShape(MyOsCornerSmall),
        medium = RoundedCornerShape(MyOsCorner),
        large = RoundedCornerShape(MyOsCorner),
        extraLarge = RoundedCornerShape(MyOsCorner),
    )

val MyOsCardShape = RoundedCornerShape(MyOsCorner)
val MyOsSearchShape = RoundedCornerShape(MyOsCorner)
