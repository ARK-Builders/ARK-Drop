@file:OptIn(ExperimentalMaterial3Api::class)

package dev.arkbuilders.drop.app.presentation.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import dev.arkbuilders.drop.app.R

@Composable
fun DropTopBar(
    title: String,
    leadingIcon: Painter? = null,
    onLeadingIconClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = MaterialTheme.colorScheme.onBackground,
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
        },
        navigationIcon = {
            leadingIcon?.let {
                IconButton(
                    onClick = {
                        onLeadingIconClick?.invoke()
                    },
                ) {
                    Icon(
                        painter = leadingIcon,
                        contentDescription = null,
                    )
                }
            }
        },
        actions = {
            trailingContent?.let {
                it()
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = containerColor,
                titleContentColor = contentColor,
                actionIconContentColor = contentColor,
            ),
    )
}

@Composable
fun DropTopBarBack(
    title: String,
    onBackClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = MaterialTheme.colorScheme.onBackground,
) {
    DropTopBar(
        title = title,
        leadingIcon = painterResource(R.drawable.ic_back),
        onLeadingIconClick = onBackClick,
        trailingContent = trailingContent,
        containerColor = containerColor,
        contentColor = contentColor,
    )
}
