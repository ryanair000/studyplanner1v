package com.example.studentcopilot.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.studentcopilot.R

@Composable
fun PangiaLogoLockup(
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(id = R.drawable.pangia_logo_primary),
        contentDescription = stringResource(id = R.string.pangia_logo_content_description),
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}

@Composable
fun PangiaAppIcon(
    modifier: Modifier = Modifier,
    tintedPlate: Boolean = false,
) {
    val image = @Composable {
        Image(
            painter = painterResource(id = R.mipmap.ic_launcher),
            contentDescription = stringResource(id = R.string.pangia_icon_content_description),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
    }

    if (tintedPlate) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White.copy(alpha = 0.9f))
                .padding(4.dp),
        ) {
            image()
        }
    } else {
        Box(modifier = modifier) {
            image()
        }
    }
}

@Composable
fun PangiaCompactBrand(
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PangiaAppIcon(modifier = Modifier.size(34.dp))
        Column {
            Text(
                text = "Pangia",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
            )
            subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
