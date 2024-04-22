package com.arkbuilders.arkdrop.presentation.feature.editprofile

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arkbuilders.arkdrop.R
import com.arkbuilders.arkdrop.ui.theme.BlueDark600

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(modifier: Modifier = Modifier) {
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                ),
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBackIosNew,
                            contentDescription = null
                        )
                        Spacer(modifier = modifier.width(ButtonDefaults.IconSpacing))
                    }
                })
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = modifier.height(24.dp))

            Image(
                painter = painterResource(id = R.drawable.avatar_mock),
                contentDescription = null,
                modifier = modifier
                    .size(128.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = modifier.height(12.dp))
            TextButton(
                onClick = {
                    // Open gallery picker
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.Black
                )
            ) {
                Text("Change Avatar")
                Spacer(modifier = modifier.width(ButtonDefaults.IconSpacing))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null
                )
            }
            Spacer(modifier = modifier.height(24.dp))
            OutlinedTextField(
                modifier = modifier
                    .fillMaxWidth(),
                value = "", onValueChange = {},
                trailingIcon = {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = null)
                },
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors()
            )
            Spacer(modifier = modifier.height(24.dp))
            Button(
                modifier = modifier
                    .fillMaxWidth(),
                onClick = {
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = BlueDark600,
                ),
            ) {
                Text("Save")
            }
        }
    }
}

@Preview
@Composable
fun PreviewEditProfileScreen() {
    EditProfileScreen()
}
